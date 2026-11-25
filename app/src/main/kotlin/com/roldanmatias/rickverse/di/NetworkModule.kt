package com.roldanmatias.rickverse.di

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.roldanmatias.rickverse.core.common.DefaultDispatcherProvider
import com.roldanmatias.rickverse.core.common.DispatcherProvider
import com.roldanmatias.rickverse.core.network.NetworkConfig
import com.roldanmatias.rickverse.core.network.NetworkJson
import com.roldanmatias.rickverse.core.network.NetworkLog
import com.roldanmatias.rickverse.core.network.OkHttpClientFactory
import com.roldanmatias.rickverse.core.network.interceptor.DefaultHeadersInterceptor
import com.roldanmatias.rickverse.core.network.interceptor.HttpLogger
import com.roldanmatias.rickverse.core.network.interceptor.LoggingInterceptor
import com.roldanmatias.rickverse.core.network.interceptor.MetricsInterceptor
import com.roldanmatias.rickverse.core.network.interceptor.RetryInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient
import javax.inject.Singleton

private const val HTTP_CACHE_BYTES = 20L * 1024 * 1024
private const val LOG_TAG = "RickVerse/Http"

/**
 * The shared networking graph. One `OkHttpClient` serves REST (and, from Phase 6, Apollo), so the same interceptors
 * and the same [NetworkLog] cover both APIs. Core modules never touch `Context`; this module supplies the cache dir.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    @Provides
    @Singleton
    fun json(): Json = NetworkJson.default

    @Provides
    @Singleton
    fun networkLog(): NetworkLog = NetworkLog()

    @Provides
    @Singleton
    fun dispatchers(): DispatcherProvider = DefaultDispatcherProvider

    @Provides
    @Singleton
    fun okHttpClient(
        @ApplicationContext context: Context,
        networkLog: NetworkLog,
    ): OkHttpClient {
        val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        // Release builds log nothing: no URLs with user input, no tokens.
        val logger = if (debuggable) HttpLogger { Log.d(LOG_TAG, it) } else HttpLogger.None
        return OkHttpClientFactory.create(
            config = NetworkConfig(cache = Cache(context.cacheDir.resolve("http"), HTTP_CACHE_BYTES)),
            // Order matters: metrics wrap retry, so one event covers the whole logical call including retries.
            applicationInterceptors =
            listOf(
                DefaultHeadersInterceptor(),
                MetricsInterceptor(networkLog),
                RetryInterceptor(),
            ),
            networkInterceptors = listOf(LoggingInterceptor(logger)),
        )
    }
}
