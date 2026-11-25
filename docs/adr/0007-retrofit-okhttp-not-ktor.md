# 0007. Retrofit + OkHttp (and why not Ktor)

Status: accepted

## Context
The REST side needs an HTTP client, a typed API layer and JSON serialization. Apollo Kotlin (GraphQL) also runs on
OkHttp on Android. The candidates were Retrofit + OkHttp + kotlinx.serialization, or Ktor client.

## Decision
**Retrofit + OkHttp + kotlinx.serialization.**

* One `OkHttpClient` is shared by Retrofit *and* Apollo, so the header, retry, logging and metrics interceptors apply to
  both APIs and feed one network log. With Ktor that would be two stacks with two pipelines.
* OkHttp is the de-facto Android standard: `Authenticator`, HTTP cache, connection pooling, MockWebServer for tests.
* Retrofit `suspend` functions are cancellation-aware (the underlying `Call` is cancelled with the coroutine).
* kotlinx.serialization is compile-time and reflection-free, so it needs no keep rules and is R8-friendly.

Ktor would be the right choice for Kotlin Multiplatform, because it is multiplatform and OkHttp is not. This project is
Android-only.

## Consequences
* `:core:network` is a pure JVM module. The `Context`-dependent HTTP cache directory is passed in from `:app`.
* Interceptor placement is a deliberate decision: headers, retry and metrics are *application* interceptors (they see the
  logical request once); logging is a *network* interceptor (it sees every hop on the wire).
