package com.bano.base.arch.main

import android.os.Handler
import android.os.HandlerThread
import android.support.annotation.WorkerThread
import android.util.Log
import com.bano.base.annotation.IdParent
import com.bano.base.arch.Repository
import com.bano.base.contract.BaseObjMapperContract
import com.bano.base.contract.queryById
import com.bano.base.contract.toArrayList
import com.bano.base.util.RepositoryUtil
import io.realm.Realm
import io.realm.RealmModel
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.annotations.PrimaryKey


/**
 * Created by bk_alexandre.pereira on 25/07/2017.
 *
 */
abstract class BaseRepository<E, T> : Repository, BaseObjMapperContract<E, T> where T : RealmModel  {
    private val tag = "BaseRepository"
    var idParent: Long?

    val limit: Int
    var offset: Int = 0
        private set
    var total: Int? = null
    private val mOrderFieldName: String?
    protected val mPrimaryKeyFieldName: String
    protected val mRealmClass: Class<T>

    constructor(clazz: Class<T>): super() {
        idParent = null
        limit = 0
        mOrderFieldName = null
        mRealmClass = clazz
        mPrimaryKeyFieldName = getPrimaryKeyFieldName(clazz)
    }

    constructor(builder: Builder<T>): super() {
        idParent = builder.idParent
        limit = builder.limit
        mOrderFieldName = builder.orderFieldName
        offset = 0
        mRealmClass = builder.realmClass
        mPrimaryKeyFieldName = getPrimaryKeyFieldName(mRealmClass)
    }

    constructor(realm: Realm, builder: Builder<T>): super(realm) {
        idParent = builder.idParent
        limit = builder.limit
        mOrderFieldName = builder.orderFieldName
        offset = 0
        mRealmClass = builder.realmClass
        mPrimaryKeyFieldName = getPrimaryKeyFieldName(mRealmClass)
    }

    constructor(realm: Realm, clazz: Class<T>) : super(realm) {
        idParent = null
        limit = 0
        offset = 0
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

    open fun getRealmQueryTable(realm: Realm): RealmQuery<T> = realm.where(mRealmClass)

    open fun getLocalObj(id: Any): E? {
        val e = when(id) {
            is Long -> getLocalObj(getRealmQueryTable(getRealm()).equalTo(mPrimaryKeyFieldName, id))
            is String -> getLocalObj(getRealmQueryTable(getRealm()).equalTo(mPrimaryKeyFieldName, id))
            is Int -> getLocalObj(getRealmQueryTable(getRealm()).equalTo(mPrimaryKeyFieldName, id))
            else -> throw IllegalArgumentException("$id is not supported")

        }
        resetRealm()
        return e
    }

    open fun getLocalObj(id: Any, callback: (E?) -> Unit) {
        executeRealmInAsync(execute = { realm ->
            getLocalObj(realm, id)
        }, callback = callback)
    }

    protected open fun getLocalObj(realm: Realm, id: Any): E? {
        return getLocalObj(getRealmQueryTable(realm).queryById(mPrimaryKeyFieldName, id))
    }


    fun getLocalObj(realmQuery: RealmQuery<T>): E? {
        val realmModel = realmQuery.findFirst() ?: return null
        return createObj(realmModel)
    }

    fun getRealmQueryTable(): RealmQuery<T> {
        val idParentFieldName = getIdParentFieldName()
        return if (idParent == null || idParentFieldName == null) getRealmQueryTable(getRealm())
        else getRealmQueryTable(getRealm()).equalTo(idParentFieldName, idParent)
    }

    open fun getLocalList(callback: (offset: Int, List<E>) -> Unit) {
        getLocalList(offset, callback)
    }

    open fun getLocalList(offset: Int, callback: (offset: Int, List<E>) -> Unit) {
        executeRealmInAsync(offset,execute = { realm ->
            getLocalList(realm, offset)
        }, callback = callback)
    }

    protected open fun getLocalList(realm: Realm, offset: Int): List<E> {
        return getQueryByOrder(offset, getRealmQueryTable(realm)).toArrayList { createObj(it) }
    }

    open fun getLocalList(): List<E> {
        val eList = getDatabaseList().toArrayList(this)
        resetRealm()
        return eList
    }
    fun getLocalList(orderFieldName: String): List<E> {
        val eList = getDatabaseList(orderFieldName).toArrayList(this)
        resetRealm()
        return eList
    }
    fun getLocalList(realmQuery: RealmQuery<T>): List<E> {
        val eList = getDatabaseList(realmQuery).toArrayList(this)
        resetRealm()
        return eList
    }
    private fun getDatabaseList(): RealmResults<T> = getQueryByOrder(getRealmQueryTable())
    private fun getDatabaseList(orderFieldName: String): RealmResults<T> = getQueryByOrder(orderFieldName, getRealmQueryTable())
    private fun getDatabaseList(realmQuery: RealmQuery<T>): RealmResults<T> = getQueryByOrder(realmQuery)

    protected open fun getQueryByOrder(realmQuery: RealmQuery<T>): RealmResults<T> =
            getQueryByOrder(offset, realmQuery)

    protected fun getQueryByOrder(offset: Int, realmQuery: RealmQuery<T>): RealmResults<T> {
        return getQueryByOrder(offset, mOrderFieldName, realmQuery)
    }

    private fun getQueryByOrder(orderFieldName: String?, realmQuery: RealmQuery<T>): RealmResults<T> {
        return getQueryByOrder(offset, orderFieldName, realmQuery)
    }

    private fun getQueryByOrder(offset: Int, orderFieldName: String?, realmQuery: RealmQuery<T>): RealmResults<T> {
        val idParentFieldName = getIdParentFieldName()
        if (idParent != null && idParentFieldName != null) realmQuery.equalTo(idParentFieldName, idParent)

        if(limit > 0 && orderFieldName != null)
            realmQuery.between(orderFieldName, offset, (offset + limit) - 1)

        if(orderFieldName != null)
            realmQuery.notEqualTo(orderFieldName, NO_ORDER).sort(orderFieldName)

        return realmQuery.findAll()
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

    @WorkerThread
    open fun deleteInMainThread(getRealmQuery: (realmQuery: RealmQuery<T>) -> RealmQuery<T>) {
        getRealm().executeTransaction { realm ->
            getDatabaseList(getRealmQuery(getRealmQueryTable(realm))).deleteAllFromRealm()
        }
        resetRealm()
    }

    @WorkerThread
    open fun deleteAllInMainThread() {
        getRealm().executeTransaction { realm ->
            getDatabaseList(getRealmQueryTable(realm)).deleteAllFromRealm()
        }
        resetRealm()
    }

    @WorkerThread
    fun insertOrUpdate(realm: Realm, apiList: List<E>) {
        apiList.forEach {
            realm.insertOrUpdate(createRealmObj(it))
        }
    }

    @WorkerThread
    fun insertOrUpdate(list: List<E>) {
        getRealm().executeTransaction { realm ->
            list.forEach {
                realm.insertOrUpdate(createRealmObj(it))
            }
        }
        resetRealm()
    }

    open fun update(e: E, callback: () -> Unit) {
        executeTransactionAsync(execute = { realm ->
            realm.insertOrUpdate(createRealmObj(e))
            Log.d(tag, "$e updated")
        }, onFinished = {
            callback()
        })
    }

    @WorkerThread
    open fun update(e: E) {
        getRealm().executeTransaction { realm ->
            realm.insertOrUpdate(createRealmObj(e))
            Log.d(tag, "$e updated")
        }
        resetRealm()
    }

    fun update(eList: List<E>, callback: () -> Unit) {
        executeTransactionAsync(execute = { realm ->
            eList.forEach {
                realm.insertOrUpdate(createRealmObj(it))
                Log.d(tag, "$it updated")
            }
        }, onFinished = {
            callback()
        })
    }

    @WorkerThread
    fun update(eList: List<E>) {
        getRealm().executeTransaction { realm ->
            eList.forEach {
                realm.insertOrUpdate(createRealmObj(it))
                Log.d(tag, "$it updated")
            }
        }
        resetRealm()
    }

    @WorkerThread
    fun update(eList: Set<E>) {
        getRealm().executeTransaction { realm ->
            eList.forEach {
                realm.insertOrUpdate(createRealmObj(it))
                Log.d(tag, "$it updated")
            }
        }
        resetRealm()
    }

    open fun insertOrUpdate(e: E, callback: (e: E) -> Unit) {
        executeTransactionAsync(execute = { realm ->
            realm.insertOrUpdate(createRealmObj(e))
        }, onFinished = {
            callback(e)
        })
    }

    @WorkerThread
    open fun insertOrUpdate(e: E) {
        getRealm().executeTransaction{ realm ->
            realm.insertOrUpdate(createRealmObj(e))
        }
        resetRealm()
    }

    fun insertOrUpdateNoCallback(t: T, callback: (e: E) -> Unit) {

        val handlerThread = HandlerThread("MyHandlerThread")
        handlerThread.start()
        val looper = handlerThread.looper
        val handler = Handler(looper)
        handler.post{

            getRealm().executeTransactionAsync(Realm.Transaction { realm ->
                realm.insertOrUpdate(t)
            }, Realm.Transaction.OnSuccess {
                callback(createObj(t))
            })

        }
    }

    protected open fun executeTransactionAsync(execute: (Realm) -> Unit, onFinished: () -> Unit) {
        val mainRealm = getRealm()
        mainRealm.executeTransactionAsync(Realm.Transaction { realm ->
            execute(realm)
        }, Realm.Transaction.OnSuccess {
            mainRealm.close()
            onFinished()
        })
    }

    protected open fun <K> executeRealmInAsync(execute: (realm: Realm) -> K, callback: (K) -> Unit) {
        RepositoryUtil.executeRealmInAsyncHandlerThread(execute = { realm ->
            execute(realm)
        }, callback = callback)
    }

    protected open fun <K, R> executeRealmInAsync(r: R, execute: (realm: Realm) -> K, callback: (R, K) -> Unit) {
        RepositoryUtil.executeRealmInAsyncHandlerThread(r, execute = { realm ->
            execute(realm)
        }, callback = callback)
    }

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

    companion object {
        const val NO_ORDER = -1
    }
}