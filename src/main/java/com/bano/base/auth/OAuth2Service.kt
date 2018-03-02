package com.bano.base.auth


import com.bano.base.model.BaseApiRequestModel
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Created by henrique.oliveira on 10/10/2017.
 *
 */

object OAuth2Service {

    private fun buildClient(apiRequestModel: BaseApiRequestModel): OkHttpClient {
        val httpClient = OkHttpClient.Builder()
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        httpClient.addInterceptor(interceptor)
        val headers = apiRequestModel.getHeaders()
        if (headers != null) {
            httpClient.addInterceptor { chain ->
                val original = chain.request()

                val requestBuilder = original.newBuilder()
                for ((key, value) in headers) {
                    requestBuilder.header(key, value)
                }

                chain.proceed(requestBuilder.build())
            }
        }
        val timeout = apiRequestModel.requestTimeout
        if(timeout != null) {
            httpClient.readTimeout(timeout.toLong(), TimeUnit.SECONDS)
            httpClient.writeTimeout(timeout.toLong(), TimeUnit.SECONDS)
            httpClient.connectTimeout(timeout.toLong(), TimeUnit.SECONDS)
        }
        return httpClient.build()
    }

    fun <T> buildRetrofitService(apiRequestModel: BaseApiRequestModel, clazz: Class<T>): T {
        val client = buildClient(apiRequestModel)
        val retrofit = Retrofit.Builder()
                //.addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(buildGson()))
                .client(client)
                .baseUrl(apiRequestModel.url)
                .build()

        return retrofit.create(clazz)
    }

    private fun buildGson(): Gson {
        return GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ") // ISO 8601
                .create()
    }
}
