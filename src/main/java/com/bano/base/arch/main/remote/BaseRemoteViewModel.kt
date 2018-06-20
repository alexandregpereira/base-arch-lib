package com.bano.base.arch.main.remote

import android.arch.lifecycle.MutableLiveData
import com.bano.base.BaseResponse
import com.bano.base.arch.main.BaseViewModel
import io.realm.RealmModel

/**
 * Created by bk_alexandre.pereira on 18/09/2017.
 *
 */
@Suppress("UNCHECKED_CAST")
abstract class BaseRemoteViewModel<E : Any, T, X : Any> :
        BaseViewModel<E, T>() where T : RealmModel {

    val responseCodeLiveData = MutableLiveData<Int>()
    val logoutLiveData = MutableLiveData<Boolean>()
    val loadingLiveData = MutableLiveData<Boolean>()
    val loadingPaginationLiveData = MutableLiveData<Boolean>()

    abstract fun logout(callback: () -> Unit)

    override fun loadObj(id: Any) {
        super.loadObj(id)
        loadObjRemote(id)
    }

    override fun load() {
        loadLocal()
        loadRemote()
    }

    fun reload(): Int {
        val repository = getRepository() as? BaseRemoteApiRepository<E, T, X, *> ?: throw IllegalAccessException("repository must be BaseRemoteApiRepository")
        repository.clearData()
        loadRemote()
        return repository.limit
    }

    fun reloadObj(id: Any): Int {
        val repository = getRepository() as? BaseRemoteApiRepository<E, T, X, *> ?: throw IllegalAccessException("repository must be BaseRemoteApiRepository")
        repository.clearObjData()
        loadObjRemote(id)
        return repository.limit
    }

    open fun loadRemote() {
        loadRemote(null)
    }

    protected fun loadRemote(callback: (() -> Unit)?) {
        loadRemote<List<E>>({ onResponse ->
            val repository = getRepository() as? BaseRemoteApiRepository<E, T, X, *> ?: throw IllegalAccessException("repository must be BaseRemoteApiRepository")
            repository.getRemoteList(onResponse)
        }, { baseResponse ->
            listLiveData.value = baseResponse
            callback?.invoke()
        })
    }

    protected fun <N> loadRemote(getRemote: ((baseResponse: BaseResponse<N>) -> Unit) -> Unit, callback: (baseResponse: BaseResponse<N>) -> Unit) {
        val repository = getRepository()
        val offset = getRepository().offset
        if(offset == 0) loadingLiveData.value = true
        else loadingPaginationLiveData.value = true
        getRemote { baseResponse ->
            loadingLiveData.value = false
            loadingPaginationLiveData.value = (repository as? BaseRemoteApiRepository<*, *, *, *>)?.isInProgress() ?: false
            if(offset == 0) responseCodeLiveData.value = baseResponse.responseCode
            if(BaseResponse.isErrorToChangeNavigation(baseResponse.responseCode)) {
                logout {
                    logoutLiveData.value = true
                }
                return@getRemote
            }
            if(baseResponse.responseCode == BaseResponse.CACHE_MODE_CODE) {
                return@getRemote
            }
            callback(baseResponse)
        }
    }

    open fun loadObjRemote(id: Any) {
        val repository = getRepository() as? BaseRemoteApiRepository<E, T, X, *> ?: throw IllegalAccessException("repository must be BaseRemoteApiRepository")
        loadingLiveData.value = true
        repository.getRemoteObj(id) { baseResponse ->
            loadingLiveData.value = false
            responseCodeLiveData.value = baseResponse.responseCode
            if(baseResponse.value == null) return@getRemoteObj
            objLiveData.value = baseResponse
        }
    }
}