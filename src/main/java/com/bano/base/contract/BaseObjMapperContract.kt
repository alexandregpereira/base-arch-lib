package com.bano.base.contract

import io.realm.RealmModel

/**
 * Created by bk_alexandre.pereira on 23/08/2017.
 *
 */
interface BaseObjMapperContract<E, T : RealmModel> {

    fun createObj(realmModel: T): E
    fun createRealmObj(obj: E): T
}