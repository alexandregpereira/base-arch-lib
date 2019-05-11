package com.bano.base.arch

import android.util.Log
import io.realm.Realm

/**
 * Created by bk_alexandre.pereira on 26/10/2017.
 *
 */
abstract class Repository(realm: Realm? = null) {

    private var mRealm: Realm? = realm
    private var mResetRealmManually = false

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
        if(mResetRealmManually) return
        Log.d("Repository", "${this.javaClass.simpleName} resetRealm()")
        mRealm?.close()
        mRealm = null
    }

    fun resetRealmOnlyManually(resetManually: Boolean) {
        mResetRealmManually = resetManually
    }
}