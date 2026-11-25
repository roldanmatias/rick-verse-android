package com.roldanmatias.rickverse.contract

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.junit.Assume.assumeTrue
import org.junit.Before
import java.util.concurrent.TimeUnit

internal const val REST = "https://rickandmortyapi.com/api"
internal const val GRAPHQL = "https://rickandmortyapi.com/graphql"

/** Base for tests that need the real network: skipped unless the env var named by [switch] is `1`. */
internal abstract class LiveApiTest(private val switch: String) {
    @Before
    fun requireOptIn() {
        assumeTrue("Set $switch=1 to run live-API tests", System.getenv(switch) == "1")
    }
}

internal fun liveClient(): OkHttpClient =
    OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build()

internal fun OkHttpClient.get(url: String): Response = newCall(Request.Builder().url(url).build()).execute()

internal fun OkHttpClient.graphql(query: String, variables: String = "{}"): Response {
    val body = """{"query":${Json.encodeToString(String.serializer(), query)},"variables":$variables}"""
    return newCall(
        Request.Builder().url(GRAPHQL).post(body.toRequestBody("application/json".toMediaType())).build(),
    ).execute()
}

internal fun Response.json(): JsonElement = use { Json.parseToJsonElement(it.body.string()) }

private fun String.Companion.serializer() = kotlinx.serialization.serializer<String>()
