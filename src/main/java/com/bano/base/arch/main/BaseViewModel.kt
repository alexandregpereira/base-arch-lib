package com.bano.base.arch.main

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.bano.base.BaseResponse
import com.bano.base.contract.BaseContract
import io.realm.RealmModel

/**
 * Created by bk_alexandre.pereira on 18/09/2017.
 *
 */
abstract class BaseViewModel<E : BaseContract, T, X> :
        ViewModel() where T : RealmModel, T : BaseContract {

    var idParent: Long? = null
    var total: Int? = null
    private var mRepository: BaseRepository<E, T, X>? = null

    open val listLiveData = MutableLiveData<BaseResponse<List<E>>>()
    open val objLiveData = MutableLiveData<BaseResponse<E>>()

    abstract protected fun createRepository(idParent: Long?): BaseRepository<E, T, X>

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

    open fun loadObjLocal(id: Long) {
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

    fun updateInAsync(obj: E) {
        getRepository().update(obj) {
            objLiveData.postValue(BaseResponse(obj))
        }
    }

    fun updateInAsync(objList: List<E>) {
        getRepository().update(objList) {  }
    }
}