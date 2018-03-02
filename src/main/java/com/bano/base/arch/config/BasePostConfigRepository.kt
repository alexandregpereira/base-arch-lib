package com.bano.base.arch.config

import io.realm.RealmObject

/**
 * Created by henrique.oliveira on 06/10/2017.
 *
 */
abstract class BasePostConfigRepository<in E , T, V>(clazz: Class<V>) : BaseConfigRepository<T, V>(clazz) where T:  RealmObject, T : Config {

    protected abstract fun getFromApi(postObj: E, api: V, onResponse: (response: Int, T?) -> Unit, onFailure: (t: Throwable) -> Unit)

    override fun getFromApi(api: V, onResponse: (response: Int, T?) -> Unit, onFailure: (t: Throwable) -> Unit) {}

    fun getRemote(e: E, callback: (response: Int, T?) -> Unit) {
        getRemote(callback) { api, onResponse, onFailure ->
            getFromApi(e, api, onResponse, onFailure)
        }
    }
}