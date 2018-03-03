package com.bano.base.arch.main.embedded

import com.bano.base.BaseResponse
import com.bano.base.arch.main.BaseRepository
import com.bano.base.arch.main.remote.BaseRemoteRepository
import com.bano.base.arch.main.sync.BaseSyncRepository
import io.realm.Realm
import io.realm.RealmModel

/**
 * Created by bk_alexandre.pereira on 15/09/2017.
 *
 */
abstract class BaseEmbeddedListRepository<E, T, X, Z, Y, W, V> : BaseRemoteRepository<E, T, X, V>
        where T : RealmModel, Y : RealmModel  {

    var embeddedRepository: BaseRepository<Z, Y, W>? = null

    constructor(clazz: Class<V>): super(clazz)

    constructor(realm: Realm, clazz: Class<V>, builder: Builder): super(realm, clazz, builder)

    constructor(realm: Realm, clazz: Class<V>): super(realm, clazz)

    protected abstract fun getEmbeddedListFromApi(apiData: X): List<W>?
    protected abstract fun createEmbeddedRepository(realm: Realm, idParent: Long?): BaseRepository<Z, Y, W>
    protected abstract fun getId(apiData: X): Long

    override fun getRemoteList(callback: (baseResponse: BaseResponse<List<E>>) -> Unit) {
        super.getRemoteList { responseCode ->
            callback(responseCode)
            val embeddedRepository = createEmbeddedRepository(getRealm(), null)
            if(embeddedRepository is BaseSyncRepository<*, *, *, *>) {
                embeddedRepository.checkPendentSync()
            }
        }
    }

    override fun getRemoteObj(id: Long, callback: (baseResponse: BaseResponse<E>) -> Unit) {
        super.getRemoteObj(id) { responseCode ->
            callback(responseCode)
            val embeddedRepository = createEmbeddedRepository(getRealm(), null)
            if(embeddedRepository is BaseSyncRepository<*, *, *, *>) {
                embeddedRepository.checkPendentSync()
            }
        }
    }

    fun getEmbeddedRepository(realm: Realm, idParent: Long): BaseRepository<Z, Y, W> {
        if(embeddedRepository?.idParent != idParent) {
            embeddedRepository = null
        }
        val baseRepository = embeddedRepository ?: createEmbeddedRepository(realm, idParent)
        embeddedRepository = baseRepository
        return baseRepository
    }

    fun getDefaultEmbeddedRepository(): BaseRepository<Z, Y, W> {
        val baseRepository = createEmbeddedRepository(getRealm(), null)
        embeddedRepository = baseRepository
        return baseRepository
    }

    override fun onBeforeInsertData(realm: Realm, apiObj: X) {
        val embeddedListFromApi = getEmbeddedListFromApi(apiObj) ?: return
        val embeddedRepository = getEmbeddedRepository(realm, getId(apiObj))
        embeddedRepository.resumeMode = true
        embeddedRepository.insertOrUpdateList(embeddedRepository.offset, realm, embeddedListFromApi)
    }

    override fun onBeforeInsertData(realm: Realm, id: Long, apiObj: X) {
        val objApiList = getEmbeddedListFromApi(apiObj) ?: return
        val embeddedRepository = getEmbeddedRepository(realm, id)
        embeddedRepository.resumeMode = false
        embeddedRepository.insertOrUpdateList(embeddedRepository.offset, realm, objApiList)
    }
}