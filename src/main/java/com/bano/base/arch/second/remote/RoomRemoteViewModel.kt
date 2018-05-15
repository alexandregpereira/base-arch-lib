package com.bano.base.arch.second.remote

import android.arch.lifecycle.MutableLiveData
import com.bano.base.BaseResponse
import com.bano.base.arch.second.RoomViewModel
import com.bano.base.contract.BaseRemoteViewModelContract

@Suppress("UNCHECKED_CAST")
abstract class RoomRemoteViewModel<E, X: Any> : RoomViewModel<E>(), BaseRemoteViewModelContract {

    override val responseCodeLiveData = MutableLiveData<Int>()
    override val logoutLiveData = MutableLiveData<Boolean>()
    override val loadingLiveData = MutableLiveData<Boolean>()
    override val loadingPaginationLiveData = MutableLiveData<Boolean>()

    override fun loadObj(id: Long) {
        super.loadObj(id)
        loadObjRemote(id)
    }

    override fun loadNextPage(){
        val repository = getRepository()
        repository.nextPage()
        loadRemote()
    }

    override fun reload(): Int {
        val repository = getRepository() as? RoomRemoteRepository<*, *, *>
                ?: throw IllegalAccessException("repository must be BaseRemoteRepository")
        repository.clearData()
        loadRemote()
        return repository.limit
    }

    override fun reloadObj(id: Long): Int {
        val repository = getRepository() as? RoomRemoteRepository<*, *, *>
                ?: throw IllegalAccessException("repository must be BaseRemoteRepository")
        repository.clearObjData()
        loadObjRemote(id)
        return repository.limit
    }

    override fun loadRemote() {
        loadRemote<List<E>> { onResponse ->
            val repository = getRepository() as? RoomRemoteRepository<E, X, *>
                    ?: throw IllegalAccessException("repository must be BaseRemoteRepository")
            repository.getRemoteList(onResponse)
        }
    }


    protected fun <N> loadRemote(getRemote: ((baseResponse: BaseResponse<N>) -> Unit) -> Unit) {
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
        }
    }

    override fun loadObjRemote(id: Long) {
        val repository = getRepository() as? RoomRemoteRepository<E, X, *>
                ?: throw IllegalAccessException("repository must be BaseRemoteRepository")
        loadingLiveData.value = true
        repository.getRemoteObj(id) { baseResponse ->
            loadingLiveData.value = false
            responseCodeLiveData.value = baseResponse.responseCode
            if(baseResponse.value == null) return@getRemoteObj
            objLiveData.value = baseResponse
        }
    }
}