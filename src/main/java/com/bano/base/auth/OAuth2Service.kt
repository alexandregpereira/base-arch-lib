package com.bano.base.auth


import android.os.Build
import android.util.Log
import com.bano.base.model.BaseApiRequestModel
import com.bano.base.util.Tls12SocketFactory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.KeyStore
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


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

        if (Build.VERSION.SDK_INT == 19) {
            enableTls12(httpClient)
        }

        return httpClient.build()
    }

    fun enableTls12(httpClient: OkHttpClient.Builder) {
        try {
            val sc = SSLContext.getInstance("TLSv1.2")
            sc.init(null, null, null)
            val trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(null as KeyStore?)
            val trustManagers = trustManagerFactory.trustManagers
            if (trustManagers.size != 1 || trustManagers[0] !is X509TrustManager) {
                throw IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers))
            }
            val trustManager = trustManagers[0] as X509TrustManager
            httpClient.sslSocketFactory(Tls12SocketFactory(sc.socketFactory), trustManager)

            val cs = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2)
                    .build()

            val specs = ArrayList<ConnectionSpec>()
            specs.add(cs)
            specs.add(ConnectionSpec.COMPATIBLE_TLS)
            specs.add(ConnectionSpec.CLEARTEXT)

            httpClient.connectionSpecs(specs)
        } catch (exc: Exception) {
            Log.e("OkHttpTLSCompat", "Error while setting TLS 1.2", exc)
        }
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
