package com.bano.base.contract

import io.realm.RealmList
import io.realm.RealmModel
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.annotations.PrimaryKey
import java.lang.reflect.Modifier

/**
 * Created by bk_alexandre.pereira on 16/03/2018.
 *
 */

fun <T : RealmModel, E> T.toObj(newInstance: (T) -> E): E {
    return newInstance(this)
}

fun Any.getId(): Any {
    val idField = this::javaClass.get().declaredFields.find { field ->
        field.annotations.any { it is PrimaryKey }
    } ?: throw RuntimeException("$this do not have primary key")

    return if(Modifier.isPublic(idField.modifiers)) {
        idField.get(this)
    }
    else {
        val methods = this::javaClass.get().declaredMethods
        methods.find { it.name?.toLowerCase()?.contains("get${idField.name.toLowerCase()}") == true }?.invoke(this)
    } ?: throw RuntimeException("Not possible get the $idField value")
}

fun <T : RealmModel> RealmQuery<T>.queryById(primaryKeyFieldName: String, id: Any): RealmQuery<T> {
    return when(id) {
        is Long -> this.equalTo(primaryKeyFieldName, id)
        is String -> this.equalTo(primaryKeyFieldName, id)
        is Int -> this.equalTo(primaryKeyFieldName, id)
        else -> throw IllegalArgumentException("$id is not supported")
    }
}

fun <T : RealmModel, R : Any> RealmQuery<T>.queryByIds(primaryKeyFieldName: String, idList: List<R>): RealmQuery<T>? {
    if(idList.isEmpty()) return null
    return when {
        idList[0] is Long -> {
            val ids = idList.map { it as Long }.toTypedArray()
            this.`in`(primaryKeyFieldName, ids)
        }
        idList[0] is String -> {
            val ids = idList.map { it as String }.toTypedArray()
            this.`in`(primaryKeyFieldName, ids)
        }
        idList[0] is Int -> {
            val ids = idList.map { it as Int }.toTypedArray()
            this.`in`(primaryKeyFieldName, ids)
        }
        else -> null
    }
}

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

fun <E, T> RealmResults<T>?.toArrayList(mapper: BaseObjMapperContract<E, T>): ArrayList<E> {
    val list = ArrayList<E>()
    this?.forEach { list.add(mapper.createObj(it)) }
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