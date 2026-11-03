package com.ibramusic.mobile.core.di

import android.content.Context
import com.ibramusic.mobile.core.common.Constants
import com.ibramusic.mobile.core.network.DeezerApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDir, 50L * 1024L * 1024L) // 50MB HTTP Disk Cache

        return OkHttpClient.Builder()
            .cache(cache)
            .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestWithHeaders = original.newBuilder()
                    .apply {
                        if (original.header("User-Agent") == null) {
                            header(
                                "User-Agent",
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
                            )
                        }
                    }
                    .build()
                chain.proceed(requestWithHeaders)
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideDeezerApiService(okHttpClient: OkHttpClient): DeezerApiService {
        return Retrofit.Builder()
            .baseUrl(Constants.DEEZER_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeezerApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSaavnApiService(okHttpClient: OkHttpClient): com.ibramusic.mobile.core.network.SaavnApiService {
        return Retrofit.Builder()
            .baseUrl(Constants.SAAVN_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.ibramusic.mobile.core.network.SaavnApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSupabaseApiService(okHttpClient: OkHttpClient): com.ibramusic.mobile.core.network.SupabaseApiService {
        return Retrofit.Builder()
            .baseUrl(Constants.SUPABASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.ibramusic.mobile.core.network.SupabaseApiService::class.java)
    }
}

