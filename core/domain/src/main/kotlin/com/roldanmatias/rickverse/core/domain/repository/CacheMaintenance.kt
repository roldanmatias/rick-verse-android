package com.roldanmatias.rickverse.core.domain.repository

/** Cache housekeeping exposed to Settings without leaking which cache implementation exists. */
public interface CacheMaintenance {
    /** Drops every cached GraphQL record. The next request goes to the network. */
    public suspend fun clearCache()
}
