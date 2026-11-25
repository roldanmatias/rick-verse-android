# 0010. Offline cache with Room, and manual pagination vs Paging 3

Status: accepted (Paging 3 variant: decision only, not implemented)

## Context
The app should keep working on a bad connection, and the cast list is paginated, searchable and filterable.

## Decision: offline cache
* `:data:local` (Room) holds `cast_member`, `episode` and a `cast_episode` join table. `OfflineFirstCastRepository` is a
  **decorator** over the `@Remote` repository (whichever of REST/GraphQL is active), bound as the unqualified
  `CastRepository`. Features still know nothing about it.
* **Detail** is cache-then-network: Room is read first (instant, works offline), the remote refreshes and the result is
  saved, `fromCache` tells the UI to show a banner. **Lists** are network-first with fallback to Room on *transient* errors
  (Offline, Timeout, Server, Unknown) with `Page.fromCache = true`. `NotFound`/`InvalidData` are answers, not outages.
* Deviation from "the database is the single source of truth": a true SSOT needs the UI to observe a `Flow` from Room and
  the network to only write. For a searchable, paginated list that means `RemoteMediator`-style plumbing and a local
  query that mirrors server search semantics. We kept the existing `suspend` contract and accepted that offline search is
  an approximation (`LIKE`), and offline pagination only covers pages seen before.
* Schema is exported and committed (`data/local/schemas`); there is one real additive migration (1 -> 2, detail columns)
  with a `MigrationTest` and no destructive fallback. List upserts never wipe detail columns (`saveList` = insert-ignore + update).
* Episodes are not cached offline (not required by the screens' offline story); noted as a gap.

## Decision: Paging 3
Keep the **manual** pagination from Phase 4 (`Page<T>` in Domain, a `channelFlow` per criteria in the ViewModel). Not
implemented as a second variant.

| | Manual (chosen) | Paging 3 |
|---|---|---|
| Control | Full: duplicate requests, retry footer, de-dup by id are explicit | Hidden in the library; configured via `PagingConfig` |
| Testability | Plain `StateFlow`, fakes, virtual time | Needs `asSnapshot`/`TestPager`; `LazyPagingItems` is UI-bound |
| Boilerplate | More code we own | Less, but `PagingSource` + `RemoteMediator` + `LazyPagingItems` quirks (refresh/append load states, `itemKey`) |
| Debounced search | `debounce` + `flatMapLatest` (done) | `flatMapLatest` into a new `Pager`; same idea |
| Domain purity | `Page<T>` is ours | Paging types must stay out of Domain (a mapper layer or `:data` only) |
| Offline | Our fallback above | Best fit: `RemoteMediator` + Room is the canonical SSOT |

If the list needed true offline-first with infinite scroll, Paging 3 + RemoteMediator + Room would be the right call,
and the dependency rule would still apply: Paging types live in the data layer and are mapped to Domain models.

## Consequences
* The cache can serve stale data indefinitely (no expiry); acceptable for this data, worth a TTL in a real app.
* The Room database is part of `allowBackup=false` scope: it is a re-downloadable cache.
