package com.music.app.data.remote

import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.concurrent.TimeUnit

object NetworkModule {
    // 公网地址，通过frp映射
    const val BASE_URL_2 = "http://120.79.224.90:18080/"
    const val BASE_URL = "http://103.215.83.202:39080/"
//    const val BASE_URL = "http://127.0.0.1:8080/"
    val currentBaseUrl: String
        get() {
            val ctx = appContext
            return if (ctx != null) ServerEndpointStore.getSelectedBaseUrl(ctx) else BASE_URL
        }

    val staticBaseUrl: String
        get() = currentBaseUrl + "static/"

    // 全局 Context，用于 Token 气泡、读取本地存储
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC // 减少日志输出
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            // 服务器切换：根据本地缓存重写请求的 scheme/host/port
            .addInterceptor { chain ->
                val ctx = appContext
                val selectedBase = if (ctx != null) ServerEndpointStore.getSelectedBaseUrl(ctx) else BASE_URL
                val selectedHttpUrl = selectedBase.toHttpUrlOrNull()
                val original = chain.request()
                val originalUrl = original.url

                val newUrl = if (selectedHttpUrl != null) {
                    originalUrl.newBuilder()
                        .scheme(selectedHttpUrl.scheme)
                        .host(selectedHttpUrl.host)
                        .port(selectedHttpUrl.port)
                        .build()
                } else {
                    originalUrl
                }

                chain.proceed(
                    if (newUrl == originalUrl) original else original.newBuilder().url(newUrl).build()
                )
            }
            // Token 拦截器：自动将本地存储的 token 添加到请求头
            .addInterceptor { chain ->
                val ctx = appContext
                val token = if (ctx != null && TokenStore.isLoggedInWithin15Days(ctx)) TokenStore.getToken(ctx) else null
                val request = if (!token.isNullOrBlank()) {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(okhttp3.ConnectionPool(5, 30, TimeUnit.MINUTES))
            .build()
    }

    val musicApiService: MusicApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MusicApiService::class.java)
    }
}
