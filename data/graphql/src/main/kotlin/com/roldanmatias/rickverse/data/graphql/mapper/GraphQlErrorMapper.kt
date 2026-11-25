package com.roldanmatias.rickverse.data.graphql.mapper

import com.roldanmatias.rickverse.core.domain.error.DomainError
import com.roldanmatias.rickverse.core.network.NetworkError
import com.roldanmatias.rickverse.core.network.toNetworkError
import com.roldanmatias.rickverse.data.graphql.client.GraphQlClientError

private const val HTTP_NOT_FOUND = 404
private const val HTTP_REQUEST_TIMEOUT = 408
private const val HTTP_OK = 200

/** Raised by the repositories when a response is present but a required element is not. */
internal class InvalidDataException(message: String) : Exception(message)

/** `character(id:)` returned `null`: GraphQL's way of saying "no such item" (HTTP stays 200). */
internal object NotFoundException : Exception("Not found")

/** The only place GraphQL failures meet Domain. `toNetworkError` rethrows `CancellationException`. */
internal fun Throwable.toDomainError(): DomainError =
    when (this) {
        NotFoundException -> DomainError.NotFound

        is InvalidDataException -> DomainError.InvalidData(message.orEmpty())

        is GraphQlClientError.Parse -> DomainError.InvalidData(message.orEmpty())

        // data == null with errors: the server answered 200 but failed. Report it as a server error.
        is GraphQlClientError.NoData -> DomainError.Server(HTTP_OK)

        is GraphQlClientError.Http -> status.toDomainError()

        is GraphQlClientError.Network -> (cause ?: this).transportError()

        else -> transportError()
    }

private fun Int.toDomainError(): DomainError =
    when (this) {
        HTTP_NOT_FOUND -> DomainError.NotFound
        HTTP_REQUEST_TIMEOUT -> DomainError.Timeout
        else -> DomainError.Server(this)
    }

private fun Throwable.transportError(): DomainError =
    when (val network = toNetworkError()) {
        NetworkError.Offline -> DomainError.Offline
        NetworkError.Timeout -> DomainError.Timeout
        else -> DomainError.Unknown(network.toString())
    }
