package com.bano.base.arch

import android.util.Log
import io.realm.Realm

/**
 * Created by bk_alexandre.pereira on 26/10/2017.
 *
 */
abstract class Repository {

    private var mRealm: Realm?

    constructor() {
        mRealm = Realm.getDefaultInstance()
    }

    constructor(realm: Realm) {
        mRealm = realm
    }

    fun getRealm(): Realm {
        var realm = mRealm ?: Realm.getDefaultInstance()
        try {
            if(realm.isClosed) {
                realm = Realm.getDefaultInstance()
            }
        } catch (ex: IllegalStateException) {
            Log.e("Repository: getRealm()", ex.message)
            realm = Realm.getDefaultInstance()
        }
        mRealm = realm
        return realm
    }

    fun resetRealm() {
        Log.d("Repository", "${this.javaClass.simpleName} resetRealm()")
        mRealm?.close()
        mRealm = null
    }
}