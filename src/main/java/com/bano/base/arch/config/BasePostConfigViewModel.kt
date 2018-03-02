package com.bano.base.arch.config

import android.app.Application
import io.realm.RealmObject

/**
 * Created by henrique.oliveira on 06/10/2017.
 *
 */
abstract class BasePostConfigViewModel<in E, T >(app: Application, override val repository: BasePostConfigRepository<E, T, *>) : BaseConfigViewModel<T>(app, repository) where T:  RealmObject, T : Config {

    open fun loadRemote(e: E) {
        loadRemote(e, null)
    }

    protected fun loadRemote(e: E, callback: ((T?) -> Unit)?) {
        repository.getRemote(e) { response, config ->
            responseCodeLiveData.value = response
            configObjLiveData.value = config
            callback?.invoke(config)
        }
    }
}