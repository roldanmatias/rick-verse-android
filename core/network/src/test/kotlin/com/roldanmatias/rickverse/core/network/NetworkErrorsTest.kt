package com.roldanmatias.rickverse.core.network

import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

@RunWith(Parameterized::class)
class NetworkErrorsMappingTest(
    private val name: String,
    private val throwable: Throwable,
    private val expected: NetworkError,
) {
    @Test
    fun `maps the throwable`() {
        assertEquals(name, expected, throwable.toNetworkError())
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf("UnknownHost", UnknownHostException("x"), NetworkError.Offline),
                arrayOf("ConnectException", ConnectException("x"), NetworkError.Offline),
                arrayOf("SocketTimeout", SocketTimeoutException("x"), NetworkError.Timeout),
                arrayOf("call timeout", InterruptedIOException("timeout"), NetworkError.Timeout),
                arrayOf("Serialization", SerializationException("bad"), NetworkError.Decoding("bad")),
                arrayOf("IOException", IOException("reset"), NetworkError.Transport("reset")),
                arrayOf("unknown", IllegalStateException("odd"), NetworkError.Transport("odd")),
                arrayOf(
                    "HTTP 503",
                    HttpException(Response.error<Any>(503, "down".toResponseBody())),
                    NetworkError.Http(503, "down"),
                ),
            )
    }
}

class NetworkErrorsCancellationTest {
    @Test
    fun `CancellationException is rethrown, never mapped`() {
        assertThrows(CancellationException::class.java) { CancellationException("cancelled").toNetworkError() }
    }
}
