package com.bano.base.arch.main.implementation

import com.bano.base.arch.main.remote.BaseRemoteRepository
import io.realm.Realm
import io.realm.RealmObject

/**
 * Created by Alexandre on 04/03/2018.
 *
 */
abstract class BaseSingleRealmRepository<E : Any, T : RealmObject> : BaseRemoteRepository<E, T, E> {

    constructor(realmClass: Class<T>): super(realmClass)
    constructor(builder: Builder<T>): super(builder)
    constructor(realm: Realm, builder: Builder<T>): super(realm, builder)
    constructor(realm: Realm, realmClass: Class<T>) : super(realm, realmClass)
}