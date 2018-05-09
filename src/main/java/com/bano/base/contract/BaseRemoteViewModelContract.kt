package com.bano.base.contract

import android.arch.lifecycle.MutableLiveData

interface BaseRemoteViewModelContract {

    val responseCodeLiveData: MutableLiveData<Int>
    val logoutLiveData: MutableLiveData<Boolean>
    val loadingLiveData: MutableLiveData<Boolean>
    val loadingPaginationLiveData: MutableLiveData<Boolean>

    fun logout(callback: () -> Unit)

    fun loadObj(id: Long)

    fun load()

    fun reload(): Int

    fun reloadObj(id: Long): Int

    fun loadRemote()

    fun loadObjRemote(id: Long)
}