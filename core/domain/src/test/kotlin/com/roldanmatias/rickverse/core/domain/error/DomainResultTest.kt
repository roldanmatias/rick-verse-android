package com.roldanmatias.rickverse.core.domain.error

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class DomainResultTest {
    @Test
    fun `map transforms success`() {
        assertEquals(DomainResult.Success(4), DomainResult.Success(2).map { it * 2 })
    }

    @Test
    fun `map leaves failure untouched`() {
        val failure: DomainResult<Int> = DomainResult.Failure(DomainError.Timeout)

        assertSame(failure, failure.map { it * 2 })
    }

    @Test
    fun `fold picks the matching branch`() {
        val success: DomainResult<Int> = DomainResult.Success(1)
        val failure: DomainResult<Int> = DomainResult.Failure(DomainError.Offline)

        assertEquals("ok 1", success.fold({ "ok $it" }, { "error $it" }))
        assertEquals("error Offline", failure.fold({ "ok $it" }, { "error $it" }))
    }

    @Test
    fun `getOrNull returns the value or null`() {
        assertEquals(1, DomainResult.Success(1).getOrNull())
        assertNull(DomainResult.Failure(DomainError.NotFound).getOrNull())
    }

    @Test
    fun `runCatchingDomain wraps a returned value`() =
        runTest {
            assertEquals(DomainResult.Success("x"), runCatchingDomain { "x" })
        }

    @Test
    fun `runCatchingDomain maps exceptions with the supplied mapper`() =
        runTest {
            val result = runCatchingDomain(mapError = { DomainError.Server(500) }) { throw IOException("boom") }

            assertEquals(DomainResult.Failure(DomainError.Server(500)), result)
        }

    @Test
    fun `runCatchingDomain maps to Unknown by default`() =
        runTest {
            val result = runCatchingDomain { error("boom") }

            assertEquals(DomainResult.Failure(DomainError.Unknown("boom")), result)
        }

    @Test
    fun `runCatchingDomain rethrows CancellationException instead of mapping it`() =
        runTest {
            var mapperCalled = false

            assertThrows(CancellationException::class.java) {
                kotlinx.coroutines.runBlocking {
                    runCatchingDomain(mapError = {
                        mapperCalled = true
                        DomainError.Unknown()
                    }) {
                        throw CancellationException("cancelled")
                    }
                }
            }

            assertEquals(false, mapperCalled)
        }
}
