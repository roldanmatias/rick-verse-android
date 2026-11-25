package com.roldanmatias.rickverse.data.graphql.client

import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.apollographql.cache.normalized.cacheInfo
import com.roldanmatias.rickverse.core.network.NetworkEvent
import com.roldanmatias.rickverse.core.network.NetworkLog
import com.roldanmatias.rickverse.core.network.NetworkSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

/**
 * What OkHttp cannot see. A normalized-cache hit never reaches HTTP, so the shared `MetricsInterceptor` is silent
 * for it. This Apollo-level interceptor logs those hits (with the operation name) so the network log shows
 * "CastMemberDetail, cache" next to the real requests. Network calls are NOT logged here: the OkHttp layer already
 * did, and logging twice would double-count.
 */
internal class CacheEventInterceptor(
    private val log: NetworkLog,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : ApolloInterceptor {
    override fun <D : Operation.Data> intercept(
        request: ApolloRequest<D>,
        chain: ApolloInterceptorChain,
    ): Flow<ApolloResponse<D>> =
        chain.proceed(request).onEach { response ->
            val info = response.cacheInfo ?: return@onEach
            if (info.isCacheHit) {
                log.record(
                    NetworkEvent(
                        id = log.nextId(),
                        timestampMillis = currentTimeMillis(),
                        source = NetworkSource.GraphQl,
                        label = request.operation.name(),
                        status = 0,
                        durationMs = info.cacheEndMillis - info.cacheStartMillis,
                        bytes = 0,
                        servedFromCache = true,
                    ),
                )
            }
        }
}
