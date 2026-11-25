package com.roldanmatias.rickverse.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

/** Which API produced a [NetworkEvent]. */
public enum class NetworkSource { Rest, GraphQl }

/**
 * One finished network call.
 *
 * @property label `GET /episode/1,2,3` for REST, the operation name (`CastPage`) for GraphQL.
 * @property status HTTP status, or `0` when the call failed before a response arrived.
 * @property bytes response body size, or `-1` when unknown.
 * @property servedFromCache `true` when the HTTP cache answered without touching the network.
 */
public data class NetworkEvent(
    val id: Long,
    val timestampMillis: Long,
    val source: NetworkSource,
    val label: String,
    val status: Int,
    val durationMs: Long,
    val bytes: Long,
    val servedFromCache: Boolean,
)

/**
 * Thread-safe, bounded, in-memory store of recent network calls, newest first.
 * One instance is shared by REST and GraphQL, so the in-app network log shows both APIs side by side.
 * Interceptors run on OkHttp threads, hence the lock-free `MutableStateFlow.update`.
 */
public class NetworkLog(private val maxEvents: Int = DEFAULT_MAX_EVENTS) {
    private val state = MutableStateFlow<List<NetworkEvent>>(emptyList())
    private val ids = AtomicLong()

    /** Newest event first. */
    public val events: StateFlow<List<NetworkEvent>> = state.asStateFlow()

    /** A unique, increasing id for a new event. */
    public fun nextId(): Long = ids.incrementAndGet()

    public fun record(event: NetworkEvent) {
        state.update { current -> (listOf(event) + current).take(maxEvents) }
    }

    public fun clear() {
        state.value = emptyList()
    }

    private companion object {
        const val DEFAULT_MAX_EVENTS = 200
    }
}
