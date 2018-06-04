package com.bano.base.arch.main.sync

import android.util.Log
import com.bano.base.BaseResponse
import com.bano.base.arch.main.remote.BaseRemoteApiRepository
import com.bano.base.contract.toList
import com.bano.base.util.RepositoryUtil
import io.realm.Realm
import io.realm.RealmModel
import io.realm.RealmQuery

/**
 * Created by bk_alexandre.pereira on 06/10/2017.
 *
 */
abstract class BaseSyncRepository<E, T, X : Any, V> : BaseRemoteApiRepository<E, T, X, V>
        where E : Syncable, T : RealmModel {

    constructor(clazz: Class<V>, builder: Builder<T>): super(clazz, builder)

    constructor(realm: Realm, clazz: Class<V>, builder: Builder<T>): super(realm, clazz, builder)

    protected abstract fun sendPendentSync(api: V, syncList: List<E>, callback: (Boolean) -> Unit)
    protected abstract fun sendPendentSync(api: V, syncObj: E, callback: (Boolean) -> Unit)

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

    override fun getRemoteObj(id: Any, callback: (baseResponse: BaseResponse<E>) -> Unit) {
        super.getRemoteObj(id) { responseCode ->
            callback(responseCode)
            checkPendentSync()
        }
    }

    fun checkPendentSync() {
        RepositoryUtil.executeRealmInAsyncHandlerThread(execute = { realm ->
            getPendentSyncQuery(getRealmQueryTable(realm)).findAll().toList(this)
        }, callback = { syncList ->
            if(!syncList.isEmpty()) {
                Log.d(getTagLog(), "sendPendentSync List")
                val api = getApi()
                sendPendentSync(api, syncList) { result ->
                    if(result) {
                        updateToSyncRealized(syncList)
                    }
                }
            }
        })
    }

    open fun updateToSyncRealized(syncList: List<E>) {
        syncList.forEach {
            it.syncStatus = Syncable.SYNC_REALIZED_STATUS
        }
        update(syncList) {}
    }

    open fun getPendentSyncQuery(realmQuery: RealmQuery<T>): RealmQuery<T> {
        return realmQuery.equalTo(Syncable.SYNC_STATUS_FIELD_NAME, Syncable.SYNC_PENDENT_STATUS)
    }
}