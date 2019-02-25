@file:Suppress("UNCHECKED_CAST")

package com.bano.base.arch.main.embedded

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bano.base.BaseResponse
import com.bano.base.arch.main.remote.BaseRemoteRepository
import com.bano.base.arch.main.remote.BaseRemoteViewModel
import io.realm.RealmModel

/**
 * Created by bk_alexandre.pereira on 04/10/2017.
 *
 */
abstract class BaseEmbeddedViewModel<E : Any, T, X : Any, F : Any> :
        BaseRemoteViewModel<E, T, X>() where T : RealmModel {

    val holderMapLiveData = MutableLiveData<HolderMapResponse<E, F>>()
    val embeddedListLiveData = MutableLiveData<BaseResponse<List<F>>>()
    val embeddedObjLiveData = MutableLiveData<BaseResponse<F>>()

    abstract fun getEmbeddedRepository(embeddedRepositoryList: List<BaseRemoteRepository<*, *, *>>): BaseRemoteRepository<F, *, *>

    open fun load(id: Long) {
        loadLocal(id)
        loadRemote(id)
    }

    fun loadLocal(id: Long) {
        val repository = getRepository() as? BaseEmbeddedListRemoteRepository<E, T, X, *> ?:  throw IllegalAccessException("repository must be BaseEmbeddedListRepository")
        val embeddedRepositoryList = repository.getEmbeddedRepositoryList(repository.getRealm(), id)
        val embeddedLocalList: List<F> = getEmbeddedRepository(embeddedRepositoryList).getLocalList()
        holderMapLiveData.value = HolderMapResponse(repository.getLocalObj(id), embeddedLocalList, false)
        embeddedListLiveData.value = BaseResponse(embeddedLocalList)
    }

    fun loadRemote(id: Long) {
        val repository = getRepository() as? BaseEmbeddedListRemoteRepository<E, T, X, *> ?:  throw IllegalAccessException("repository must be BaseEmbeddedListRepository")
        loadingLiveData.value = true
        repository.getRemoteObj(id) { baseResponse ->
            loadingLiveData.value = false
            if(baseResponse.responseCode == BaseResponse.CACHE_MODE_CODE) return@getRemoteObj
            responseCodeLiveData.value = baseResponse.responseCode
            onLoadRemote(id, baseResponse, repository)
        }
    }

    protected open fun onLoadRemote(id: Long, baseResponse: BaseResponse<E>, repository: BaseEmbeddedListRemoteRepository<E, T, X, *>) {
        val embeddedRepositoryList = repository.getEmbeddedRepositoryList(repository.getRealm(), id)
        val embeddedLocalList = getEmbeddedRepository(embeddedRepositoryList).getLocalList()
        val localObj = baseResponse.value ?: repository.getLocalObj(id)
        holderMapLiveData.value = HolderMapResponse(localObj, embeddedLocalList, true)
        embeddedListLiveData.value = BaseResponse(embeddedLocalList, true)
    }

    fun reload(id: Long) {
        val repository = getRepository() as? BaseEmbeddedListRemoteRepository<E, T, X, *> ?:  throw IllegalAccessException("repository must be BaseEmbeddedListRepository")
        repository.clearObjData()
        loadRemote(id)
    }

    fun loadEmbeddedObjLocal(id: Any) {
        val repository = getRepository() as? BaseEmbeddedListRemoteRepository<E, T, X, *> ?:  throw IllegalAccessException("repository must be BaseEmbeddedListRepository")
        val embeddedRepositoryList = repository.createEmbeddedRepositoryList(repository.getRealm(), null)
        embeddedObjLiveData.value = BaseResponse(getEmbeddedRepository(embeddedRepositoryList).getLocalObj(id))
    }

    fun loadEmbeddedListLocalAsRemote(idParent: Long) {
        val repository = getRepository() as? BaseEmbeddedListRemoteRepository<E, T, X, *> ?:  throw IllegalAccessException("repository must be BaseEmbeddedListRepository")
        val embeddedRepositoryList = repository.getEmbeddedRepositoryList(repository.getRealm(), idParent)
        embeddedListLiveData.value = BaseResponse(getEmbeddedRepository(embeddedRepositoryList).getLocalList(), true)
    }

    fun updateEmbeddedObj(embeddedObj: F): LiveData<F> {
        val repository = getRepository() as? BaseEmbeddedListRemoteRepository<E, T, X, *> ?:  throw IllegalAccessException("repository must be BaseEmbeddedListRepository")
        val embeddedObjLiveData = MutableLiveData<F>()
        val list = repository.embeddedRepositoryList
        if(list != null) getEmbeddedRepository(list).update(embeddedObj) {
            embeddedObjLiveData.value = embeddedObj
        }
        return embeddedObjLiveData
    }
}