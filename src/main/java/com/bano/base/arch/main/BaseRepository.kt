package com.bano.base.arch.main

import android.os.Handler
import android.os.HandlerThread
import android.support.annotation.WorkerThread
import android.util.Log
import com.bano.base.annotation.IdParent
import com.bano.base.arch.Repository
import com.bano.base.contract.BaseContract
import com.bano.base.contract.MapperContract
import io.realm.Realm
import io.realm.RealmModel
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.annotations.PrimaryKey
import java.lang.reflect.Modifier
import java.util.*


/**
 * Created by bk_alexandre.pereira on 25/07/2017.
 *
 */
abstract class BaseRepository<E, T, X : Any> : Repository, MapperContract<E, T, X> where T : RealmModel  {
    private val tag = "BaseRepository"
    val idParent: Long?
    /**
     * set this flag when is necessary to specify different scenarios in setFieldFromApi method
     */
    var resumeMode: Boolean = false
    val limit: Int
    var offset: Int = 0
        private set
    var total: Int? = null
    private val mExcludeFieldName: String?
    private val mOrderFieldName: String?
    private val mPrimaryKeyFieldName: String
    private val mRealmClass: Class<T>

    constructor(clazz: Class<T>): super() {
        idParent = null
        limit = 0
        mExcludeFieldName = null
        mOrderFieldName = null
        mRealmClass = clazz
        mPrimaryKeyFieldName = getPrimaryKeyFieldName(clazz)
    }

    constructor(builder: Builder<T>): super() {
        idParent = builder.idParent
        limit = builder.limit
        resumeMode = builder.resumeMode
        mExcludeFieldName = builder.excludeFieldName
        mOrderFieldName = builder.orderFieldName
        offset = 0
        mRealmClass = builder.realmClass
        mPrimaryKeyFieldName = getPrimaryKeyFieldName(mRealmClass)
    }

    constructor(realm: Realm, builder: Builder<T>): super(realm) {
        idParent = builder.idParent
        limit = builder.limit
        resumeMode = builder.resumeMode
        mExcludeFieldName = builder.excludeFieldName
        mOrderFieldName = builder.orderFieldName
        offset = 0
        mRealmClass = builder.realmClass
        mPrimaryKeyFieldName = getPrimaryKeyFieldName(mRealmClass)
    }

    constructor(realm: Realm, clazz: Class<T>) : super(realm) {
        idParent = null
        limit = 0
        offset = 0
        mExcludeFieldName = null
        mOrderFieldName = null
        mRealmClass = clazz
        mPrimaryKeyFieldName = getPrimaryKeyFieldName(mRealmClass)
    }

    private fun getPrimaryKeyFieldName(clazz: Class<T>): String = clazz.declaredFields.find {
        it.annotations.any { it is PrimaryKey }
    }?.name ?: throw IllegalArgumentException("$clazz must have primary key")

    protected open fun getTagLog(): String = "BaseRepository"

    protected open fun getIdParentFieldName(): String? = mRealmClass.declaredFields.find {
        it.annotations.any { it is IdParent }
    }?.name

    protected open fun isSameObj(obj: E, apiObj: X): Boolean = obj == apiObj

    open fun getRealmQueryTable(realm: Realm): RealmQuery<T> = realm.where(mRealmClass)

    open fun getLocalObj(id: Long): E? =
        getLocalObj(getRealmQueryTable(getRealm()).equalTo(mPrimaryKeyFieldName, id))

    fun getLocalObj(realmQuery: RealmQuery<T>): E? {
        val realmModel = realmQuery.findFirst() ?: return null
        return createObj(realmModel)
    }

    fun getRealmQueryTable(): RealmQuery<T> {
        val idParentFieldName = getIdParentFieldName()
        return if (idParent == null || idParentFieldName == null) getRealmQueryTable(getRealm())
        else getRealmQueryTable(getRealm()).equalTo(idParentFieldName, idParent)
    }

    fun getLocalList(): List<E> = map(getDatabaseList())
    fun getLocalList(realmQuery: RealmQuery<T>): List<E> = map(getDatabaseList(realmQuery))
    private fun getDatabaseList(): RealmResults<T> = sortQuery(getRealmQueryTable())
    private fun getDatabaseList(realmQuery: RealmQuery<T>): RealmResults<T> = sortQuery(realmQuery)

    protected open fun sortQuery(realmQuery: RealmQuery<T>): RealmResults<T> =
            getQueryByOrder(offset, realmQuery)

    private fun getQueryByOrder(offset: Int, realmQuery: RealmQuery<T>): RealmResults<T> {
        val idParentFieldName = getIdParentFieldName()
        if (idParent != null && idParentFieldName != null) realmQuery.equalTo(idParentFieldName, idParent)
        return if(limit > 0 && mOrderFieldName != null && mExcludeFieldName != null) realmQuery
                .between(mOrderFieldName, offset, (offset + limit) -1)
                .isNull(mExcludeFieldName).sort(mOrderFieldName).findAll()
        else if(mOrderFieldName != null && mExcludeFieldName != null) realmQuery
                .isNull(mExcludeFieldName).sort(mOrderFieldName).findAll()
        else realmQuery.findAll()
    }

    /**
     * Call this function to set the next page only. Do the query after this method
     */
    open fun nextPage() {
        val total = total
        if(total != null && offset + limit > total) {
            return
        }
        offset += limit
    }

    fun resetPage() {
        resetRealm()
        offset = 0
    }

    protected open fun onBeforeInsertData(realm: Realm, apiObj: X) = Unit
    protected open fun onBeforeInsertData(realm: Realm, id: Long, apiObj: X) = Unit

    /**
     * This method is accessed when exists an obj local.
     * Must be implemented when the api obj fields is different from obj local
     *
     * @param oldObjLocal The obj local before the database update
     * @param objUpdated The obj updated with the apiObj fields
     */
    protected open fun setFieldsFromApi(oldObjLocal: E, objUpdated: E) = Unit

    /**
     * This method is accessed when exists an obj local and resumeMode is enabled.
     * Must be implemented when the api obj fields comes with less fields
     *
     * @param objLocal The obj local that must be updated
     * @param apiObj The obj that comes from API with less fields
     */
    protected open fun setFieldsFromApiInResumeMode(objLocal: E, apiObj: X) = Unit

    /**
     * Check if some data was deleted comparing the localList with apiList.
     * If in the apiList does not contains some data of localList, the local obj field excludeDate is updated
     * @param apiList The list that comes from the API
     */
    protected open fun handleDeletedDataFromApi(realm: Realm, localList: List<E>, apiList: List<X>) {
        localList.forEach { localObj ->
            if(localObj !is BaseContract) return@forEach
            val apiObj = apiList.find { isSameObj(localObj, it) }
            if (apiObj == null) {
                Log.d(tag, "$localObj excludeDate updated")
                localObj.excludeDate = Date().time
                realm.insertOrUpdate(createRealmObj(localObj))
            }
        }
    }

    fun insertOrUpdateList(offset: Int, apiList: List<X>, callback: (List<E>) -> Unit) {
        getRealm().executeTransactionAsync(Realm.Transaction { realm ->
            insertOrUpdateList(offset, realm, apiList)
        }, Realm.Transaction.OnSuccess {
            callback(getLocalList())
        })
    }

    fun insertOrUpdateList(offset: Int, realm: Realm, apiList: List<X>) {
        val localList = map(getQueryByOrder(offset, getRealmQueryTable(realm)))
        val apiListFiltered = apiList.filter { it != null }
        handleDeletedDataFromApi(realm, localList, apiListFiltered)
        Log.d(tag, getTagLog() + ": insertOrUpdateList()")
        var order = offset
        apiListFiltered.forEach { apiObj ->
            onBeforeInsertData(realm, apiObj)
            val objLocal = localList.find { isSameObj(it, apiObj) }
            if (objLocal != null) {
                val objToUpdate: E = if (resumeMode) {
                    setFieldsFromApiInResumeMode(objLocal, apiObj)
                    objLocal
                } else {
                    val objLocalUpdated = createObjFromObjApi(apiObj)
                    setFieldsFromApi(objLocal, objLocalUpdated)
                    objLocalUpdated
                }
                if(objToUpdate is BaseContract) objToUpdate.order = order++
                realm.insertOrUpdate(createRealmObj(objToUpdate))
            } else {
                val objToInsert = createObjFromObjApi(apiObj)
                if(objToInsert is BaseContract) objToInsert.order = order++
                realm.insertOrUpdate(createRealmObj(objToInsert))
            }
        }
    }

    /**
     * Call this method inside a transaction.
     * Insert or update list in same thread that this method was call.
     * Don't call this method in the main thread
     */
    @WorkerThread
    fun insertOrUpdateHashList(realm: Realm, apiList: HashSet<X>) {
        apiList.forEach {
            realm.insertOrUpdate(createRealmObj(createObjFromObjApi(it)))
        }
    }

    @WorkerThread
    fun insertOrUpdateHashList(realm: Realm, apiList: HashSet<X>, afterInsert: (realm: Realm, apiObj: X) -> Unit) {
        apiList.forEach {
            realm.insertOrUpdate(createRealmObj(createObjFromObjApi(it)))
            afterInsert(realm, it)
        }
    }

    open fun update(e: E, callback: () -> Unit) {
        getRealm().executeTransactionAsync(Realm.Transaction { realm ->
            realm.copyToRealmOrUpdate(createRealmObj(e))
            Log.d(tag, "$e updated")
        }, Realm.Transaction.OnSuccess {
            callback()
        })
    }

    fun update(eList: List<E>, callback: () -> Unit) {
        getRealm().executeTransactionAsync(Realm.Transaction { realm ->
            eList.forEach {
                realm.insertOrUpdate(createRealmObj(it))
                Log.d(tag, "$it updated")
            }
        }, Realm.Transaction.OnSuccess {
            callback()
        })
    }

    open fun insertOrUpdate(e: E, callback: (e: E) -> Unit) {
        getRealm().executeTransactionAsync(Realm.Transaction { realm ->
            realm.insertOrUpdate(createRealmObj(e))
        }, Realm.Transaction.OnSuccess {
            callback(e)
        })
    }

    fun insertOrUpdateNoCallback(t: T, callback: (e: E) -> Unit) {

        val handlerThread = HandlerThread("MyHandlerThread")
        handlerThread.start()
        val looper = handlerThread.looper
        val handler = Handler(looper)
        handler.post({

            getRealm().executeTransactionAsync(Realm.Transaction { realm ->
                realm.insertOrUpdate(t)
            }, Realm.Transaction.OnSuccess {
                callback(createObj(t))
            })

        })
    }

    protected fun insertOrUpdateFromObjApi(id: Long, objApi: X, callback: (E?) -> Unit) {
        getRealm().executeTransactionAsync(Realm.Transaction { realm ->
            onBeforeInsertData(realm, id, objApi)
            val idField = objApi::javaClass.get().declaredFields.find { field ->
                field.annotations.any { it is PrimaryKey }
            }

            val objLocal = if(idField != null) {
                val idFieldValue = if(Modifier.isPublic(idField.modifiers)) {
                    idField.get(objApi)
                }
                else {
                    val methods = objApi::javaClass.get().declaredMethods
                    methods.find { it.name?.toLowerCase()?.contains("get${idField.name.toLowerCase()}") == true }?.invoke(objApi)
                }

                when (idFieldValue) {
                    is Long -> getRealmQueryTable(realm).equalTo(mPrimaryKeyFieldName, idFieldValue).findFirst()
                    is String -> getRealmQueryTable(realm).equalTo(mPrimaryKeyFieldName, idFieldValue).findFirst()
                    is Int -> getRealmQueryTable(realm).equalTo(mPrimaryKeyFieldName, idFieldValue).findFirst()
                    else -> null
                }
            } else {
                Log.e(getTagLog(), "$objApi don't have PrimaryKey, the order will be missed")
                null
            }

            val objToUpdate = createObjFromObjApi(objApi)
            if(objLocal != null && objToUpdate is BaseContract && objLocal is BaseContract) {
                objToUpdate.order = objLocal.order
            }
            realm.insertOrUpdate(createRealmObj(objToUpdate))
        }, Realm.Transaction.OnSuccess {
            callback(getInsertedObj(id, objApi))
        })
    }

    protected open fun getInsertedObj(id: Long, objApi: X): E? = getLocalObj(id)

    class Builder<T : RealmModel>(internal val realmClass: Class<T>) {
        internal var excludeFieldName: String? = null
        internal var orderFieldName: String? = null
        internal var resumeMode: Boolean = false
        internal var limit: Int = 0
        internal var total: Int? = null
        internal var idParent: Long? = null

        fun setExcludeDateFieldName(fieldName: String): Builder<T> {
            excludeFieldName = fieldName
            return this
        }

        fun setOrderFieldName(fieldName: String): Builder<T> {
            orderFieldName = fieldName
            return this
        }

        fun resumeMode(): Builder<T> {
            resumeMode = true
            return this
        }

        fun setLimitQuery(limit: Int): Builder<T> {
            this.limit = limit
            return this
        }

        fun setTotalPagination(total: Int): Builder<T> {
            this.total = total
            return this
        }

        fun setIdParent(idParent: Long?): Builder<T> {
            this.idParent = idParent
            return this
        }
    }
}