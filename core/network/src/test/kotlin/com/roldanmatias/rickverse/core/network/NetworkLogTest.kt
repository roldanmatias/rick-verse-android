package com.roldanmatias.rickverse.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkLogTest {
    private fun event(id: Long) =
        NetworkEvent(
            id = id,
            timestampMillis = id,
            source = NetworkSource.Rest,
            label = "GET /x",
            status = 200,
            durationMs = 1,
            bytes = 1,
            servedFromCache = false,
        )

    @Test
    fun `newest event comes first`() {
        val log = NetworkLog()

        log.record(event(1))
        log.record(event(2))

        assertEquals(listOf(2L, 1L), log.events.value.map { it.id })
    }

    @Test
    fun `size is bounded and the oldest events are dropped`() {
        val log = NetworkLog(maxEvents = 3)

        (1L..5L).forEach { log.record(event(it)) }

        assertEquals(listOf(5L, 4L, 3L), log.events.value.map { it.id })
    }

    @Test
    fun `clear empties the log`() {
        val log = NetworkLog()
        log.record(event(1))

        log.clear()

        assertTrue(log.events.value.isEmpty())
    }

    @Test
    fun `ids increase`() {
        val log = NetworkLog()

        assertTrue(log.nextId() < log.nextId())
    }
}
