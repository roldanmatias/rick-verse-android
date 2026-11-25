# Notes

Personal interview notes, written in my own words after each phase. See `GUIDE.md` for the question bank.

## Phase 0: build setup

**Why convention plugins and a version catalog?**
SDK levels, Kotlin options and lint rules live in exactly one place (`build-logic`). Modules stay consistent and each
`build.gradle.kts` is a handful of lines, so adding a module is cheap. Versions live in `libs.versions.toml`, which
gives type-safe accessors and one file for Dependabot to update.

**Configuration cache: what breaks it?**
Reading `Project` or other build-time objects inside task actions, reading environment variables/files without
declaring them as inputs, and plugins that are not cache-compatible. All convention plugins here only touch the
`Project` at configuration time, and use `providers.gradleProperty` for opt-in flags.

**`api` vs `implementation`**: default to `implementation` so transitive types do not leak. `:core:domain` uses `api`
for coroutines (Flow is in its public interfaces) and `javax.inject` (qualifiers), on purpose.

**Environment findings (record of what differed from the guide)**
* AGP 9 has built-in Kotlin, so `org.jetbrains.kotlin.android` is not applied. The AGP 9 DSL is non-generic
  (`CommonExtension`) and has no lambda blocks, so convention plugins use property access (`lint.apply { }`).
* Compose BOM 2026.11 needs `compileSdk` 37.1 (`release(37) { minorApiLevel = 1 }`).
* detekt 1.23 cannot parse the JDK 25 version string, so the build uses detekt 2.0 (`dev.detekt`), whose config keys differ.
* AGP forbids `javac --release`; pure JVM modules use it, Android modules use `compileOptions`.

## Phase 1: Domain

**Isn't a use case that only trims a string and forwards it over-engineering?**
In a tiny app the ViewModel could call the repository directly. The use case is a stable seam: one obvious place for
the rule (blank search means no filter), identical behaviour for every screen and both data sources, and a unit
that is trivially testable. I would drop the use cases that are pure pass-through if the app stayed this size and had
no second data source; here they keep ViewModels depending on one narrow type each.

**Why `DomainResult` and not exceptions?** See ADR 0002. The key risk to explain is `CancellationException`: a
`catch (e: Exception)` swallows it and breaks structured concurrency, so `runCatchingDomain` rethrows it and a test
proves that.

**Why is Domain a pure Kotlin module?** The compiler enforces that it cannot import `android.*`, Retrofit, OkHttp
or Apollo. `./gradlew :core:domain:dependencies --configuration runtimeClasspath` lists only the Kotlin stdlib,
coroutines and `javax.inject`.

**Compose stability and Domain**: Domain has no `@Immutable`. With strong skipping (default in current Compose)
unstable parameters are compared by instance equality and lambdas are memoized, and
`config/compose/stability.conf` marks Domain model packages stable without a Compose dependency.

## Phase 2: networking

**Application vs network interceptors.** Application interceptors see the logical request once, may call `proceed` more
than once, and see cached responses; network interceptors see every hop (redirects, retries) as sent on the wire, and
are skipped on cache hits. So: retry = application (it must re-`proceed`), metrics = application (one event per logical
call, includes retries and cache hits), logging = network (the real wire traffic).

**Why only retry idempotent requests?** Repeating a non-idempotent request (POST) can duplicate a side effect. A GraphQL
*query* is a POST but read-only, so it opts in with `X-Retry-Safe`. To retry a real POST safely you need an
idempotency key the server de-duplicates on.

**Thundering herd.** Delay is `random(0..base*2^attempt)` (full jitter) so clients that failed together do not retry
together. `Retry-After` wins when the server tells us when to come back (capped at 30 s so a bad header cannot park a thread).

**Single-flight token refresh.** Five requests get 401 with the same expired token. `TokenAuthenticator` takes a `Mutex`;
the first one refreshes and saves, the others see the stored token changed and just reuse it. A test asserts one refresh.

**Cancellation to the socket.** `viewModelScope.cancel()` -> the Retrofit suspend call's `Call.cancel()` -> OkHttp closes
the socket. A cancelled request is not an error to show: `toNetworkError()` rethrows `CancellationException`, and the retry
interceptor checks `chain.call().isCanceled()` before every attempt.

**OkHttp HTTP cache vs my own Room cache.** HTTP cache honours `Cache-Control`/`ETag` and is transparent, good for
GET responses the server marks cacheable. A database is for offline-first: it is the source of truth, survives
eviction, is queryable and can be observed. This API sends no useful cache headers, hence Room in Phase 10.

**Timeouts (10/20/20/30 s)** are in `NetworkConfig` with the reasoning in its KDoc.

**Environment finding:** OkHttp 5 ships MockWebServer as `mockwebserver3` (`MockResponse.Builder()`,
`server.close()`), not the old `okhttp3.mockwebserver` API.

## Phase 3: REST data layer

**Quirks handled (each has a test against a real fixture):**
1. An empty search is HTTP 404 -> an empty `Page`, not `NotFound` (only for list calls; `detail` 404 stays `NotFound`).
2. `/episode/1` returns an object while `/episode/1,2` returns an array -> `OneOrMany<T>` serializer.
3. `info.next` is a URL -> `parseNextPage` reads the `page` query parameter with `HttpUrl`.
4. `air_date` is `"December 2, 2013"` -> `DateTimeFormatter` pinned to `Locale.ENGLISH`; the test runs with the
   default locale set to Spanish.

**Where Network meets Domain**: only `ErrorMapper.kt`. `:core:network` knows nothing about `DomainError`.
**DTOs are `internal`**: the compiler stops them leaking. Mapping tolerates bad data: unknown status -> `Unknown`,
bad air date -> `null`, malformed episode code -> `InvalidData`.
**Why `withContext(dispatchers.io)` if Retrofit suspend is main-safe?** The network call is, but JSON decoding and
mapping of a 20-item page are CPU work that should not sit on the caller's dispatcher; the dispatcher is injected so
tests control it.

## Phase 4: Characters list

**MVVM + UDF.** `Screen(state, onIntent)`: state flows down, intents flow up. The ViewModel owns a `StateFlow<UiState>`
(coming from BLoC: ViewModel ~ Cubit, `UiState` ~ state, intents ~ events).

**Why one-off events are state, not a `SharedFlow`.** A `SharedFlow` event emitted while the UI is not collecting
(rotation, background) is lost or replayed twice. State is always there when the UI comes back. Here, "refresh failed,
list kept" is derived from state (`Failed` phase with non-empty items), so the snackbar survives rotation.

**`collectAsStateWithLifecycle` vs `collectAsState`.** The lifecycle-aware one stops collecting when the lifecycle drops
below STARTED (app in background), so upstream work can stop; `collectAsState` keeps collecting.

**`WhileSubscribed(5_000)`.** The upstream (debounce, requests) runs only while someone collects. The 5 s grace period
bridges a configuration change: the UI unsubscribes during rotation and re-subscribes within milliseconds, so the
pipeline is not restarted and no request is repeated.

**Stale responses.** One `channelFlow` per set of criteria under `flatMapLatest`. A new query cancels the previous
one including its in-flight page-2 request; `send` after cancellation throws, so even a call that ignores cancellation
cannot append old data (there is a test with a `NonCancellable` fake). The "generation counter" in the guide is
unnecessary because cancellation scope == criteria scope.

**Pagination.** The UI sends `ItemAppeared(lastVisibleId)`, de-duplicated by `snapshotFlow.distinctUntilChanged()` so it
fires once per threshold crossing, not per frame. The ViewModel emits the *page number* it wants; the loop ignores any
request whose page is not the current `nextPage`, so a duplicate intent is harmless.

**Debounce with virtual time.** `advanceTimeBy(400)` tests the debounce instantly. The first emission bypasses the
debounce (`merge(take(1), drop(1).debounce())`) so opening the screen does not wait 400 ms.

**Compose stability.** No `@Immutable` in Domain; strong skipping is on by default and `config/compose/stability.conf` declares
the Domain model packages stable. Check with `-Prickverse.composeMetrics=true`.

**Environment findings.** `hiltViewModel` moved to `androidx.hilt:hilt-lifecycle-viewmodel-compose`; the old
navigation-compose one is deprecated (and warnings are errors here). `createComposeRule`/`runComposeUiTest` are
deprecated in Compose UI 1.13 in favour of `androidx.compose.ui.test.v2.runComposeUiTest`.

**Not verified here:** the app was built and unit-tested but not launched, because this machine has no emulator or device.
