package com.roldanmatias.rickverse.data.graphql.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.cache.normalized.apolloStore
import com.roldanmatias.rickverse.core.domain.repository.CacheMaintenance
import javax.inject.Inject

internal class ApolloCacheMaintenance @Inject constructor(private val apollo: ApolloClient) : CacheMaintenance {
    override suspend fun clearCache() {
        apollo.apolloStore.clearAll()
    }
}
