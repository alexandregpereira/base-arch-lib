package com.bano.base.arch.second

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.paging.PagedList
import com.bano.base.BaseResponse
import com.bano.base.contract.BaseViewModelContract

abstract class RoomViewModel<E> : ViewModel(), BaseViewModelContract<E> {

    var idParent: Long? = null
    private var mRepository: RoomRepository<E>? = null

    val totalLiveData = getRepository().totalLiveData

    override val listLiveData = MutableLiveData<BaseResponse<List<E>>>()
    override val objLiveData = MutableLiveData<BaseResponse<E>>()

    protected abstract fun createRepository(idParent: Long?): RoomRepository<E>

    protected fun getRepository(): RoomRepository<E> {
        val repository =
                if(mRepository?.idParent != idParent) {
                    createRepository(idParent)
                }
                else mRepository ?: createRepository(idParent)
        mRepository = repository
        return repository
    }

    override fun loadObj(id: Long) {
        loadObjLocal(id)
    }

    override fun loadObjLocal(id: Any) {

    }

    override fun resetPage() {
        getRepository().resetPage()
    }

    override fun update(obj: E): LiveData<E> {
        val objLiveDataTmp = MutableLiveData<E>()
        getRepository().update(obj) {
            objLiveDataTmp.value = obj
            objLiveData.value = BaseResponse(obj)
        }
        return objLiveDataTmp
    }

    override fun updateInAsync(objList: List<E>) {
        getRepository().update(objList) {  }
    }
}