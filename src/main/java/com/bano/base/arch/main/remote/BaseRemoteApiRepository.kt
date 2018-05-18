package com.bano.base.arch.main.remote

import android.util.Log
import com.bano.base.BaseResponse
import com.bano.base.auth.OAuth2Service
import com.bano.base.contract.MapperContract
import com.bano.base.model.ApiRequestModel
import io.realm.Realm
import io.realm.RealmModel
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.*
import kotlin.collections.HashSet

/**
 * Created by bk_alexandre.pereira on 15/09/2017.
 *
 */
abstract class BaseRemoteApiRepository<E : Any, T, X : Any, V> : BaseRemoteRepository<E, T, X>, MapperContract<E, T, X> where T : RealmModel {
    private val tag = "BaseRepository"
    private var remoteObj: E? = null
    private val clazz: Class<V>
    private var mApiRequestModel: ApiRequestModel<*>? = null
    private val mRequestsPool = TreeSet<RequestPoolItem<V, List<X>, List<E>>>()
    private var mRequestObjPoolItemInProgress = false
    private val mCachePool = HashSet<Int>()

    constructor(realmClass: Class<T>, clazz: Class<V>): super(realmClass) {
        this.clazz = clazz
    }

    constructor(clazz: Class<V>, builder: Builder<T>): super(builder) {
        this.clazz = clazz
    }

    constructor(realm: Realm, clazz: Class<V>, builder: Builder<T>): super(realm, builder) {
        this.clazz = clazz
    }

    constructor(realm: Realm, realmClass: Class<T>, clazz: Class<V>): super(realm, realmClass) {
        this.clazz = clazz
    }

    protected abstract fun getApiList(api: V, offset: Int, limit: Int, onResponse: (BaseResponse<List<X>>) -> Unit, onFailure: (t: Throwable) -> Unit)
    protected abstract fun getObjApi(api: V, id: Any, onResponse: (BaseResponse<X>) -> Unit, onFailure: (t: Throwable) -> Unit)
    protected abstract fun createAPIRequestModel(): ApiRequestModel<*>

    open fun getRemoteList(callback: (baseResponse: BaseResponse<List<E>>) -> Unit){
        getRemoteList(offset, callback, { api, offset, onResponse, onFailure ->
            getApiList(api, offset, limit, onResponse, onFailure)
        }, { offset, apiList, onStored ->
            insertOrUpdateFromApi(offset, apiList) {
                onStored(it)
            }
        })
    }

    open fun getRemoteObj(id: Any, callback: (baseResponse: BaseResponse<E>) -> Unit) {
        getRemoteObj(callback, { api, _, onResponse, onFailure ->
            getObjApi(api, id, onResponse, onFailure)
        }, { _, objApi, onStored ->
            insertOrUpdateFromObjApi(id, objApi) {
                onStored(it)
            }
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