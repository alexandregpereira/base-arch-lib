package com.bano.base.arch.main

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.bano.base.BaseResponse
import io.realm.RealmModel

/**
 * Created by bk_alexandre.pereira on 18/09/2017.
 *
 */
abstract class BaseViewModel<E : Any, T> :
        ViewModel() where T : RealmModel {

    var idParent: Long? = null
    var total: Int? = null
    private var mRepository: BaseRepository<E, T>? = null

    open val listLiveData = MutableLiveData<BaseResponse<List<E>>>()
    open val objLiveData = MutableLiveData<BaseResponse<E>>()

    protected abstract fun createRepository(idParent: Long?): BaseRepository<E, T>

    protected fun getRepository(): BaseRepository<E, T> {
        val repository =
                if(mRepository?.idParent != idParent) {
                    createRepository(idParent)
                }
                else mRepository ?: createRepository(idParent)
        if(repository.total == null) {
            repository.total = total
        }
        mRepository = repository
        return repository
    }

    open fun loadObj(id: Any) {
        loadObjLocal(id)
    }

    open fun load() {
        loadLocal()
    }

    open fun loadLocal() {
        listLiveData.value = null
        getRepository().getLocalList { offset, value ->
            listLiveData.value = BaseResponse(offset, value)
        }
    }

    open fun loadObjLocal(id: Any) {
        getRepository().getLocalObj(id) {
            objLiveData.value = BaseResponse(it)
        }
    }

    fun loadNextPage(){
        val repository = getRepository()
        repository.nextPage()
        load()
    }

    fun resetPage() {
        getRepository().resetPage()
    }

    fun update(obj: E): LiveData<E> {
        val objLiveDataTmp = MutableLiveData<E>()
        getRepository().update(obj) {
            objLiveDataTmp.value = obj
            objLiveData.value = BaseResponse(obj)
        }
        return objLiveDataTmp
    }

    fun updateInAsync(objList: List<E>) {
        getRepository().update(objList) {  }
    }
}