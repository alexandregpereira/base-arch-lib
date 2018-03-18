package com.bano.base.contract

import io.realm.RealmList

/**
 * Created by bk_alexandre.pereira on 16/03/2018.
 *
 */
fun <T> List<T>?.toRealmList(newInstance: (T) -> T): RealmList<T> {
    val realmList = RealmList<T>()
    this?.forEach { realmList.add(newInstance(it)) }
    return realmList
}

/**
 * For primitives types
 */
fun <T> List<T>?.toRealmList(): RealmList<T> {
    val realmList = RealmList<T>()
    this?.forEach { realmList.add(it) }
    return realmList
}

fun <T> RealmList<T>?.toArrayList(newInstance: (T) -> T): ArrayList<T> {
    val realmList = ArrayList<T>()
    this?.forEach { realmList.add(newInstance(it)) }
    return realmList
}

/**
 * For primitives types
 */
fun <T> RealmList<T>?.toArrayList(): ArrayList<T> {
    val realmList = ArrayList<T>()
    this?.forEach { realmList.add(it) }
    return realmList
}