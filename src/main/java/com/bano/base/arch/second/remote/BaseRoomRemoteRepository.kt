package com.bano.base.arch.second.remote

import android.arch.persistence.room.RoomDatabase
import android.support.annotation.WorkerThread
import android.util.Log
import com.bano.base.annotation.PrimaryKey
import com.bano.base.arch.second.RoomRepository
import com.bano.base.contract.BaseContract
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.lang.reflect.Modifier
import java.util.*

abstract class BaseRoomRemoteRepository<E, X : Any> : RoomRepository<E> {
    private val tag = "BaseRepository"

    /**
     * set this flag when is necessary to specify different scenarios in setFieldFromApi method
     */
    var resumeMode: Boolean = false

    constructor(): super()

    constructor(builder: RoomRepository.Builder<E>): super(builder)

    abstract fun getLocalList(offset: Int, limit: Int): List<E>
    abstract fun delete(e: E)
    abstract fun getRoomDataBase(): RoomDatabase

    protected open fun isSameObj(obj: E, apiObj: X): Boolean = obj == apiObj
    abstract fun createObjFromObjApi(apiObj: X): E

    /**
     * This method is accessed when exists an obj local.
     * Must be implemented when the api obj fields is different from obj local
     *
     * @param oldObjLocal The obj local before the database update
     * @param objUpdated The obj updated with the apiObj fields
     */
    protected open fun setFieldsFromApi(oldObjLocal: E, objUpdated: E) = Unit

    /**
     * This method is accessed when exists an obj local and resumeMode is enabled.
     * Must be implemented when the api obj fields comes with less fields
     *
     * @param objLocal The obj local that must be updated
     * @param apiObj The obj that comes from API with less fields
     */
    protected open fun setFieldsFromApiInResumeMode(objLocal: E, apiObj: X) = Unit

    /**
     * Check if some data was deleted comparing the localList with apiList.
     * If in the apiList does not contains some data of localList, the local obj field excludeDate is updated
     * @param apiList The list that comes from the API
     */
    protected open fun handleDeletedDataFromApi(localList: List<E>, apiList: List<X>) {
        localList.forEach { localObj ->
            if(localObj !is BaseContract) return@forEach
            val apiObj = apiList.find { isSameObj(localObj, it) }
            if (apiObj == null) {
                Log.d(tag, "$localObj excludeDate updated")
                localObj.excludeDate = Date().time
                delete(localObj)
            }
        }
    }

    @WorkerThread
    fun insertApiList(offset: Int, apiList: List<X>): List<E> {
        val localList = getLocalList(offset, limit)
        val apiListFiltered = apiList.filter { it != null }

        handleDeletedDataFromApi(localList, apiListFiltered)
        Log.d(tag, getTagLog() + ": insertOrUpdateList()")
        var order = offset
        val objToSaveList = apiListFiltered.map { apiObj ->
            val objLocal = localList.find { isSameObj(it, apiObj) }
            if (objLocal != null) {
                val objToUpdate: E = if (resumeMode) {
                    setFieldsFromApiInResumeMode(objLocal, apiObj)
                    objLocal
                } else {
                    val objLocalUpdated = createObjFromObjApi(apiObj)
                    setFieldsFromApi(objLocal, objLocalUpdated)
                    objLocalUpdated
                }
                if(objToUpdate is BaseContract) objToUpdate.order = order++
                objToUpdate
            } else {
                val objToInsert = createObjFromObjApi(apiObj)
                if(objToInsert is BaseContract) objToInsert.order = order++
                objToInsert
            }
        }
        insertOrUpdateListFromApi(apiList, objToSaveList)
        return objToSaveList
    }

    open fun insertOrUpdateListFromApi(apiList: List<X>, objToSaveList: List<E>) {
        insertOrUpdate(objToSaveList)
    }

    protected fun insertOrUpdateFromObjApi(id: Long, objApiTmp: X): Flowable<E?> {
        return Flowable.just(objApiTmp)
                .subscribeOn(Schedulers.io())
                .map { objApi ->
                    val idField = objApi::javaClass.get().declaredFields.find { field ->
                        field.annotations.any { it is PrimaryKey }
                    }

                    if(idField != null) {
                        val idFieldValue = if(Modifier.isPublic(idField.modifiers)) {
                            idField.get(objApi)
                        }
                        else {
                            val methods = objApi::javaClass.get().declaredMethods
                            methods.find { it.name?.toLowerCase()?.contains("get${idField.name.toLowerCase()}") == true }?.invoke(objApi)
                        }

                        if(idFieldValue != null) getLocalObj(idFieldValue)
                        else null
                    } else {
                        Log.e(getTagLog(), "$objApi don't have PrimaryKey, the order will be missed")
                        null
                    }
                }
                .map { objLocal ->
                    val objToUpdate = createObjFromObjApi(objApiTmp)
                    if(objLocal != null) setFieldsFromApi(objLocal, objToUpdate)
                    if(objLocal != null && objToUpdate is BaseContract && objLocal is BaseContract) {
                        objToUpdate.order = objLocal.order
                    }
                    insertOrUpdateObjFromApi(objApiTmp, objToUpdate)
                    getInsertedObj(id, objApiTmp)
                }
                .observeOn(AndroidSchedulers.mainThread())
    }

    open fun insertOrUpdateObjFromApi(objApi: X, objToSave: E) {
        insertOrUpdate(objToSave)
    }

    protected open fun getInsertedObj(id: Long, objApi: X): E? = getLocalObj(id)
}