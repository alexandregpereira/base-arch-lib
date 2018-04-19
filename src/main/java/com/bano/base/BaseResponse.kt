package com.bano.base

import retrofit2.Response

/**
 * Created by bk_alexandre.pereira on 19/09/2017.
 *
 */
open class BaseResponse<K> {

    val responseCode: Int
    val value: K?
    val isRemoteCallback: Boolean
    var payload: Any? = null

    constructor(responseCode: Int, value: K?, isRemoteCallback: Boolean) {
        this.responseCode = responseCode
        this.value = value
        this.isRemoteCallback = isRemoteCallback
    }

    constructor(value: K?) {
        this.responseCode = 200
        this.value = value
        this.isRemoteCallback = false
    }

    constructor(value: K?, isRemoteCallback: Boolean) {
        this.responseCode = 200
        this.value = value
        this.isRemoteCallback = isRemoteCallback
    }

    constructor(responseCode: Int) {
        this.responseCode = responseCode
        this.value = null
        this.isRemoteCallback = true
    }

    constructor(response: Response<K>){
        this.responseCode = response.code()
        this.value = response.body()
        this.isRemoteCallback = true
    }

    fun isSuccessful(): Boolean = responseCode in 200..299

    companion object {
        const val CACHE_MODE_CODE = 3
        const val UNKNOWN_HOST = 4
        const val UNKNOWN_ERROR = 500
        const val TIMEOUT_ERROR = 6
        const val TOKEN_ERROR = 7

        const val HTTP_SUCCESS = 200
        const val HTTP_NOT_AUTHORIZED = 401
        const val HTTP_SESSION_EXPIRED = 403
        const val HTTP_SERVICE_UNAVAILABLE = 503
        private const val HTTP_AUTH_ERROR = 408
        const val HTTP_NOT_FOUND = 404
        const val HTTP_BAD_REQUEST = 400

        fun isError(responseCode: Int): Boolean = responseCode == HTTP_NOT_AUTHORIZED
                || responseCode == HTTP_SESSION_EXPIRED
                || responseCode == HTTP_SERVICE_UNAVAILABLE
                || responseCode == HTTP_AUTH_ERROR
                || responseCode == HTTP_NOT_FOUND
                || responseCode == UNKNOWN_HOST
                || responseCode >= UNKNOWN_ERROR
                || responseCode == TIMEOUT_ERROR
                || responseCode == TOKEN_ERROR

        fun isSuccessful(responseCode: Int): Boolean = responseCode in 200..299


        fun isErrorToChangeNavigation(responseCode: Int): Boolean =
                responseCode == HTTP_SESSION_EXPIRED
                        || responseCode == HTTP_NOT_AUTHORIZED
                        || responseCode == TOKEN_ERROR
    }
}