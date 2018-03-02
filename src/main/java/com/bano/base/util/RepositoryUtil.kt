package com.bano.base.util

import android.util.Log

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
}