package com.bano.base.arch.main.embedded

import com.bano.base.arch.main.remote.BaseRemoteRepository
import io.realm.Realm
import io.realm.RealmModel

/**
 * Created by bk_alexandre.pereira on 15/09/2017.
 *
 */
abstract class BaseEmbeddedListRepository<E : Any, T, X : Any> : BaseRemoteRepository<E, T, X>, BaseEmbeddedListContract<X>
        where T : RealmModel  {

    override var embeddedRepositoryList: List<BaseRemoteRepository<*, *, *>>? = null

    constructor(realmClass: Class<T>): super(realmClass)
    constructor(realm: Realm, builder: Builder<T>): super(realm, builder)
    constructor(builder: Builder<T>): super(builder)
    constructor(realm: Realm, realmClass: Class<T>): super(realm, realmClass)

    fun getDefaultEmbeddedRepositoryList(): List<BaseRemoteRepository<out Any, out RealmModel, out Any>> {
        val baseRepositoryList = createEmbeddedRepositoryList(getRealm(), null)
        embeddedRepositoryList = baseRepositoryList
        return baseRepositoryList
    }

    override fun onBeforeInsertData(realm: Realm, apiObj: X) {
        super<BaseEmbeddedListContract>.onBeforeInsertData(realm, apiObj)
    }

    override fun onBeforeInsertData(realm: Realm, id: Any, apiObj: X) {
        super<BaseEmbeddedListContract>.onBeforeInsertData(realm, id, apiObj)
    }
}