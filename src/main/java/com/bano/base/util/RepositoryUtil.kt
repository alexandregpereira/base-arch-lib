package com.bano.base.util

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import io.realm.Realm

/**
 * Created by bk_alexandre.pereira on 05/12/2017.
 *
 */
object RepositoryUtil {

    fun <X> calculatePerformance(performanceTag: String, callback: () -> X): X {
        val startTime = System.currentTimeMillis()
        val x = callback()
        val endTime = System.currentTimeMillis()
        Log.d(performanceTag, "performance: ${(endTime - startTime)}")
        return x
    }

    fun <T> executeInAsyncHandlerThread(execute: () -> T, callback: (T) -> Unit) {
        val handlerThread = HandlerThread("executeInAsyncHandlerThread")
        handlerThread.start()
        val mainHandler = Handler()
        Handler(handlerThread.looper).post {
            val t = execute()
            mainHandler.post {
                handlerThread.quit()
                callback(t)
            }
        }
    }

    fun <T> executeRealmInAsyncHandlerThread(execute: (realm: Realm) -> T, callback: (T) -> Unit) {
        val handlerThread = HandlerThread("executeRealmInAsyncHandlerThread")
        handlerThread.start()
        val mainHandler = Handler()
        Handler(handlerThread.looper).post {
            val realm = Realm.getDefaultInstance()
            val t = execute(realm)
            realm.close()
            mainHandler.post {
                handlerThread.quit()
                callback(t)
            }
        }
    }

    fun <T, R> executeRealmInAsyncHandlerThread(r: R, execute: (realm: Realm) -> T, callback: (R, T) -> Unit) {
        val handlerThread = HandlerThread("executeRealmInAsyncHandlerThread")
        handlerThread.start()
        val mainHandler = Handler()
        Handler(handlerThread.looper).post {
            val realm = Realm.getDefaultInstance()
            val t = execute(realm)
            realm.close()
            mainHandler.post {
                handlerThread.quit()
                callback(r, t)
            }
        }
    }
}