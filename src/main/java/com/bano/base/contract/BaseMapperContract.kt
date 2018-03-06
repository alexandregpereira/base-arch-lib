package com.bano.base.contract

import io.realm.RealmList
import io.realm.RealmModel

/**
 * Created by bk_alexandre.pereira on 23/08/2017.
 *
 */
interface BaseMapperContract<E, T : RealmModel> : BaseObjMapperContract<E, T>{

    fun map(realmList: List<T>?): List<E> {
        if(realmList == null) return ArrayList()
        val list = ArrayList<E>(realmList.size)
        realmList.forEach { list.add(createObj(it)) }
        return list
    }

    fun mapToRealm(obsList: List<E>?): RealmList<T> {
        val list = RealmList<T>()
        obsList?.forEach { list.add(createRealmObj(it)) }
        return list
    }

    companion object {
        fun <T> mapToRealmList(list: List<T>?, newInstance: (T) -> T): RealmList<T> {
            val realmList = RealmList<T>()
            list?.forEach { realmList.add(newInstance(it)) }
            return realmList
        }

        fun <T> mapToList(list: RealmList<T>?, newInstance: (T) -> T): ArrayList<T> {
            val realmList = ArrayList<T>()
            list?.forEach { realmList.add(newInstance(it)) }
            return realmList
        }
    }
}