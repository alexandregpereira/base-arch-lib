package com.bano.base.contract

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.paging.PagedList
import com.bano.base.BaseResponse

interface BaseViewModelContract<T> {

    var total: Int?
    val listOnlyLiveData: LiveData<PagedList<T>?>
    val objOnlyLiveData: MutableLiveData<T>
    val listLiveData: MutableLiveData<BaseResponse<List<T>>>
    val objLiveData: MutableLiveData<BaseResponse<T>>

    fun loadObj(id: Long)

    fun load()

    fun loadLocal()

    fun loadObjLocal(id: Any)

    fun loadNextPage()

    fun resetPage()

    fun update(obj: T): LiveData<T>

    fun updateInAsync(objList: List<T>)
}