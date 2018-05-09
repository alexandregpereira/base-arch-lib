package com.bano.base.arch.second

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.paging.PagedList
import com.bano.base.BaseResponse
import com.bano.base.contract.BaseViewModelContract

abstract class RoomViewModel<E> : ViewModel(), BaseViewModelContract<E> {

    var idParent: Long? = null
    override var total: Int? = null
    private var mRepository: RoomRepository<E>? = null

    override val listOnlyLiveData: LiveData<PagedList<E>?> by lazy {
        getRepository().getLocalList()
    }
    override val objOnlyLiveData = MutableLiveData<E>()
    override val listLiveData = MutableLiveData<BaseResponse<List<E>>>()
    override val objLiveData = MutableLiveData<BaseResponse<E>>()

    protected abstract fun createRepository(idParent: Long?): RoomRepository<E>

    protected fun getRepository(): RoomRepository<E> {
        val repository =
                if(mRepository?.idParent != idParent) {
                    createRepository(idParent)
                }
                else mRepository ?: createRepository(idParent)
        repository.total = total
        mRepository = repository
        return repository
    }

    override fun loadObj(id: Long) {
        loadObjLocal(id)
    }

    override fun load() {
        loadLocal()
    }

    override fun loadLocal() {

    }

    override fun loadObjLocal(id: Any) {
        getRepository().getLocalObj(id).subscribe {
            objOnlyLiveData.postValue(it)
        }
    }

    override fun loadNextPage(){
        val repository = getRepository()
        repository.nextPage()
        load()
    }

    override fun resetPage() {
        getRepository().resetPage()
    }

    override fun update(obj: E): LiveData<E> {
        val objLiveDataTmp = MutableLiveData<E>()
//        getRepository().update(obj) {
//            objLiveDataTmp.value = obj
//            objLiveData.value = BaseResponse(obj)
//        }
        return objLiveDataTmp
    }

    override fun updateInAsync(objList: List<E>) {
//        getRepository().update(objList) {  }
    }
}