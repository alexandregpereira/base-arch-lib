package com.bano.base.arch.main.embedded

import com.bano.base.BaseResponse
import com.bano.base.arch.main.remote.BaseRemoteApiRepository
import com.bano.base.arch.main.remote.BaseRemoteRepository
import com.bano.base.arch.main.sync.BaseSyncRepository
import io.realm.Realm
import io.realm.RealmModel

/**
 * Created by bk_alexandre.pereira on 15/09/2017.
 *
 */
abstract class BaseEmbeddedListRemoteRepository<E : Any, T, X : Any, V> : BaseRemoteApiRepository<E, T, X, V>, BaseEmbeddedListContract<X>
        where T : RealmModel  {

    override var embeddedRepositoryList: List<BaseRemoteRepository<*, *, *>>? = null

    constructor(realmClass: Class<T>, clazz: Class<V>): super(realmClass, clazz)
    constructor(realm: Realm, clazz: Class<V>, builder: Builder<T>): super(realm, clazz, builder)
    constructor(clazz: Class<V>, builder: Builder<T>): super(clazz, builder)
    constructor(realm: Realm, realmClass: Class<T>, clazz: Class<V>): super(realm, realmClass, clazz)

    override fun getRemoteList(callback: (baseResponse: BaseResponse<List<E>>) -> Unit) {
        super.getRemoteList { responseCode ->
            callback(responseCode)
            val embeddedRepositoryList = createEmbeddedRepositoryList(getRealm(), null)
            embeddedRepositoryList.forEach { embeddedRepository ->
                if(embeddedRepository is BaseSyncRepository<*, *, *, *>) {
                    embeddedRepository.checkPendentSync()
                }
            }
        }
    }

    override fun getRemoteObj(id: Any, callback: (baseResponse: BaseResponse<E>) -> Unit) {
        super.getRemoteObj(id) { responseCode ->
            callback(responseCode)
            val embeddedRepositoryList = createEmbeddedRepositoryList(getRealm(), null)
            embeddedRepositoryList.forEach { embeddedRepository ->
                if(embeddedRepository is BaseSyncRepository<*, *, *, *>) {
                    embeddedRepository.checkPendentSync()
                }
            }
        }
    }

    override fun onBeforeInsertData(realm: Realm, apiObj: X) {
        super<BaseEmbeddedListContract>.onBeforeInsertData(realm, apiObj)
    }

    override fun onBeforeInsertData(realm: Realm, id: Long, apiObj: X) {
        super<BaseEmbeddedListContract>.onBeforeInsertData(realm, id, apiObj)
    }
}