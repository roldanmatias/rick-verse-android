package com.roldanmatias.rickverse.core.domain.model

/** Which backend API serves the data. Switchable at runtime from Settings. */
public enum class DataSource { Rest, GraphQl }

/**
 * How the GraphQL normalized cache is consulted. REST ignores it (OkHttp's HTTP cache is separate).
 */
public enum class CachePolicy {
    /** Use the cache if it has the data, otherwise go to the network. Best for rarely-changing data. */
    CacheFirst,

    /** Try the network, fall back to the cache on failure. */
    NetworkFirst,

    /** Always hit the network; never read the cache. For data that must be fresh (e.g. a balance). */
    NetworkOnly,

    /** Emit the cached value immediately, then the fresh network value (stale-while-revalidate). */
    CacheAndNetwork,
}
