# 0004. REST and GraphQL behind one repository interface, switched via Hilt

Status: accepted

## Context
The same screens must work against REST and against GraphQL, and the user can change backend at runtime.

## Decision
* Domain defines `CastRepository` / `EpisodeRepository`. `:data:rest` and `:data:graphql` each implement them and expose
  them under `@Rest` / `@GraphQl` qualifiers (Hilt `@Binds`). Nothing else from either module is public.
* `:app` is the only module that sees both. It binds the **unqualified** interfaces to `SwitchingCastRepository` /
  `SwitchingEpisodeRepository`, which read `SettingsRepository.dataSource` at call time and delegate.
* Features inject the unqualified interface through use cases and never learn which backend answered.

## Consequences
* Adding a third backend (Room-backed, say) means one more qualifier and one more branch in `:app`.
* Both data modules map to the same Domain types and the same `DomainError`s, so behaviour differences are visible
  in tests, not in features (REST 404-on-empty-search vs GraphQL empty list is absorbed in the data layer).
* A call in flight when the user switches finishes against the backend it started with: acceptable, and the UI is
  reset anyway (ADR 0009).
* Rejected: a Hilt component per backend, or `Provider<CastRepository>` re-resolved per call. More machinery for
  the same result.
