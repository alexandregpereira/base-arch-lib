package com.bano.base.arch.main.implementation

import com.bano.base.arch.main.BaseRepository
import io.realm.Realm
import io.realm.RealmObject

/**
 * Created by Alexandre on 04/03/2018.
 *
 */
abstract class BaseSingleRepository<T : RealmObject> : BaseRepository<T, T, T> {

    constructor(realmClass: Class<T>): super(realmClass)
    constructor(builder: Builder<T>): super(builder)
    constructor(realm: Realm, builder: Builder<T>): super(realm, builder)
    constructor(realm: Realm, realmClass: Class<T>) : super(realm, realmClass)

    override fun createRealmObj(obj: T): T = createObj(obj)
    override fun createObjFromObjApi(obj: T): T = createObj(obj)
}