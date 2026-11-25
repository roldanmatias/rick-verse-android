package com.roldanmatias.rickverse.data.rest.mapper

import com.roldanmatias.rickverse.core.domain.error.DomainError
import com.roldanmatias.rickverse.core.network.NetworkError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

@RunWith(Parameterized::class)
class ErrorMapperTest(
    private val input: NetworkError,
    private val expected: DomainError,
) {
    @Test
    fun `maps to the domain error`() {
        assertEquals(expected, input.toDomainError())
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} -> {1}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf(NetworkError.Offline, DomainError.Offline),
                arrayOf(NetworkError.Timeout, DomainError.Timeout),
                arrayOf(NetworkError.Http(404, null), DomainError.NotFound),
                arrayOf(NetworkError.Http(408, null), DomainError.Timeout),
                arrayOf(NetworkError.Http(429, null), DomainError.Server(429)),
                arrayOf(NetworkError.Http(500, null), DomainError.Server(500)),
                arrayOf(NetworkError.Http(503, null), DomainError.Server(503)),
                arrayOf(NetworkError.Http(400, null), DomainError.Unknown("HTTP 400")),
                arrayOf(NetworkError.Decoding("bad json"), DomainError.InvalidData("bad json")),
                arrayOf(NetworkError.Transport("reset"), DomainError.Unknown("reset")),
            )
    }
}

class ThrowableErrorMapperTest {
    @Test
    fun `mapping failure becomes InvalidData`() {
        assertEquals(DomainError.InvalidData("nope"), MappingException("nope").toDomainError())
    }

    @Test
    fun `unknown host becomes Offline`() {
        assertEquals(DomainError.Offline, UnknownHostException("x").toDomainError())
    }

    @Test
    fun `cancellation is rethrown`() {
        assertThrows(CancellationException::class.java) { CancellationException("c").toDomainError() }
    }
}
