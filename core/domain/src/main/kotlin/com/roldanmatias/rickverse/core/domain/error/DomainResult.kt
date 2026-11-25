package com.roldanmatias.rickverse.core.domain.error

import kotlin.coroutines.cancellation.CancellationException

/**
 * Typed success-or-failure, the closest Kotlin gets to Swift's `throws(DomainError)`.
 * Callers must handle [Failure] with an exhaustive `when`, so failure cannot be forgotten like an exception can.
 */
public sealed interface DomainResult<out T> {
    /** The operation succeeded with [value]. */
    public data class Success<out T>(val value: T) : DomainResult<T>

    /** The operation failed with a domain-level [error]. */
    public data class Failure(val error: DomainError) : DomainResult<Nothing>
}

/** Transforms the success value, leaving a failure untouched. */
public inline fun <T, R> DomainResult<T>.map(transform: (T) -> R): DomainResult<R> =
    when (this) {
        is DomainResult.Success -> DomainResult.Success(transform(value))
        is DomainResult.Failure -> this
    }

/** Collapses both branches into one value. */
public inline fun <T, R> DomainResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (DomainError) -> R,
): R =
    when (this) {
        is DomainResult.Success -> onSuccess(value)
        is DomainResult.Failure -> onFailure(error)
    }

/** The value on success, `null` on failure. */
public fun <T> DomainResult<T>.getOrNull(): T? = (this as? DomainResult.Success)?.value

/**
 * Runs [block] and converts any thrown exception into [DomainResult.Failure] using [mapError].
 *
 * **[CancellationException] is always rethrown.** Swallowing it breaks structured concurrency: a cancelled
 * coroutine would keep running and could even publish a bogus error to the UI. This is the single place in the
 * code base that is allowed to catch `Throwable`.
 */
@Suppress("TooGenericExceptionCaught")
public suspend inline fun <T> runCatchingDomain(
    mapError: (Throwable) -> DomainError = { DomainError.Unknown(it.message) },
    block: () -> T,
): DomainResult<T> =
    try {
        DomainResult.Success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        DomainResult.Failure(mapError(throwable))
    }
