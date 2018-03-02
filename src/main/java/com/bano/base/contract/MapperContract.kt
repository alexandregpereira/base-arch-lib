package com.bano.base.contract

import io.realm.RealmModel

/**
 * Created by bk_alexandre.pereira on 23/08/2017.
 *
 */
interface MapperContract<E, T : RealmModel, in X> : BaseMapperContract<E, T> {

    fun createObjFromObjApi(obj: X): E
}