package com.bano.base.arch.second.remote

import android.util.Log
import com.bano.base.BaseResponse
import com.bano.base.arch.second.RoomRepository
import com.bano.base.auth.OAuth2Service
import com.bano.base.contract.BaseContract
import com.bano.base.model.ApiRequestModel
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.realm.annotations.PrimaryKey
import java.lang.reflect.Modifier
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.*

abstract class RoomRemoteRepository<E, X : Any, V> : RoomRepository<E> {
    private val tag = "BaseRepository"
    private var remoteObj: E? = null
    private val clazz: Class<V>
    private var mApiRequestModel: ApiRequestModel<*>? = null
    private val mRequestsPool = TreeSet<RequestPoolItem<V, List<X>, List<E>>>()
    private var mRequestObjPoolItemInProgress = false
    private val mCachePool = HashSet<Int>()
    /**
     * set this flag when is necessary to specify different scenarios in setFieldFromApi method
     */
    var resumeMode: Boolean = false

    constructor(clazz: Class<V>): super() {
        this.clazz = clazz
    }

    constructor(clazz: Class<V>, builder: RoomRepository.Builder<E>): super(builder) {
        this.clazz = clazz
        resumeMode = builder.resumeMode
    }

    protected abstract fun getApiList(api: V, offset: Int, limit: Int, onResponse: (BaseResponse<List<X>>) -> Unit, onFailure: (t: Throwable) -> Unit)
    protected abstract fun getObjApi(api: V, id: Long, onResponse: (BaseResponse<X>) -> Unit, onFailure: (t: Throwable) -> Unit)
    protected abstract fun createAPIRequestModel(): ApiRequestModel<*>
    abstract fun getLocalList(offset: Int, limit: Int): List<E>
    abstract fun getLocalListAsync(offset: Int, limit: Int): Flowable<List<E>>
    abstract fun delete(e: E)
    abstract fun insertOrUpdate(objToSave: E)

    open fun getRemoteList(callback: (baseResponse: BaseResponse<List<E>>) -> Unit){
        getRemoteList(offset, callback, { api, offset, onResponse, onFailure ->
            getApiList(api, offset, limit, onResponse, onFailure)
        }, { offset, apiList, onStored ->
            insertOrUpdateList(offset, apiList).subscribe {
                onStored(it)
            }
        })
    }

    open fun getRemoteObj(id: Long, callback: (baseResponse: BaseResponse<E>) -> Unit) {
        getRemoteObj(callback, { api, _, onResponse, onFailure ->
            getObjApi(api, id, onResponse, onFailure)
        }, { _, objApi, onStored ->
            insertOrUpdateFromObjApi(id, objApi)
        })
    }

    protected fun getRemoteList(offset: Int, callback: (baseResponse: BaseResponse<List<E>>) -> Unit,
                                consumeApi: (api: V, offset: Int, (BaseResponse<List<X>>) -> Unit, (t: Throwable) -> Unit) -> Unit, storeApiData: (offset: Int, List<X>, (List<E>?) -> Unit) -> Unit) {
        getRemote(offset, callback, consumeApi, storeApiData, { isInProgress(offset) }, {
            val set = if(someRequestInProgress(it.offset)) {
                Log.d(tag, getTagLog() + ": someRequestInProgress(): $offset")
                false
            } else {
                it.inProgress = true
                true
            }
            mRequestsPool.remove(it)
            addRequestToThePool(it)
            if(!set) {
                val requestInProgress = mRequestsPool.first()
                getRemoteList(requestInProgress.offset, requestInProgress.callback, requestInProgress.consumeApi, requestInProgress.storeApiData)
            }
            set
        })
    }

    protected fun getRemoteObj(callback: (baseResponse: BaseResponse<E>) -> Unit,
                               consumeApi: (api: V, offset: Int, (BaseResponse<X>) -> Unit, (t: Throwable) -> Unit) -> Unit, storeApiData: (offset: Int, X, (E?) -> Unit) -> Unit) {
        getRemote(-1, callback, consumeApi, storeApiData, { mRequestObjPoolItemInProgress }, { setObjInProgress() })
    }

    private fun <K, N> getRemote(offset: Int,
                                 callback: (baseResponse: BaseResponse<N>) -> Unit,
                                 consumeApi: (api: V, offset: Int, (BaseResponse<K>) -> Unit, (t: Throwable) -> Unit) -> Unit,
                                 storeApiData: (offset: Int, K, (N?) -> Unit) -> Unit,
                                 isInProgress: () -> Boolean,
                                 setInProgress: (requestPoolItem: RequestPoolItem<V, K, N>) -> Boolean) {
        if(isInProgress()) {
            Log.d(tag, getTagLog() + ": isInProgress(): $offset")
            return
        }

        val requestPoolItem = RequestPoolItem(offset, callback, consumeApi, storeApiData)
        if(!isCached(offset)) {
            if(!setInProgress(requestPoolItem)) return
            Log.d(tag, getTagLog() + ": getRemote()")
            getApiAndConsume(false, requestPoolItem)
        }
        else {
            Log.d(tag, getTagLog() + ": isCached($offset)")
            sendCallbackError(BaseResponse(BaseResponse.CACHE_MODE_CODE), requestPoolItem)
        }
    }

    private fun someRequestInProgress(offset: Int): Boolean =
            mRequestsPool.any { it.offset < offset }

    private fun isInProgress(offset: Int): Boolean = mRequestsPool.any { it.offset == offset && it.inProgress }

    private fun addRequestToThePool(requestPoolItem: RequestPoolItem<V, List<X>, List<E>>) {
        Log.d(tag, getTagLog() + ": addRequestToThePool(${requestPoolItem.offset}) - size = ${mRequestsPool.size}")
        mRequestsPool.add(requestPoolItem)
    }

    private fun setObjInProgress(): Boolean {
        mRequestObjPoolItemInProgress = true
        return mRequestObjPoolItemInProgress
    }

    private fun removeRequestToThePool(requestPoolItemToRemove: RequestPoolItem<*, *, *>) {
        Log.d(tag, getTagLog() + ": removeRequestToThePool(${requestPoolItemToRemove.offset}) - size = ${mRequestsPool.size}")
        mRequestsPool.remove(requestPoolItemToRemove)
        if(mRequestsPool.isEmpty()) return
        val requestPoolItem = mRequestsPool.first()
        getRemoteList(requestPoolItem.offset, requestPoolItem.callback, requestPoolItem.consumeApi, requestPoolItem.storeApiData)
    }

    private fun setCached(offset: Int) {
        mCachePool.add(offset)
    }

    private fun clearCache() {
        mCachePool.clear()
        mRequestsPool.clear()
    }

    private fun isCached(offset: Int): Boolean =
            mCachePool.any { it == offset }

    private fun <K, N> getApiAndConsume(newTentative: Boolean,
                                        requestPoolItem: RequestPoolItem<V, K, N>) {
        val api = getApi()
        requestPoolItem.consumeApi(api, requestPoolItem.offset, { response ->
            if(!response.isSuccessful()) {
                if(BaseResponse.isErrorToChangeNavigation(response.responseCode) && !newTentative) {
                    mApiRequestModel?.refreshToken { responseCode ->
                        when (responseCode) {
                            BaseResponse.HTTP_SUCCESS -> {
                                Log.d("TokenRefresh", "${getTagLog()}: Token refreshed, trying again")
                                //Try again
                                getApiAndConsume(true, requestPoolItem)
                            }
                            BaseResponse.UNKNOWN_ERROR -> {
                                Log.e("TokenRefresh", "${getTagLog()}: Token refresh failed")
                                sendCallbackError(BaseResponse(BaseResponse.TOKEN_ERROR), requestPoolItem)
                            }
                            else -> {
                                Log.e("TokenRefresh", "${getTagLog()}: Token refresh failed")
                                sendCallbackError(BaseResponse(responseCode), requestPoolItem)
                            }
                        }
                    }
                    return@consumeApi
                }
                sendCallbackError(BaseResponse(response.responseCode), requestPoolItem)
                return@consumeApi
            }
            val apiData = response.value
            if(apiData == null) {
                sendCallbackError(BaseResponse(BaseResponse.UNKNOWN_ERROR), requestPoolItem)
                return@consumeApi
            }
            requestPoolItem.storeApiData(requestPoolItem.offset, apiData) {
                setCached(requestPoolItem.offset)
                val baseResponse = BaseResponse(response.responseCode, it, true)
                baseResponse.payload = response.payload
                requestPoolItem.callback(baseResponse)
                removeRequestToThePool(requestPoolItem)
                mRequestObjPoolItemInProgress = false
            }
        }, { throwable ->
            Log.e(getTagLog(), throwable.message)
            when (throwable) {
                is UnknownHostException -> sendCallbackError(BaseResponse(BaseResponse.UNKNOWN_HOST), requestPoolItem)
                is SocketTimeoutException -> sendCallbackError(BaseResponse(BaseResponse.TIMEOUT_ERROR), requestPoolItem)
                else -> sendCallbackError(BaseResponse(BaseResponse.UNKNOWN_ERROR), requestPoolItem)
            }
        })
    }

    protected fun getApi(): V {
        val requestModel = mApiRequestModel ?: createAPIRequestModel()
        mApiRequestModel = requestModel
        return OAuth2Service.buildRetrofitService(requestModel, clazz)
    }

    private fun <K, N> sendCallbackError(baseResponse: BaseResponse<N>, requestPoolItem: RequestPoolItem<V, K, N>){
        mRequestsPool.find { it.offset == requestPoolItem.offset }?.inProgress = false
        mRequestObjPoolItemInProgress = false
        requestPoolItem.callback(baseResponse)
    }

    fun clearData(){
        if(isInProgress(offset)) return
        resetPage()
        clearCache()
    }

    fun clearObjData() {
        if(isInProgress(offset)) return
        clearCache()
    }

    protected open fun onBeforeInsertData(objToUpdateList: List<X>) = Unit
    protected open fun onBeforeInsertData(id: Long, apiObj: X) = Unit

    protected open fun isSameObj(obj: E, apiObj: X): Boolean = obj == apiObj
    abstract fun createObjFromObjApi(apiObj: X): E

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
    protected open fun handleDeletedDataFromApi(localList: List<E>, apiList: List<X>) {
        localList.forEach { localObj ->
            if(localObj !is BaseContract) return@forEach
            val apiObj = apiList.find { isSameObj(localObj, it) }
            if (apiObj == null) {
                Log.d(tag, "$localObj excludeDate updated")
                localObj.excludeDate = Date().time
                delete(localObj)
            }
        }
    }

    fun insertOrUpdateList(offset: Int, apiList: List<X>): Flowable<List<E>> {
        return Flowable.just(apiList)
                .subscribeOn(Schedulers.io())
                .map { apiListRx ->
                    val localList = getLocalList(offset, limit)
                    val apiListFiltered = apiListRx.filter { it != null }

                    handleDeletedDataFromApi(localList, apiListFiltered)
                    Log.d(tag, getTagLog() + ": insertOrUpdateList()")
                    var order = offset
                    val objToUpdateList = apiListFiltered.map { apiObj ->
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
                            objToUpdate
                        } else {
                            val objToInsert = createObjFromObjApi(apiObj)
                            if(objToInsert is BaseContract) objToInsert.order = order++
                            objToInsert
                        }
                    }
                    insertOrUpdateListFromApi(apiListRx, objToUpdateList)
                    objToUpdateList
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    abstract fun insertOrUpdateListFromApi(apiList: List<X>, objToSaveList: List<E>)

    protected fun insertOrUpdateFromObjApi(id: Long, objApiTmp: X): Flowable<E?> {
        return Flowable.just(objApiTmp)
                .subscribeOn(Schedulers.io())
                .concatMap { objApi ->
                    val idField = objApi::javaClass.get().declaredFields.find { field ->
                        field.annotations.any { it is PrimaryKey }
                    }

                    if(idField != null) {
                        val idFieldValue = if(Modifier.isPublic(idField.modifiers)) {
                            idField.get(objApi)
                        }
                        else {
                            val methods = objApi::javaClass.get().declaredMethods
                            methods.find { it.name?.toLowerCase()?.contains("get${idField.name.toLowerCase()}") == true }?.invoke(objApi)
                        }

                        if(idFieldValue != null) getLocalObj(idFieldValue)
                        else Flowable.just(null)
                    } else {
                        Log.e(getTagLog(), "$objApi don't have PrimaryKey, the order will be missed")
                        Flowable.just(null)
                    }
                }
                .concatMap { objLocal ->
                    val objToUpdate = createObjFromObjApi(objApiTmp)
                    if(objLocal != null) setFieldsFromApi(objLocal, objToUpdate)
                    if(objLocal != null && objToUpdate is BaseContract && objLocal is BaseContract) {
                        objToUpdate.order = objLocal.order
                    }
                    insertOrUpdateObjFromApi(objApiTmp, objToUpdate)
                    getInsertedObj(id, objApiTmp)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    open fun insertOrUpdateObjFromApi(objApi: X, objToSave: E) {
        insertOrUpdate(objToSave)
    }

    protected open fun getInsertedObj(id: Long, objApi: X): Flowable<E?> = getLocalObj(id)

    class RequestPoolItem<V, K, N>(val offset: Int,
                                   val callback: (baseResponse: BaseResponse<N>) -> Unit,
                                   val consumeApi: (api: V, offset: Int, onResponse: (BaseResponse<K>) -> Unit, onFailure: (t: Throwable) -> Unit) -> Unit,
                                   val storeApiData: (offset: Int, K, (N?) -> Unit) -> Unit) : Comparable<RequestPoolItem<V, K, N>> {

        var inProgress: Boolean = false

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as RequestPoolItem<*, *, *>

            if (offset != other.offset) return false

            return true
        }

        override fun hashCode(): Int = offset

        override fun compareTo(other: RequestPoolItem<V, K, N>): Int = offset.compareTo(other.offset)
    }

}