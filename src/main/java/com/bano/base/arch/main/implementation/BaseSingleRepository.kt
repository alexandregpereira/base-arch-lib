package com.bano.base.arch.main.implementation

import com.bano.base.arch.main.BaseRepository
import io.realm.Realm
import io.realm.RealmObject

/**
 * Created by Alexandre on 04/03/2018.
 *
 */
abstract class BaseSingleRepository<T : RealmObject> : BaseRepository<T, T, T> {

    constructor(): super()
    constructor(builder: Builder): super(builder)
    constructor(realm: Realm, builder: Builder): super(realm, builder)
    constructor(realm: Realm) : super(realm)

    override fun createRealmObj(obj: T): T = createObj(obj)
    override fun createObjFromObjApi(obj: T): T = createObj(obj)
}