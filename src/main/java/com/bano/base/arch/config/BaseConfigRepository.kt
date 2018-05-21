package com.bano.base.arch.config

import com.bano.base.BaseResponse
import com.bano.base.arch.Repository
import com.bano.base.auth.OAuth2Service
import com.bano.base.contract.BaseObjMapperContract
import com.bano.base.model.BaseApiRequestModel
import io.realm.Realm
import io.realm.RealmObject
import io.realm.RealmQuery
import java.net.SocketTimeoutException
import java.net.UnknownHostException


/**
 * Created by bk_alexandre.pereira on 03/08/2017.
 *
 */
abstract class BaseConfigRepository<T, V>(private val clazz: Class<V>) : Repository(), BaseObjMapperContract<T, T> where T:  RealmObject, T : Config {

    protected var inProgress = false
    private var mApiRequestModel: BaseApiRequestModel? = null

    protected abstract fun getFromApi(api: V, onResponse: (response: Int, T?) -> Unit, onFailure: (t: Throwable) -> Unit)
    protected abstract fun getRealmQueryTable(realm: Realm): RealmQuery<T>
    protected abstract fun createAPIRequestModel(): BaseApiRequestModel

    fun getLocal(): T? {
        val realm = Realm.getDefaultInstance()
        val realmObj = getDatabaseLocal(realm) ?: return null
        val configObj = createObj(realmObj)
        realm.close()
        return configObj
    }

    protected fun getLocal(realm: Realm): T? {
        val realmObj = getDatabaseLocal(realm) ?: return null
        return createObj(realmObj)
    }

    protected fun getDatabaseLocal(): T? = getRealmQueryTable(getRealm()).findFirst()
    protected fun getDatabaseLocal(realm: Realm): T? = getRealmQueryTable(realm).findFirst()

    fun getRemote(callback: (response: Int, T?) -> Unit) {
        getRemote(callback) { retrofit, onResponse, onFailure ->
            getFromApi(retrofit, onResponse, onFailure)
        }
    }

    protected fun getRemote(callback: (response: Int, T?) -> Unit, consumeApi: (api: V, (response: Int, T?) -> Unit, (t: Throwable) -> Unit) -> Unit) {
        if (inProgress) return

        val api = getApi()
        consumeApi(api, { response, config ->
            if (response != BaseResponse.HTTP_SUCCESS || config == null) {
                inProgress = false
                callback(response, config)
                return@consumeApi
            }
            insertOrUpdate(config) {
                callback(BaseResponse.HTTP_SUCCESS, config)
                inProgress = false
            }
        }, { throwable ->
            when (throwable) {
                is UnknownHostException -> sendCallbackError(BaseResponse.UNKNOWN_HOST, callback)
                is SocketTimeoutException -> sendCallbackError(BaseResponse.TIMEOUT_ERROR, callback)
                else -> sendCallbackError(BaseResponse.UNKNOWN_ERROR, callback)
            }
        })
    }

    private fun <K> sendCallbackError(errorCode: Int, callback: (responseCode: Int, K?) -> Unit){
        inProgress = false
        callback(errorCode, null)
    }

    fun insertOrUpdate(t: T, callback: () -> Unit) {
        getRealm().executeTransactionAsync( Realm.Transaction { realm ->
            realm.insertOrUpdate(createRealmObj(t))
        }, Realm.Transaction.OnSuccess {
            resetRealm()
            callback()
        })
    }

    protected fun getApi(): V {
        val requestModel = mApiRequestModel ?: createAPIRequestModel()
        mApiRequestModel = requestModel
        return OAuth2Service.buildRetrofitService(requestModel, clazz)
    }
}