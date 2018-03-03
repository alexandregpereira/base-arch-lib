@file:Suppress("UNCHECKED_CAST")

package com.bano.base.arch.main.embedded

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.bano.base.BaseResponse
import com.bano.base.arch.main.remote.BaseRemoteViewModel
import io.realm.RealmModel

/**
 * Created by bk_alexandre.pereira on 04/10/2017.
 *
 */
abstract class BaseEmbeddedViewModel<E, T, X, F, Z, Y> :
        BaseRemoteViewModel<E, T, X>() where T : RealmModel, Z : RealmModel {

    val holderMapLiveData = MutableLiveData<HolderMapResponse<E, F>>()
    val embeddedListLiveData = MutableLiveData<BaseResponse<List<F>>>()
    val embeddedObjLiveData = MutableLiveData<BaseResponse<F>>()

    fun load(id: Long) {
        val repository = getRepository() as? BaseEmbeddedListRepository<E, T, X, F, Z, Y, *> ?:  throw IllegalAccessException("repository must be BaseEmbeddedListRepository")
        val embeddedLocalList: List<F> = repository.getEmbeddedRepository(repository.getRealm(), id).getLocalList()
        holderMapLiveData.value = HolderMapResponse(repository.getLocalObj(id), embeddedLocalList, false)
        embeddedListLiveData.value = BaseResponse(embeddedLocalList)
        loadRemote(id)
    }

    fun loadRemote(id: Long) {
        val repository = getRepository() as? BaseEmbeddedListRepository<E, T, X, F, Z, Y, *> ?:  throw IllegalAccessException("repository must be BaseEmbeddedListRepository")
        loadingLiveData.value = true
        repository.getRemoteObj(id) { baseResponse ->
            loadingLiveData.value = false
            if(baseResponse.responseCode == BaseResponse.CACHE_MODE_CODE) return@getRemoteObj
            responseCodeLiveData.value = baseResponse.responseCode
            val embeddedLocalList = repository.getEmbeddedRepository(repository.getRealm(), id).getLocalList()
            val localObj = baseResponse.value ?: repository.getLocalObj(id)
            holderMapLiveData.value = HolderMapResponse(localObj, embeddedLocalList, true)
            embeddedListLiveData.value = BaseResponse(embeddedLocalList, true)
        }
    }

    fun reload(id: Long) {
        val repository = getRepository() as? BaseEmbeddedListRepository<E, T, X, F, Z, Y, *> ?:  throw IllegalAccessException("repository must be BaseEmbeddedListRepository")
        repository.clearObjData()
        loadRemote(id)
    }

    override fun loadLocal() {
        val repository = getRepository() as? BaseEmbeddedListRepository<E, T, X, F, Z, Y, *> ?:  throw IllegalAccessException("repository must be BaseEmbeddedListRepository")
        super.loadLocal()
        embeddedListLiveData.value = BaseResponse(repository.getDefaultEmbeddedRepository().getLocalList())
    }

    override fun loadRemote() {
        val repository = getRepository() as? BaseEmbeddedListRepository<E, T, X, F, Z, Y, *> ?:  throw IllegalAccessException("repository must be BaseEmbeddedListRepository")
        loadRemote {
            embeddedListLiveData.value = BaseResponse(repository.getDefaultEmbeddedRepository().getLocalList(), true)
        }
    }

    fun loadEmbeddedObjLocal(id: Long) {
        val repository = getRepository() as? BaseEmbeddedListRepository<E, T, X, F, Z, Y, *> ?:  throw IllegalAccessException("repository must be BaseEmbeddedListRepository")
        embeddedObjLiveData.value = BaseResponse(repository.getEmbeddedRepository(repository.getRealm(), id).getLocalObj(id))
    }

    fun loadEmbeddedListLocalAsRemote(idParent: Long) {
        val repository = getRepository() as? BaseEmbeddedListRepository<E, T, X, F, Z, Y, *> ?:  throw IllegalAccessException("repository must be BaseEmbeddedListRepository")
        embeddedListLiveData.value = BaseResponse(repository.getEmbeddedRepository(repository.getRealm(), idParent).getLocalList(), true)
    }

    fun updateEmbeddedObj(embeddedObj: F): LiveData<F> {
        val repository = getRepository() as? BaseEmbeddedListRepository<E, T, X, F, Z, Y, *> ?:  throw IllegalAccessException("repository must be BaseEmbeddedListRepository")
        val embeddedObjLiveData = MutableLiveData<F>()
        repository.embeddedRepository?.update(embeddedObj) {
            embeddedObjLiveData.value = embeddedObj
        }
        return embeddedObjLiveData
    }
}