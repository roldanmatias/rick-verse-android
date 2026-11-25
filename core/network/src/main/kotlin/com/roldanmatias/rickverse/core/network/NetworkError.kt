package com.roldanmatias.rickverse.core.network

/**
 * Transport-level failure. Knows nothing about the domain: each data module maps this to `DomainError` in
 * exactly one place (its `ErrorMapper`).
 */
public sealed interface NetworkError {
    /** No connectivity: DNS failure or connection refused. */
    public data object Offline : NetworkError

    /** Connect, read, write or whole-call timeout. */
    public data object Timeout : NetworkError

    /** The server answered with a non-2xx [status]. [body] is the (possibly empty) error body. */
    public data class Http(val status: Int, val body: String?) : NetworkError

    /** The response arrived but could not be decoded. */
    public data class Decoding(val message: String?) : NetworkError

    /** Any other I/O or protocol failure. */
    public data class Transport(val message: String?) : NetworkError
}
