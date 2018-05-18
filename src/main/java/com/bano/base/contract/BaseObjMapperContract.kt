package com.bano.base.contract

/**
 * Created by bk_alexandre.pereira on 23/08/2017.
 *
 */
interface BaseObjMapperContract<E, T> {

    fun createObj(realmModel: T): E
    fun createRealmObj(obj: E): T
}