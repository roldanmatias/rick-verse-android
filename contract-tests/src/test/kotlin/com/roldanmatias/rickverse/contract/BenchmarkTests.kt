package com.roldanmatias.rickverse.contract

import com.roldanmatias.rickverse.core.network.NetworkLog
import com.roldanmatias.rickverse.core.network.interceptor.MetricsInterceptor
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Request count and wall time for loading Rick's detail three ways, measured with the SAME `NetworkLog` the
 * app uses. JVM + real network, so timings describe *this machine's* connection (not a phone's): the request counts
 * are the portable result. Writes a Markdown table to `$BENCHMARK_OUTPUT` (or prints it).
 */
internal class BenchmarkTests : LiveApiTest("RUN_BENCHMARKS") {
    private val log = NetworkLog()
    private val client: OkHttpClient =
        liveClient().newBuilder().addInterceptor(MetricsInterceptor(log)).build()

    private data class Row(val name: String, val requests: Int, val millis: Long)

    private fun measure(name: String, block: () -> Unit): Row {
        log.clear()
        val start = System.nanoTime()
        block()
        val millis = (System.nanoTime() - start) / NANOS_PER_MILLI
        return Row(name, log.events.value.size, millis)
    }

    private fun naive() {
        val rick = client.get("$REST/character/1").json()
        val episodes = rick.toString().let {
            Regex("""/episode/(\d+)""").findAll(it).map { m -> m.groupValues[1] }.toList()
        }
        episodes.forEach { client.get("$REST/episode/$it").use { response -> response.body.string() } }
    }

    private fun batch() {
        val rick = client.get("$REST/character/1").json().toString()
        val ids = Regex("""/episode/(\d+)""").findAll(rick).map { it.groupValues[1] }.joinToString(",")
        client.get("$REST/episode/$ids").use { it.body.string() }
    }

    private fun graphql() {
        val query = "{character(id:\"1\"){id name episode{id name episode air_date}}}"
        client.graphql(query).use { it.body.string() }
    }

    @Test
    fun `measure strategies and write the table`() {
        // The cheap strategies go first: the public API sits behind a rate limiter (Cloudflare error 1015), and the
        // 52-request naive run is exactly what trips it. That is itself a finding for the write-up.
        val batch = measure("REST batch", ::batch)
        val graphql = measure("GraphQL network", ::graphql)
        Thread.sleep(COOL_DOWN_MILLIS) // live benchmark against a rate-limited service; not a unit test
        val naive = measure("REST naive (sequential)", ::naive)
        val rows = listOf(naive, batch, graphql)
        assertEquals(listOf(52, 2, 1), rows.map { it.requests })

        val table = buildString {
            appendLine("| Strategy | Requests | Total ms |")
            appendLine("|---|---:|---:|")
            rows.forEach { appendLine("| ${it.name} | ${it.requests} | ${it.millis} |") }
        }
        System.getenv("BENCHMARK_OUTPUT")?.let { File(it).writeText(table) } ?: println(table)
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
        const val COOL_DOWN_MILLIS = 10_000L
    }
}
