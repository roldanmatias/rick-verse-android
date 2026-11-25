package com.roldanmatias.rickverse.core.domain.error

/**
 * Everything that can go wrong from the point of view of the UI, independent of REST or GraphQL.
 *
 * There is intentionally **no** `Cancelled` case: cancellation is a `CancellationException` that must propagate,
 * never something to show. See [runCatchingDomain].
 *
 * User-facing copy is not defined here; the UI layer maps each case to a localized message.
 */
public sealed interface DomainError {
    /** No connectivity (DNS failure, connection refused). */
    public data object Offline : DomainError

    /** The request did not finish in time. */
    public data object Timeout : DomainError

    /** The requested entity does not exist. */
    public data object NotFound : DomainError

    /** The backend failed (HTTP 5xx or a GraphQL error without data). */
    public data class Server(
        val statusCode: Int? = null,
    ) : DomainError

    /** The response arrived but could not be understood (decoding or mapping failure). */
    public data class InvalidData(
        val detail: String? = null,
    ) : DomainError

    /** Anything else. Always log the cause where this is created. */
    public data class Unknown(
        val detail: String? = null,
    ) : DomainError
}
