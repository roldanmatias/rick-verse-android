package com.roldanmatias.rickverse.core.network

import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

/**
 * The one place that maps a [Throwable] to a [NetworkError].
 *
 * **[CancellationException] is rethrown, never mapped**: a cancelled request is not an error to show.
 * Callers invoke this from a `catch` block, so rethrowing here keeps cancellation intact.
 */
public fun Throwable.toNetworkError(): NetworkError =
    when (this) {
        is CancellationException -> {
            throw this
        }

        is UnknownHostException, is ConnectException -> {
            NetworkError.Offline
        }

        // SocketTimeoutException is an InterruptedIOException, as is OkHttp's whole-call timeout.
        is InterruptedIOException -> {
            NetworkError.Timeout
        }

        is HttpException -> {
            NetworkError.Http(
                status = code(),
                body =
                runCatching {
                    response()?.errorBody()?.string()
                }.getOrNull(),
            )
        }

        is SerializationException -> {
            NetworkError.Decoding(message)
        }

        is IOException -> {
            NetworkError.Transport(message)
        }

        else -> {
            NetworkError.Transport(message ?: this::class.simpleName)
        }
    }
