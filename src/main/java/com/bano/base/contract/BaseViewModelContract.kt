package com.bano.base.contract

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.bano.base.BaseResponse

interface BaseViewModelContract<T> {

    val listLiveData: MutableLiveData<BaseResponse<List<T>>>
    val objLiveData: MutableLiveData<BaseResponse<T>>

    fun loadObj(id: Long)

    fun loadObjLocal(id: Any)

    fun resetPage()

    fun update(obj: T): LiveData<T>

    fun updateInAsync(objList: List<T>)
}