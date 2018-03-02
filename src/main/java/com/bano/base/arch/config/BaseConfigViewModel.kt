package com.bano.base.arch.config

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import io.realm.RealmObject

/**
 * Created by bk_alexandre.pereira on 25/09/2017.
 *
 */
abstract class BaseConfigViewModel<T>(app: Application, open protected val repository: BaseConfigRepository<T, *>) : AndroidViewModel(app) where T:  RealmObject, T : Config {
    val configObjLiveData = MutableLiveData<T>()
    val responseCodeLiveData = MutableLiveData<Int>()

    open fun load() {
        load(null)
    }

    protected fun load(callback: ((T?) -> Unit)?) {
        loadRemote { response, configObj ->
            if(configObj == null) {
                loadLocal(callback)
                return@loadRemote
            }
            responseCodeLiveData.value = response
            configObjLiveData.value = configObj
            callback?.invoke(configObj)
        }
    }

    protected fun loadRemote(callback: (response: Int, T?) -> Unit) {
        repository.getRemote(callback)
    }

    fun loadRemote() {
        loadRemote { response, configObj ->
            responseCodeLiveData.value = response
            configObjLiveData.value = configObj
        }
    }

    fun loadLocal() {
        loadLocal(null)
    }

    protected fun loadLocal(callback: ((T?) -> Unit)?) {
        val configObj = repository.getLocal()
        configObjLiveData.value = configObj
        callback?.invoke(configObj)
    }
}