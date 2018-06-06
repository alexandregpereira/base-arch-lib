package com.bano.base.model

import android.util.Log
import com.bano.base.BaseResponse
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.InputStream

/**
 * Created by henrique.oliveira on 11/10/2017.
 *
 */

abstract class BaseApiRequestModel {
    var pinningCert: InputStream? = null
    var requestTimeout: Int? = null
    private var mHeaders: HashMap<String, String>? = null
    val url: String

    constructor(url: String) {
        this.url = url
    }

    constructor(url: String, requestTimeout: Int) {
        this.url = url
        this.requestTimeout = requestTimeout
    }

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

    fun checkRefreshToken(responseCode: Int,
                          newTentative: Boolean,
                          onTokenRefreshed: () -> Unit,
                          onError: (responseCodeError: Int) -> Unit) {
        if(BaseResponse.isErrorToChangeNavigation(responseCode) && !newTentative) {
            refreshToken { refreshResponseCode ->
                when (refreshResponseCode) {
                    BaseResponse.HTTP_SUCCESS -> {
                        Log.d("TokenRefresh", "Token refreshed, trying again")
                        //Try again
                        onTokenRefreshed()
                    }
                    BaseResponse.UNKNOWN_ERROR -> {
                        Log.e("TokenRefresh", "Token refresh failed")
                        onError(BaseResponse.TOKEN_ERROR)
                    }
                    else -> {
                        Log.e("TokenRefresh", "Token refresh failed")
                        onError(responseCode)
                    }
                }
            }
            return
        }
        onError(responseCode)
    }

    fun <R> consumeApiWithRefreshTokenHandle(apiCall: () -> Call<R>, secondTentative: Boolean, onResponse: (baseResponse: BaseResponse<R>) -> Unit, onFailure: (t: Throwable) -> Unit) {
        apiCall().enqueue(object : Callback<R> {
            override fun onFailure(call: Call<R>?, t: Throwable) {
                t.printStackTrace()
                onFailure(t)
            }

            override fun onResponse(call: Call<R>?, response: Response<R>?) {
                if(response == null) {
                    onResponse(BaseResponse(BaseResponse.UNKNOWN_ERROR))
                    return
                }
                if(!response.isSuccessful) {
                    checkRefreshToken(response.code(), secondTentative, onTokenRefreshed = {
                        consumeApiWithRefreshTokenHandle(apiCall, true, onResponse, onFailure)
                    }, onError = { responseCodeError ->
                        onResponse(BaseResponse(responseCodeError))
                    })
                    return
                }
                onResponse(BaseResponse(response))
            }
        })
    }

    open fun buildGson(): Gson {
        return GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ") // ISO 8601
                .create()
    }
}