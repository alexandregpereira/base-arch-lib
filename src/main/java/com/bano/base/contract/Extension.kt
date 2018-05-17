package com.bano.base.contract

import io.realm.RealmList
import io.realm.RealmResults

/**
 * Created by bk_alexandre.pereira on 16/03/2018.
 *
 */
fun <E, T> List<E>?.toRealmList(newInstance: (E) -> T): RealmList<T> {
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

fun <E, T> RealmList<T>?.toArrayList(newInstance: (T) -> E): ArrayList<E> {
    val realmList = ArrayList<E>()
    this?.forEach { realmList.add(newInstance(it)) }
    return realmList
}

fun <E, T> RealmResults<T>?.toArrayList(newInstance: (T) -> E): ArrayList<E> {
    val list = ArrayList<E>()
    this?.forEach { list.add(newInstance(it)) }
    return list
}

/**
 * For primitives types
 */
fun <T> RealmList<T>?.toArrayList(): ArrayList<T> {
    val realmList = ArrayList<T>()
    this?.forEach { realmList.add(it) }
    return realmList
}