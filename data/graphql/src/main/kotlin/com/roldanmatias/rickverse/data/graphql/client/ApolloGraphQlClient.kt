package com.roldanmatias.rickverse.data.graphql.client

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.exception.CacheMissException
import com.apollographql.apollo.exception.JsonDataException
import com.apollographql.apollo.exception.JsonEncodingException
import com.apollographql.cache.normalized.fetchPolicy
import com.roldanmatias.rickverse.core.network.interceptor.RetryInterceptor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject
import com.apollographql.cache.normalized.FetchPolicy as ApolloFetchPolicy

internal class ApolloGraphQlClient @Inject constructor(private val apollo: ApolloClient) : GraphQlClient {
    override suspend fun <D : Query.Data> query(query: Query<D>, policy: FetchPolicy): D {
        val response = apollo.query(query).retrySafe().fetchPolicy(policy.toApollo()).execute()
        // Partial data (data + errors) is accepted: the mappers skip what is missing. See ADR 0006.
        return response.data ?: throw response.toClientError()
    }

    override fun <D : Query.Data> queryUpdates(query: Query<D>): Flow<D> =
        apollo.query(query).retrySafe().fetchPolicy(ApolloFetchPolicy.CacheAndNetwork).toFlow().mapNotNull { response ->
            // The first emission of a cold cache is a miss: not a failure, the network answer follows.
            response.data ?: if (response.exception is CacheMissException) null else throw response.toClientError()
        }

    // GraphQL queries are POSTs but read-only: tell the shared RetryInterceptor it may retry them.
    private fun <D : Query.Data> com.apollographql.apollo.ApolloCall<D>.retrySafe() =
        addHttpHeader(RetryInterceptor.RETRY_SAFE_HEADER, "true")

    private fun <D : Operation.Data> ApolloResponse<D>.toClientError(): GraphQlClientError =
        when (val failure = exception) {
            null -> GraphQlClientError.NoData(errors.orEmpty().map { it.message })
            is ApolloHttpException -> GraphQlClientError.Http(failure.statusCode)
            is ApolloNetworkException -> GraphQlClientError.Network(failure.cause)
            is JsonDataException, is JsonEncodingException -> GraphQlClientError.Parse(failure)
            else -> GraphQlClientError.NoData(listOf(failure.message.orEmpty()))
        }

    // A one-shot call cannot emit twice, so CacheAndNetwork degrades to NetworkFirst here;
    // the two-emission form is [queryUpdates].
    private fun FetchPolicy.toApollo(): ApolloFetchPolicy =
        when (this) {
            FetchPolicy.CacheFirst -> ApolloFetchPolicy.CacheFirst
            FetchPolicy.NetworkFirst, FetchPolicy.CacheAndNetwork -> ApolloFetchPolicy.NetworkFirst
            FetchPolicy.NetworkOnly -> ApolloFetchPolicy.NetworkOnly
        }
}
