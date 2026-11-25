package com.roldanmatias.rickverse.core.ui

import com.roldanmatias.rickverse.core.domain.error.DomainError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DomainErrorMessagesTest {
    @Test
    fun `every error maps to a message`() {
        val errors =
            listOf(
                DomainError.Offline,
                DomainError.Timeout,
                DomainError.NotFound,
                DomainError.Server(500),
                DomainError.InvalidData("x"),
                DomainError.Unknown("x"),
            )

        assertEquals(errors.size, errors.map { it.messageRes() }.toSet().size)
    }

    @Test
    fun `payload does not change the message`() {
        assertEquals(DomainError.Server(500).messageRes(), DomainError.Server(503).messageRes())
        assertNotEquals(DomainError.Offline.messageRes(), DomainError.Timeout.messageRes())
    }
}
