package com.bano.base.arch.main.remote

import android.arch.lifecycle.MutableLiveData
import com.bano.base.BaseResponse
import com.bano.base.arch.main.BaseViewModel
import com.bano.base.contract.BaseContract
import io.realm.RealmModel

/**
 * Created by bk_alexandre.pereira on 18/09/2017.
 *
 */
abstract class BaseRemoteViewModel<E : BaseContract, T, X> :
        BaseViewModel<E, T, X>() where T : RealmModel, T : BaseContract {

    val responseCodeLiveData = MutableLiveData<Int>()
    val logoutLiveData = MutableLiveData<Boolean>()
    val loadingLiveData = MutableLiveData<Boolean>()
    val loadingPaginationLiveData = MutableLiveData<Boolean>()

    abstract fun logout(callback: () -> Unit)

    override fun loadObj(id: Long) {
        super.loadObj(id)
        loadObjRemote(id)
    }

    override fun load() {
        loadLocal()
        loadRemote()
    }

    fun reload(): Int {
        val repository = getRepository() as? BaseRemoteRepository<E, T, X, *> ?: throw IllegalAccessException("repository must be BaseRemoteRepository")
        repository.clearData()
        loadRemote()
        return repository.limit
    }

    fun reloadObj(id: Long): Int {
        val repository = getRepository() as? BaseRemoteRepository<E, T, X, *> ?: throw IllegalAccessException("repository must be BaseRemoteRepository")
        repository.clearObjData()
        loadObjRemote(id)
        return repository.limit
    }

    open fun loadRemote() {
        loadRemote(null)
    }

    protected fun loadRemote(callback: (() -> Unit)?) {
        loadRemote<List<E>>({ onResponse ->
            val repository = getRepository() as? BaseRemoteRepository<E, T, X, *> ?: throw IllegalAccessException("repository must be BaseRemoteRepository")
            repository.getRemoteList(onResponse)
        }, { baseResponse ->
            listLiveData.value = baseResponse
            callback?.invoke()
        })
    }

    protected fun <N> loadRemote(getRemote: ((baseResponse: BaseResponse<N>) -> Unit) -> Unit, callback: (baseResponse: BaseResponse<N>) -> Unit) {
        val offset = getRepository().offset
        if(offset == 0) loadingLiveData.value = true
        else loadingPaginationLiveData.value = true
        getRemote { baseResponse ->
            loadingLiveData.value = false
            loadingPaginationLiveData.value = false
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

    fun loadObjRemote(id: Long) {
        val repository = getRepository() as? BaseRemoteRepository<E, T, X, *> ?: throw IllegalAccessException("repository must be BaseRemoteRepository")
        loadingLiveData.value = true
        repository.getRemoteObj(id) { baseResponse ->
            loadingLiveData.value = false
            responseCodeLiveData.value = baseResponse.responseCode
            if(baseResponse.value == null) return@getRemoteObj
            objLiveData.value = baseResponse
        }
    }
}