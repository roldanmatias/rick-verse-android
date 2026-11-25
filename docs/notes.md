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

## Phase 5: Detail, episodes and the N+1 problem

**N+1.** Rick appears in 51 episodes. The API's character payload only lists episode *URLs*, so the naive approach is
1 + 51 = **52 requests**. Version A (git: `feat(rest): character detail with concurrent episode fetching`) ran them with
`async`/`awaitAll` behind a `Semaphore(6)`: faster, but still 52 requests and still hammering the server.
Version B (`perf(rest): use batch endpoint to fix N+1`) uses `GET /episode/1,2,...,51`: **2 requests**, independent of
episode count. Tests assert the exact request count (`web.server.requestCount == 2`) against real captured fixtures.

**Why keep concurrency in mind anyway.** When there is no batch endpoint, bound the fan-out (Semaphore) and make it
structured (`coroutineScope`) so one failure cancels the siblings. `awaitAll` preserves input order.

**API quirk.** A batch call with a single id returns an object, not an array. `OneOrMany` absorbs that in the DTO layer.

**Navigation.** Typed `@Serializable` routes; arguments land in `SavedStateHandle` under the property name, so the
ViewModel reads `savedStateHandle["id"]` without depending on `:app`'s route class (features never depend on `:app`).
Cross-links (episode -> character) navigate to the other tab's graph. Deep links `rickverse://character/{id}` and
`rickverse://episode/{id}` are declared in both the manifest and the nav graph. Predictive back is enabled with
`android:enableOnBackInvokedCallback`.

**Not verified here:** no emulator, so deep links and predictive back are configured but not exercised on a device.

## Phase 6: GraphQL and Apollo Kotlin setup

**Headline.** Character detail is **1 request** in GraphQL (nested `episode { ...EpisodeBasic }`), versus 52 (naive REST)
and 2 (REST batch). The client chooses the shape, so no over-fetching (the list asks for 5 fields, not 12) and no N+1 on
the client. The N+1 moves to the server (resolvers per nested field), which is why public APIs add depth and cost limits:
this one rejects deep queries (`GCDN_QUERY_DEPTH_LIMIT`, HTTP 413), which I hit while exploring.

**Honest limitations of this API.** Read-only: no mutations, no subscriptions. Theory only: input types, optimistic
responses, cache updates after a write, subscriptions over WebSocket. Every field in the schema is nullable
(`id: ID`, `name: String`), so mappers must treat missing data as `InvalidData`, not crash.

**HTTP 200 is not success.** GraphQL returns errors in the body (`errors`, possibly with partial `data`). The repository
(Phase 7) must check `response.errors` and `exception`, not just the status code.

**Apollo Kotlin 5.2 differences from older docs** (the guide warned about this):
* `visibility.set("internal")` no longer exists: it is `generateAsInternal.set(true)`.
* `warnOnDeprecatedUsages` is deprecated in favour of `issueSeverity`.
* `./gradlew downloadApolloSchema --endpoint ...` is gone. Declare `introspection { endpointUrl; schemaFile }` in the
  service and run `downloadRickandmortyApolloSchemaFromIntrospection`.
* Parsing a response in a test: `Operation.parseResponse(JsonReader)` with `Buffer().writeUtf8(json).jsonReader()`.
* Apollo adds `__typename` to every selection set, so hand-captured fixtures must request it too, otherwise parsing
  fails with `NullOrMissingField`.

**Generated code is not committed.** Unlike iOS, Apollo writes to `build/`, so there is no "regenerate and commit"
step and no merge conflicts in generated files. CI builds it with every `assembleDebug`.

**Cache keys.** `@typePolicy(keyFields: "id")` on Character, Episode and Location (`extra.graphqls`). Used by the
normalized cache in Phase 8: the same Rick reached from a list, a detail and an episode is one record.

## Phase 7: GraphQL repositories and the runtime switch

**One interface for the client.** `GraphQlClient` (internal) returns data or throws `GraphQlClientError`; repositories
never see `ApolloClient`, `ApolloResponse` or Apollo exceptions. Apollo's policies map in one function. A one-shot
`execute()` can't emit twice, so `CacheAndNetwork` degrades to `NetworkFirst` there; Phase 8 adds the two-emission Flow.

**Shared OkHttp.** Apollo is built with `.okHttpClient(sharedClient)`, so default headers, retry, metrics and logging
apply to GraphQL as they do to REST.

**Cache lib 1.0.x differs from older docs.** `normalizedCache(...)` has no default key generator: pass `typePolicies` and
`fieldPolicies` explicitly (mirroring `extra.graphqls`), and `MemoryCacheFactory` lives in `...cache.normalized.memory`.
Fragment classes are generated in `generated.fragment`. `ApolloParseException` is deprecated (JSON problems are
`JsonDataException`/`JsonEncodingException`).

**REST vs GraphQL behavioural differences (absorbed in the data layer):**
| | REST | GraphQL |
|---|---|---|
| Empty search | HTTP 404 | 200 with `results: []` |
| Missing item | HTTP 404 | 200 with `character: null` |
| Failure with 200 | n/a | `errors` in the body, maybe partial `data` |
| Pagination | `info.next` is a URL (parse the page) | `info.next` is an `Int?` |
| Detail + episodes | 2 requests (batch) | 1 request |

**Why tests use a real ApolloClient + MockWebServer** (not a fake `GraphQlClient`): it exercises codegen adapters,
variables serialization (`Optional.presentIfNotNull`), HTTP errors and a dead server end to end, and it proves the
normalized cache (`CacheFirst` second call = 0 requests).

**DataStore.** One `DataStore<Preferences>` per file per process (singleton). Enums stored by name with a default fallback.
Tests inject a store on a temp file with `backgroundScope`.

**Switching.** `SwitchingCastRepository` reads the source at call time. See ADR 0004 and ADR 0009 for the UI reset and
the ViewModel-scoping subtlety (`key()` alone does not clear ViewModels).

**Not verified here:** the reset-on-switch flow was not exercised on a device (no emulator); the pieces are unit tested
but the `LocalViewModelStoreOwner` override is the riskiest part to confirm manually.

## Phase 8: shared interceptors, network log, cache policies

**One observability pipeline.** Apollo runs on the shared `OkHttpClient`, so headers, retry, metrics and logging already
cover GraphQL. GraphQL queries are POSTs, which `RetryInterceptor` refuses to retry; Apollo calls add `X-Retry-Safe` so
read-only queries are retried and mutations (when they exist) are not.

**Two layers, two views.**
| | OkHttp `MetricsInterceptor` | Apollo `CacheEventInterceptor` |
|---|---|---|
| Sees | every HTTP exchange, status, bytes, retries | operation name, cache hit/miss |
| Misses | cache hits (no HTTP happens) | nothing about HTTP |
| Logs | network calls only | **cache hits only** (to avoid double counting) |
The interceptor is added *before* `normalizedCache` so it sits outside the cache interceptor and sees `cacheInfo`.

**Stale-while-revalidate.** `CastRepository.detailUpdates(id): Flow` has a default (emit `detail` once: REST). GraphQL with
the `CacheAndNetwork` policy emits cache then network; a cold-cache `CacheMissException` is skipped, not a failure.
`CastDetailViewModel` collects it: each emission replaces state, and a failure *after* data is shown is ignored
(stale beats an error screen). To avoid UI jumping: same list keys (`key = { it.id }`) and no scroll reset, so the second
emission is a diff, not a rebuild.

**Persistent cache.** Memory LRU (2 MB) chained in front of SQLite (`SqlNormalizedCacheFactory`). Eviction is by size;
invalidation is manual (Settings -> Clear GraphQL cache). Not configured: time-based `maxAge`, so cached data never
expires on its own; a real app would set max ages per type and use `maxStale` on fetch.

**Cache lib 1.0.9 notes.** `ApolloClient.apolloStore.clearAll()`; `response.cacheInfo?.isCacheHit`; `CacheMissException` is
in `apollo.exception`.

**Not verified here:** airplane-mode cold start, SQLite cache on a device, and the network log screen were not run
(no emulator). Device timings are a TODO in `docs/benchmark.md`.

## Phase 9: testing strategy

**Pyramid in this repo.** Domain/ViewModel/data tests on the JVM with fakes and MockWebServer; Compose UI tests on the JVM
through Robolectric; screenshot tests (Roborazzi); a Hilt end-to-end test (instrumented); Konsist architecture tests;
gated contract and benchmark suites against the live API.

**Architecture tests found two real problems on first run** (rules earn their keep): test classes were swept into "use
case" and "DTO" scopes (fixed by scoping to production code), and a `@HiltViewModel` called `DataSourceScopes` broke the
`*ViewModel` naming rule (renamed). `scripts/check-architecture.sh` is the Gradle-free version of the import rules.

**Contract/benchmark gating.** Tests live in `:contract-tests`, skipped by JUnit `Assume` in a normal `test`, and run by
`contractTest` / `benchmarkTest` (not wired into `check`) so a third-party outage can never fail a PR. The live benchmark
hit the API's rate limiter (Cloudflare 1015) when the 52-request naive run came first: see `docs/benchmark.md`.

**Screenshots.** `recordRoborazziDebug` writes goldens, `verifyRoborazziDebug` compares; a plain `test` captures nothing,
so the default gate stays deterministic. Native graphics rendering differs by OS: the goldens in the repo were recorded
on macOS, so a Linux CI verify may need its own recorded baseline (TODO when the CI job is added).

**Coverage.** Gate raised from 60 to 80. The figure excludes composables, DI modules, navigation, design system, Apollo
generated code and test fixtures (UI is covered by UI/screenshot/e2e tests, not line coverage). Measured: ~95% of what
remains. Be able to say that out loud: the number is high because the denominator is deliberately logic-only.

**E2E.** `HiltTestRunner` + `@TestInstallIn` replacing `RepositoryModule` with fakes; semantic matchers only.
`compose.ui.test.junit4.v2.createAndroidComposeRule` (the old one is deprecated: v2 uses `StandardTestDispatcher`).
**Not run here** (no emulator): it compiles, it has not executed. First thing to run: `./gradlew :app:connectedDebugAndroidTest`.

## Phase 10: Android extras

**Offline-first.** Decorator over `@Remote`. Gotcha that needed a test: a list refresh (`@Upsert`) would wipe the detail
columns of the same row, so list writes are insert-ignore + update-basics. Another: a row that only came from a list has no
detail, so `gender == null` means "detail not cached" and offline detail correctly fails instead of showing a half-empty screen.
LIKE wildcards in user input (`%`, `_`) are escaped; tested.

**Migration.** Additive columns, nullable, so existing rows survive. The test uses the exported `1.json`/`2.json` through
`MigrationTestHelper` under Robolectric (JVM), no emulator.

**Dependency injection detail.** A Kotlin `() -> Long` cannot be injected by Dagger (`Function0<? extends Long>` mismatch),
so the clock was dropped instead of fighting generics.

**Performance.**
* Compose compiler metrics (`-Prickverse.composeMetrics=true`): every restartable composable in `:feature:characters`,
  `:core:ui` and `:core:designsystem` is skippable and none has unstable parameters, thanks to strong skipping and
  `config/compose/stability.conf` (Domain models declared stable). I did **not** find a real recomposition problem to fix,
  so there is no "before/after" story here and I won't invent one. Layout Inspector recomposition counts need a device.
* Lists use `key` and `contentType`. Coil avatar sizing is fixed per slot.
* R8 full mode + resource shrinking: `assembleRelease` succeeds with Hilt, Room, Apollo, Retrofit, kotlinx.serialization.
  **Not run:** a smoke test on the release variant (needs a device); that is the real proof.
* `:benchmark` (`com.android.test` + Baseline Profile plugin): `StartupBenchmark` (no compilation vs baseline profile),
  `ScrollBenchmark`, `BaselineProfileGenerator`. It builds; it has **never run** (no device). The app has `profileinstaller`.
* Debug-only StrictMode (`penaltyLog`) and LeakCanary.

**Accessibility/adaptivity.** Rows are merged semantics, status is text plus colour, offline banner is a polite live
region, screenshots exist at 2.0x font. The adaptive navigation suite switches bar/rail by window size.
**Not done:** a list-detail (two-pane) layout for tablets/foldables, and a recorded TalkBack walkthrough.

**Localization.** Spanish for every module, per-app language through `generateLocaleConfig` (Android 13+ system settings;
below 13 would need AppCompat's locale APIs, not added). Lint's `PluralsCandidate` forced "Requests: %d" instead of "%d requests".

**Security basics.** `networkSecurityConfig` forbids cleartext; `allowBackup=false` with explicit extraction rules;
logging interceptor is debug-only and never logs bodies; no secrets exist in this repo (pattern for future ones:
`local.properties`/CI secrets into `BuildConfig`, never committed).

## Phase 11: CI/CD

Written and YAML-validated only: **no workflow has executed**, as the repo is not pushed. See `docs/GITHUB-SETUP.md`.

* Tiers: hermetic PR checks (fakes, fixtures, MockWebServer) vs nightly live-API checks. A third-party outage can never
  redden a PR.
* `pull_request_target` is used only for title/label jobs and never checks out PR code (write token + untrusted code is the
  classic Actions takeover).
* Path-filtered or label-gated workflows are not required checks, otherwise PRs wait forever.
* Release signing is wired through environment variables in `app/build.gradle.kts`, only when `KEYSTORE_PATH` exists, so
  local and PR builds never need a keystore. Version name comes from the tag, version code from the run number.
* Dokka is applied via the quality convention plugin and aggregated at the root (`dokkaGeneratePublicationHtml`, verified locally).
* The Apollo schema download is deterministic (re-running it produced no diff), which is what makes the drift check trustworthy.
* The Play upload step is a deliberate placeholder (fails) rather than a pretend integration.
