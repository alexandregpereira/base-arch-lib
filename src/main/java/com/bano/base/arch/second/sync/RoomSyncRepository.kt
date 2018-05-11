package com.bano.base.arch.second.sync

import android.support.annotation.WorkerThread
import android.util.Log
import com.bano.base.BaseResponse
import com.bano.base.arch.main.sync.Syncable
import com.bano.base.arch.second.remote.RoomRemoteRepository
import io.reactivex.Flowable

/**
 * Created by bk_alexandre.pereira on 06/10/2017.
 *
 */
abstract class RoomSyncRepository<E, X : Any, V> : RoomRemoteRepository<E, X, V>
        where E : Syncable {

    constructor(clazz: Class<V>): super(clazz)

    constructor(clazz: Class<V>, builder: Builder<E>): super(clazz, builder)

    protected abstract fun sendPendentSync(api: V, syncList: List<E>, callback: (Boolean) -> Unit)
    protected abstract fun sendPendentSync(api: V, syncObj: E, callback: (Boolean) -> Unit)
    abstract fun getPendentSync(): Flowable<List<E>>

    override fun update(e: E, callback: () -> Unit) {
        super.update(e) {
            if(e.isPendentSyncStatus()) {
                Log.d(getTagLog(), "sendPendentSync Obj")
                val api = getApi()
                sendPendentSync(api, e) { result ->
                    if(result) {
                        e.syncStatus = Syncable.SYNC_REALIZED_STATUS
                        super.update(e) {}
                    }
                }
            }
            callback()
        }
    }

    override fun insertOrUpdate(e: E, callback: (e: E) -> Unit) {
        super.insertOrUpdate(e) {
            if(it.isPendentSyncStatus()) {
                Log.d(getTagLog(), "sendPendentSync Obj")
                val api = getApi()
                sendPendentSync(api, it) { result ->
                    if(result) {
                        it.syncStatus = Syncable.SYNC_REALIZED_STATUS
                        super.update(it) {}
                    }
                }
            }
            callback(it)
        }
    }

    override fun getRemoteList(callback: (baseResponse: BaseResponse<List<E>>) -> Unit) {
        super.getRemoteList { responseCode ->
            callback(responseCode)
            checkPendentSync()
        }
    }

    override fun getRemoteObj(id: Long, callback: (baseResponse: BaseResponse<E>) -> Unit) {
        super.getRemoteObj(id) { responseCode ->
            callback(responseCode)
            checkPendentSync()
        }
    }

    fun checkPendentSync() {
        getPendentSync()
                .subscribe { syncList ->
                    if(!syncList.isEmpty()) {
                        Log.d(getTagLog(), "sendPendentSync List")
                        val api = getApi()
                        sendPendentSync(api, syncList) { result ->
                            if(result) {
                                updateToSyncRealized(syncList)
                            }
                        }
                    }
                }
    }

    @WorkerThread
    open fun updateToSyncRealized(syncList: List<E>) {
        syncList.forEach {
            it.syncStatus = Syncable.SYNC_REALIZED_STATUS
        }
        insertOrUpdate(syncList)
    }
}