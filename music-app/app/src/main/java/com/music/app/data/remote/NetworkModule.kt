package com.music.app.data.remote

import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    // 公网地址，通过frp映射
    const val BASE_URL = "http://120.79.224.90:18080/"
//    const val BASE_URL = "http://127.0.0.1:8080/"
    val staticBaseUrl: String
        get() = BASE_URL + "static/"

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
            // Token 拦截器：自动将本地存储的 token 添加到请求头
            .addInterceptor { chain ->
                val ctx = appContext
                val token = if (ctx != null) TokenStore.getToken(ctx) else null
                val request = if (token != null) {
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
