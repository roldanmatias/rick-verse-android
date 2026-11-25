package com.roldanmatias.rickverse.data.rest.mapper

import com.roldanmatias.rickverse.core.domain.error.DomainError
import com.roldanmatias.rickverse.core.network.NetworkError
import com.roldanmatias.rickverse.core.network.toNetworkError

private const val HTTP_NOT_FOUND = 404
private const val HTTP_REQUEST_TIMEOUT = 408
private const val HTTP_TOO_MANY_REQUESTS = 429
private val SERVER_ERRORS = 500..599

/** The only place where network knowledge meets Domain. */
internal fun NetworkError.toDomainError(): DomainError =
    when (this) {
        NetworkError.Offline -> {
            DomainError.Offline
        }

        NetworkError.Timeout -> {
            DomainError.Timeout
        }

        is NetworkError.Decoding -> {
            DomainError.InvalidData(message)
        }

        is NetworkError.Transport -> {
            DomainError.Unknown(message)
        }

        is NetworkError.Http -> {
            when (status) {
                HTTP_NOT_FOUND -> DomainError.NotFound
                HTTP_REQUEST_TIMEOUT -> DomainError.Timeout
                HTTP_TOO_MANY_REQUESTS, in SERVER_ERRORS -> DomainError.Server(status)
                else -> DomainError.Unknown("HTTP $status")
            }
        }
    }

/** Exceptions raised inside this module (transport or mapping) to a [DomainError]. Rethrows cancellation. */
internal fun Throwable.toDomainError(): DomainError =
    when (this) {
        is MappingException -> DomainError.InvalidData(message)
        else -> toNetworkError().toDomainError()
    }
