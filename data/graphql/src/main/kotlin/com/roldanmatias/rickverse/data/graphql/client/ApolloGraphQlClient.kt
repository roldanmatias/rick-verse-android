package com.roldanmatias.rickverse.data.graphql.client

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.exception.JsonDataException
import com.apollographql.apollo.exception.JsonEncodingException
import com.apollographql.cache.normalized.fetchPolicy
import javax.inject.Inject
import com.apollographql.cache.normalized.FetchPolicy as ApolloFetchPolicy

internal class ApolloGraphQlClient @Inject constructor(private val apollo: ApolloClient) : GraphQlClient {
    override suspend fun <D : Query.Data> query(query: Query<D>, policy: FetchPolicy): D {
        val response = apollo.query(query).fetchPolicy(policy.toApollo()).execute()
        // Partial data (data + errors) is accepted: the mappers skip what is missing. See ADR 0006.
        response.data?.let { return it }
        throw when (val failure = response.exception) {
            null -> GraphQlClientError.NoData(response.errors.orEmpty().map { it.message })
            is ApolloHttpException -> GraphQlClientError.Http(failure.statusCode)
            is ApolloNetworkException -> GraphQlClientError.Network(failure.cause)
            is JsonDataException, is JsonEncodingException -> GraphQlClientError.Parse(failure)
            else -> GraphQlClientError.NoData(listOf(failure.message.orEmpty()))
        }
    }

    // A one-shot call cannot emit twice, so CacheAndNetwork degrades to NetworkFirst here.
    // The two-emission form is exposed as a Flow in Phase 8.
    private fun FetchPolicy.toApollo(): ApolloFetchPolicy =
        when (this) {
            FetchPolicy.CacheFirst -> ApolloFetchPolicy.CacheFirst
            FetchPolicy.NetworkFirst, FetchPolicy.CacheAndNetwork -> ApolloFetchPolicy.NetworkFirst
            FetchPolicy.NetworkOnly -> ApolloFetchPolicy.NetworkOnly
        }
}
