package com.bano.base.arch.main.remote

import android.support.annotation.WorkerThread
import android.util.Log
import com.bano.base.arch.main.BaseRepository
import com.bano.base.contract.*
import io.realm.Realm
import io.realm.RealmModel
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.annotations.PrimaryKey
import io.realm.kotlin.deleteFromRealm
import java.lang.reflect.Modifier


/**
 * Created by bk_alexandre.pereira on 25/07/2017.
 *
 */
abstract class BaseRemoteRepository<E : Any, T, X : Any> : BaseRepository<E, T>, MapperContract<E, T, X> where T : RealmModel  {
    private val tag = "BaseRemoteRepository"
    /**
     * set this flag when is necessary to specify different scenarios in setFieldFromApi method
     */
    var resumeMode: Boolean = false

    constructor(builder: Builder<T>): super(builder) {
        resumeMode = builder.resumeMode
    }

    constructor(realm: Realm, builder: Builder<T>) : super(realm, builder) {
        resumeMode = builder.resumeMode
    }

    constructor(clazz: Class<T>): super(clazz)

    constructor(realm: Realm, clazz: Class<T>): super(realm, clazz)

    protected open fun isSameObj(obj: E, apiObj: X): Boolean = obj == apiObj

    protected open fun onBeforeInsertData(realm: Realm, apiObj: X) = Unit
    protected open fun onBeforeInsertData(realm: Realm, id: Any, apiObj: X) = Unit

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
                deleteFromApi(realm, localObj)
                Log.d(tag, "$localObj deleted")
            }
        }
    }

    protected open fun deleteFromApi(realm: Realm, localObj: E) {
        realm.where(mRealmClass).queryById(mPrimaryKeyFieldName, localObj.getId()).findFirst()?.deleteFromRealm()
    }

    open fun insertOrUpdateFromApi(offset: Int, apiList: List<X>, callback: (List<E>) -> Unit) {
        executeTransactionAsync(execute = { realm ->
            insertOrUpdateFromApi(offset, realm, apiList)
        }, onFinished = {
            getLocalList(offset) { _, value ->
                callback(value)
            }
        })
    }

    fun insertOrUpdateFromApiWithoutReturn(offset: Int, apiList: List<X>, callback: () -> Unit) {
        executeTransactionAsync(execute = { realm ->
            insertOrUpdateFromApi(offset, realm, apiList)
        }, onFinished = {
            callback()
        })
    }

    protected open fun getLocalListByApiResponse(offset: Int, realmQuery: RealmQuery<T>, apiList: List<X>): RealmResults<T> {
        val query = getQuery(offset, realmQuery)
        if(apiList.isNotEmpty()) {
            try {
                apiList[0].getId()
            } catch (e: RuntimeException) {
                Log.d(getTagLog(), "${apiList[0]} does not have primary key annotation")
                return query.findAll()
            }

            return query.or().queryByIds(mPrimaryKeyFieldName, apiList.map { it.getId() }).findAll()
        }
        return query.findAll()
    }

    fun insertOrUpdateFromApi(offset: Int, realm: Realm, apiList: List<X>) {
        val localList = map(getLocalListByApiResponse(offset, getRealmQueryTable(realm), apiList))
        handleDeletedDataFromApi(realm, localList, apiList)
        Log.d(tag, getTagLog() + ": insertOrUpdateList()")
        var order = offset
        apiList.forEach { apiObj ->
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
                setOrderFromApi(objToUpdate, order++)
                realm.insertOrUpdate(createRealmObj(objToUpdate))
            } else {
                val objToInsert = createObjFromObjApi(apiObj)
                setOrderFromApi(objToInsert, order++)
                realm.insertOrUpdate(createRealmObj(objToInsert))
            }
        }
    }

    protected open fun <R> setOrderFromApi(objToSave: R, order: Int) {
        if(objToSave is BaseContract) objToSave.order = order
    }

    protected open fun insertOrUpdateFromObjApi(id: Any, objApi: X, callback: (E?) -> Unit) {
        executeTransactionAsync(execute = { realm ->
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
            if(objLocal != null) setFieldsFromApi(createObj(objLocal), objToUpdate)
            if(objLocal != null && objToUpdate is BaseContract && objLocal is BaseContract) {
                objToUpdate.order = objLocal.order
            }
            realm.insertOrUpdate(createRealmObj(objToUpdate))
        }, onFinished = {
            getInsertedObj(id, objApi, callback)
        })
    }

    protected open fun getInsertedObj(id: Any, objApi: X, callback: (E?) -> Unit) = getLocalObj(id, callback)

    /**
     * Call this method inside a transaction.
     * Insert or update list in same thread that this method was call.
     * Don't call this method in the main thread
     */
    @WorkerThread
    fun insertOrUpdateHashList(realm: Realm, apiList: Set<X>) {
        apiList.forEach {
            realm.insertOrUpdate(createRealmObj(createObjFromObjApi(it)))
        }
    }

    @WorkerThread
    fun insertOrUpdateHashList(apiList: Set<X>) {
        getRealm().executeTransaction { realm ->
            apiList.forEach {
                realm.insertOrUpdate(createRealmObj(createObjFromObjApi(it)))
            }
        }
    }
}