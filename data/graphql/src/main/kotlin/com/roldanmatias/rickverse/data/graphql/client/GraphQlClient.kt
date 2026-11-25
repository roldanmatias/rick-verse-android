package com.roldanmatias.rickverse.data.graphql.client

import com.apollographql.apollo.api.Query

/**
 * The only Apollo surface repositories see. Returns the operation's data (possibly partial, see ADR 0006) or throws
 * a [GraphQlClientError]. Keeping this small and ours makes repositories testable with a one-line fake and keeps
 * `ApolloClient` out of every mapper and repository.
 */
internal interface GraphQlClient {
    suspend fun <D : Query.Data> query(query: Query<D>, policy: FetchPolicy): D
}

/** Our cache policy vocabulary, mapped to Apollo's in exactly one place ([ApolloGraphQlClient]). */
internal enum class FetchPolicy { CacheFirst, NetworkFirst, NetworkOnly, CacheAndNetwork }

/** Failures of a GraphQL call, independent of Apollo's exception hierarchy. */
internal sealed class GraphQlClientError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** HTTP 200 (or cached) but no `data`, only `errors`: GraphQL reports failures in the body. */
    class NoData(val messages: List<String>) : GraphQlClientError("No data: ${messages.joinToString()}")

    /** Non-2xx HTTP response. */
    class Http(val status: Int) : GraphQlClientError("HTTP $status")

    /** Could not reach the server (offline, DNS, timeout...). The [cause] says which. */
    class Network(cause: Throwable?) : GraphQlClientError("Network failure", cause)

    /** The response was not valid for the operation. */
    class Parse(cause: Throwable?) : GraphQlClientError("Could not parse response", cause)
}
