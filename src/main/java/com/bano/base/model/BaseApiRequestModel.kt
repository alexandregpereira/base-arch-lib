package com.bano.base.model

import java.io.InputStream

/**
 * Created by henrique.oliveira on 11/10/2017.
 *
 */

abstract class BaseApiRequestModel(val url: String) {
    var pinningCert: InputStream? = null
    var requestTimeout: Int? = null
    private var mHeaders: HashMap<String, String>? = null

    abstract fun createHeaders(): HashMap<String, String>?
    abstract protected fun refreshToken(callback: (response: Int, tokenField: String?, tokenValue: String?) -> Unit)

    fun getHeaders(): HashMap<String, String>? {
        if(mHeaders == null) mHeaders = createHeaders()
        return mHeaders
    }

    open fun refreshToken(callback: (response: Int) -> Unit) {
        refreshToken { response, tokenField, tokenValue ->
            if(tokenField != null && tokenValue != null) {
                if(mHeaders == null) mHeaders = HashMap()
                mHeaders?.put(tokenField, tokenValue)
                callback(response)
            }
            else callback(response)
        }
    }
}