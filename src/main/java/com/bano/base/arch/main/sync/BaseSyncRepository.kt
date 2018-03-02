package com.bano.base.arch.main.sync

import android.util.Log
import com.bano.base.BaseResponse
import com.bano.base.arch.main.remote.BaseRemoteRepository
import com.bano.base.contract.BaseContract
import io.realm.Realm
import io.realm.RealmModel

/**
 * Created by bk_alexandre.pereira on 06/10/2017.
 *
 */
abstract class BaseSyncRepository<E, T, X, V> : BaseRemoteRepository<E, T, X, V>
        where E : BaseContract, E : Syncable, T : RealmModel, T : Syncable, T : BaseContract {

    constructor(clazz: Class<V>): super(clazz)

    constructor(limit: Int, clazz: Class<V>): super(limit, clazz)

    constructor(realm: Realm, limit: Int, clazz: Class<V>): super(realm, limit, clazz)

    constructor(realm: Realm, clazz: Class<V>): super(realm, clazz)

    constructor(limit: Int, idParent: Long?, clazz: Class<V>): super(limit, idParent, clazz)

    constructor(idParent: Long?, clazz: Class<V>): super(idParent, clazz)

    constructor(realm: Realm, limit:Int, idParent: Long?, clazz: Class<V>): super(realm, limit, idParent, clazz)

    constructor(realm: Realm, idParent: Long?, clazz: Class<V>): super(realm, idParent, clazz)

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

    override fun getRemoteObj(id: Long, callback: (baseResponse: BaseResponse<E>) -> Unit) {
        super.getRemoteObj(id) { responseCode ->
            callback(responseCode)
            checkPendentSync()
        }
    }

    fun checkPendentSync() {
        val syncList = map(getRealmQueryTable().equalTo(Syncable.SYNC_STATUS_FIELD_NAME, Syncable.SYNC_PENDENT_STATUS).findAll())
        if(!syncList.isEmpty()) {
            Log.d(getTagLog(), "sendPendentSync List")
            val api = getApi()
            sendPendentSync(api, syncList) { result ->
                if(result) {
                    syncList.forEach {
                        it.syncStatus = Syncable.SYNC_REALIZED_STATUS
                    }
                    update(syncList) {}
                }
            }
        }
    }
}