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
abstract class BaseViewModel<E, T, X : Any> :
        ViewModel() where T : RealmModel {

    var idParent: Long? = null
    var total: Int? = null
    private var mRepository: BaseRepository<E, T, X>? = null

    open val listLiveData = MutableLiveData<BaseResponse<List<E>>>()
    open val objLiveData = MutableLiveData<BaseResponse<E>>()

    protected abstract fun createRepository(idParent: Long?): BaseRepository<E, T, X>

    protected fun getRepository(): BaseRepository<E, T, X> {
        val repository =
                if(mRepository?.idParent != idParent) {
                    createRepository(idParent)
                }
                else mRepository ?: createRepository(idParent)
        repository.total = total
        mRepository = repository
        return repository
    }

    open fun loadObj(id: Long) {
        loadObjLocal(id)
    }

    open fun load() {
        loadLocal()
    }

    open fun loadLocal() {
        listLiveData.value = BaseResponse(getRepository().getLocalList())
    }

    open fun loadObjLocal(id: Any) {
        objLiveData.value = BaseResponse(getRepository().getLocalObj(id))
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