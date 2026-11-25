# Benchmark: loading Rick Sanchez's detail (51 episodes)

Same screen, four ways to fetch it. **Request counts are measured by tests** (JVM, MockWebServer, real captured
fixtures); **timings and bytes on a device are not measured yet** (see the checklist: no numbers are invented here).

| Strategy | Requests | Asserted by | Notes |
|---|---:|---|---|
| REST naive (`async` per episode, Semaphore 6) | 52 | `RestCastRepositoryDetailTest` at commit `139acd9` | 1 + 51; concurrency only hides latency |
| REST batch (`/episode/1,2,...,51`) | 2 | `RestCastRepositoryDetailTest` (`costs exactly 2 requests`) | order preserved; one id returns an object |
| GraphQL, network | 1 | `GraphQlRepositoryTest` (`detail is one request`) | nested `episode { ...EpisodeBasic }` |
| GraphQL, `CacheFirst`, second open | 0 | `GraphQlRepositoryTest` (`CacheFirst serves the second identical call`) | normalized cache hit |

## What the numbers mean
* The naive version scales with the data (N+1); batch and GraphQL do not. The fix on REST is an API feature (the batch
  endpoint), not a client trick, and it costs a second round trip that GraphQL avoids by nesting.
* GraphQL's single request is also smaller on the wire for the *list* screens (5 fields instead of 12), which matters
  more on mobile networks than the request count alone.
* A cache hit is zero requests and no network latency at all; `CacheAndNetwork` shows that instantly and revalidates.
* Caveat: GraphQL moves the N+1 to the server (nested resolvers). The public API protects itself with depth limits.

## Measure on a device (TODO checklist)
- [ ] Release-like build (`./gradlew assembleRelease` with minify) on a physical device, same Wi-Fi.
- [ ] Open character 1 five times per strategy (switch in Settings, clear the HTTP and GraphQL caches between runs).
- [ ] Read count, duration and bytes from **Settings -> Network log**; average the 5 runs.
- [ ] Fill this table and commit it:

| Strategy | Requests | Avg total ms | Bytes |
|---|---:|---:|---:|
| REST naive | 52 | _tbd_ | _tbd_ |
| REST batch | 2 | _tbd_ | _tbd_ |
| GraphQL network | 1 | _tbd_ | _tbd_ |
| GraphQL cache | 0 | _tbd_ | 0 |
