# Interview answers (short, tied to this repo)

Written to be defended out loud. Where the repo does *not* do something, the answer says so.

## REST & networking
1. **Retrofit `suspend` call.** The generated proxy builds an OkHttp `Call`, enqueues it, and the continuation resumes when it completes; the call is cancelled if the coroutine is. Application interceptors run first (headers, metrics, retry), then the cache and connection layers, then network interceptors. kotlinx.serialization decodes the body to a DTO, a mapper turns it into a Domain model, and `runCatchingDomain` turns failures into `DomainError`.
2. **Interceptors.** Application interceptors for retry and metrics: they see the logical call once and may `proceed` several times (a network interceptor would see each attempt, and not the cache). Metrics sits *outside* retry so one logical call is one log row including retries.
3. **Idempotent only.** Repeating a GET/PUT/DELETE has no extra effect; repeating a POST may duplicate it. `RetryInterceptor` retries only those. GraphQL queries are POSTs but read-only, so they carry an internal `X-Retry-Safe` header. A real POST needs an idempotency key the server honours; this API has none, so it is not implemented.
4. **Thundering herd / Retry-After.** Delay is `random(0..base*2^attempt)` (full jitter) so clients desynchronise. `Retry-After` (seconds or date) overrides the computed delay, capped. Both are unit-tested with an injected `Sleeper`.
5. **Concurrent 401.** `TokenAuthenticator` serialises refresh behind a `Mutex`, re-checks whether the token already changed, and only the first caller refreshes. It is unit-tested but unused: this API has no auth.
6. **Cancellation path.** `viewModelScope` cancel -> suspending Retrofit call cancelled -> `Call.cancel()` -> the socket closes. The retry interceptor checks `isCanceled()` between attempts, and `CancellationException` is rethrown, never mapped to an error.
7. **HTTP cache vs Room.** OkHttp's cache follows HTTP semantics, needs no code, and is bounded by what the server's headers allow. Room is for data the app must show offline and query/join. Here both exist: HTTP cache (REST), Apollo's SQLite cache (GraphQL), Room (offline cast).
8. **1% decode errors.** Honest gap: no crash/error reporter is wired, so today I could not see it. Plan: report `InvalidData` with endpoint and field path (no PII), make mappers tolerant (lists skip bad items, single items fail loudly: ADR 0006), and add the captured payload as a fixture.
9. **Retrofit vs Ktor.** Larger ecosystem, shared OkHttp client with Apollo (one interceptor chain and one network log), team familiarity; Ktor wins for multiplatform ([ADR 0007](adr/0007-retrofit-okhttp-not-ktor.md)).

## GraphQL & Apollo
10. **Solves/creates.** Solves over/under-fetching and chatty clients; creates caching complexity (everything is POST to one URL), query cost on the server, error-model subtleties, and codegen in the build.
11. **200 with `data` and `errors`.** Lists: map what is valid and skip bad items; single item: fail, since half a detail screen is misleading ([ADR 0006](adr/0006-graphql-partial-data-policy.md)). HTTP-level errors still map to `DomainError`.
12. **Normalized cache.** Each object is stored by cache key (`__typename:id`) and queries reference those records, so updating a character in one query updates it everywhere. Without an `id` the object is cached under the query path and not shared. Configured with memory + SQLite.
13. **Policies.** Feed: `CacheAndNetwork` (instant, then refresh). Profile: `CacheFirst` or `CacheAndNetwork`. Bank balance: `NetworkOnly`; stale money is a bug.
14. **Internal generated code.** Apollo types never leak: only `:data:graphql` can change when the schema or Apollo does ([ADR 0005](adr/0005-apollo-types-internal.md)); Konsist enforces it.
15. **Auth.** An OkHttp interceptor adds the header for both REST and GraphQL (shared client); refresh via the `Authenticator`. Not needed here; the pieces exist and are tested.
16. **Testing Apollo.** MockWebServer with real captured responses through the real Apollo client (what runs here), or Apollo's test `MockServer`/fake data sources. Mapping logic is tested from fixtures.
17. **"GraphQL solves N+1."** For the client's *round trips*, yes (52 -> 1). The N+1 moves to the server's resolvers unless batched (DataLoader). The public API limits depth.
18. **Optimistic update.** Not built (read-only API). Approach: write the expected result to the normalized cache via `optimisticUpdates`, send the mutation, and Apollo rolls it back automatically on failure.
19. **Cursor vs page.** Cursors are stable under inserts/deletes and fit infinite scroll; page numbers allow random access but can skip/duplicate items. This API is page-based; the list de-duplicates by id because pages can overlap.

## Architecture, Kotlin & coroutines
20. **Pure Kotlin Domain.** The compiler forbids Android/Retrofit/Apollo imports, so the rule can't be broken by accident; tests run on the JVM in milliseconds.
21. **`DomainResult`.** Errors are visible in the type and exhaustively handled in `when`. Downsides: more ceremony, easy to forget to check, and it must still let `CancellationException` through ([ADR 0002](adr/0002-domain-result-instead-of-exceptions.md)).
22. **`catch (e: Exception)`.** It catches `CancellationException`, so the coroutine looks like it failed or keeps running after being cancelled. Rethrow it or catch narrow types; `runCatchingDomain` centralises this.
23. **Stale results.** All loading for one set of criteria runs inside a `channelFlow` under `flatMapLatest`; new criteria cancel the previous one including its in-flight next page.
24. **StateFlow vs SharedFlow.** StateFlow for state (always has a value, conflated). One-off events: model them as state that is consumed, or a `Channel`/`SharedFlow` with care; this app avoids events by keeping navigation in the UI layer.
25. **`WhileSubscribed(5000)`.** Upstream stops 5 s after the last collector leaves, surviving a rotation without restarting. `collectAsStateWithLifecycle` stops collecting when the UI is not STARTED, saving work and avoiding updates to a dead UI.
26. **Dispatchers.** Network/disk work on `io`, state in `viewModelScope` (main). Injected `DispatcherProvider` lets tests use one test dispatcher with virtual time.
27. **30 engineers.** Split by feature ownership with CODEOWNERS per module, API/impl module split for data, remote build cache, a merge queue, and keeping module count in check since each costs build time.

## Hilt & DI
28. **Switching backends.** `SwitchingCastRepository` is bound to `@Remote` in `:app`; it reads the data-source setting and delegates to `@Rest` or `@GraphQl`. Room's decorator wraps `@Remote` and is the unqualified `CastRepository` features inject.
29. **Scopes.** `@Singleton` lives for the process; `@ViewModelScoped` for one ViewModel. Repositories are stateless/shared and own caches or clients, so they are singletons.
30. **Tests.** `@TestInstallIn` replaces the repository module with fakes (`FakeRepositoryModule`, compiled but unrun here). Downsides vs Koin: kapt/KSP build time and compile-time rigidity; upsides: compile-time graph validation ([ADR 0008](adr/0008-hilt-not-koin.md)).

## Compose & UI
31. **State hoisting.** `Screen(state, onIntent)` keeps the composable stateless, previewable and testable; the ViewModel owns state.
32. **Pagination trigger.** `ItemAppeared(id)` is checked against how close the item is to the end and whether pagination is `Idle`; the request carries the page number, so duplicates are ignored.
33. **Skippable.** All parameters stable or unchanged. Strong skipping makes composables with unstable parameters skippable (compared by identity), and the stability config marks Domain models stable. Metrics showed no unstable composables in the checked modules.
34. **Reset on switch.** `key()` rebuilds composition but ViewModels live in the `ViewModelStore` of the Activity/back stack entry, so they would survive with old data. Hence an epoch-scoped `ViewModelStoreOwner` ([ADR 0009](adr/0009-reset-ui-on-data-source-switch.md)). Unverified on a device.
35. **Accessibility.** Merged row semantics, status as text plus colour, content descriptions, 48dp targets, offline banner as live region, font-scale 2.0 screenshots. Tested via Compose UI tests and screenshots; no TalkBack run yet.

## Testing, CI & AI
36. **Debounce.** `runTest` virtual time: `advanceTimeBy(400)`; no real waiting.
37. **Unit vs instrumented.** Logic, mapping, ViewModels, Compose UI (Robolectric) and migrations on the JVM; instrumented only for what needs a real runtime (full Hilt graph, navigation, deep links).
38. **Flaky emulator tests.** Hermetic fakes, animations off, one retry, a cached AVD, nightly API matrix. Unproven until it runs.
39. **Enforcement.** Module graph (Domain can't import Android), Konsist tests, `check-architecture.sh` with annotations, and a detekt/Lint/Kover gate.
40. **AI policy.** Allowed: boilerplate, review suggestions, test ideas, with the pipeline as judge. Forbidden: merging, secrets, weakening gates, unexplained code. **Measurement: not done**; I would not claim a result.
41. **Prompt injection.** PR text and code are untrusted. Mitigations: plain `pull_request` (no secrets for forks), read-only tool allow-list, no merge/approve rights, collaborator-only `@claude`, capped turns, human review ([ADR 0011](adr/0011-ai-in-the-workflow.md)).
