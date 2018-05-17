package com.bano.base.arch.main.post

import com.bano.base.BaseResponse
import com.bano.base.arch.main.remote.BaseRemoteRepository
import com.bano.base.contract.BaseContract
import io.realm.Realm
import io.realm.RealmModel

/**
 * Created by bk_alexandre.pereira on 20/10/2017.
 *
 */
abstract class BasePostRepository<E, T, X : Any, V, K : Any> : BaseRemoteRepository<E, T, X, V>
        where T : RealmModel, T : BaseContract {

    constructor(realmClass: Class<T>, clazz: Class<V>): super(realmClass, clazz)

    constructor(realm: Realm, realmClass: Class<T>, clazz: Class<V>): super(realm, realmClass, clazz)

    constructor(clazz: Class<V>, builder: Builder<T>): super(clazz, builder)

    constructor(realm: Realm, clazz: Class<V>, builder: Builder<T>): super(realm, clazz, builder)

    abstract protected fun getApiList(api: V, objDto: K, onResponse: (BaseResponse<List<X>>) -> Unit, onFailure: (t: Throwable) -> Unit)
    abstract protected fun getObjApiDto(api: V, objDto: K, onResponse: (BaseResponse<X>) -> Unit, onFailure: (t: Throwable) -> Unit)

    fun getRemoteObjDto(dto: K, callback: (baseResponse: BaseResponse<E>) -> Unit) {
        getRemoteObj(callback, { api, _, onResponse, onFailure ->
            getObjApi(api, dto, onResponse, onFailure)
        }, { _, apiObj, onStored ->
            insertOrUpdate(createObjFromObjApi(apiObj)) {
                onStored(it)
            }
        })
    }
}