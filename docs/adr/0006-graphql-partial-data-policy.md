# 0006. GraphQL partial data: resilient for lists, strict for single items

Status: accepted

## Context
GraphQL can answer HTTP 200 with both `data` and `errors`, and this schema declares **every field nullable**
(`id: ID`, `name: String`). A response may therefore be partly usable.

## Decision
* **Accept partial data.** If `data` is present it is used even when `errors` is non-empty (`ApolloGraphQlClient`).
  Only `data == null` fails (`NoData` -> `DomainError.Server`).
* **Lists are resilient.** An element with a null/blank name, a null or non-numeric id, or a malformed episode code is
  dropped (`mapNotNull`); one bad element must not blank a page. An unknown `status` becomes `Unknown`.
* **Single items are strict.** A detail whose required fields are missing is `InvalidData` (nothing to skip).
  `character(id:)` returning `null` is `NotFound`, because the server still answers HTTP 200.

## Consequences
* Users see most of the data when the backend is degraded. Cost: silent loss of malformed items. A counter/log of
  dropped items is a sensible follow-up (not implemented: there is no logging dependency in the data layer).
* Behaviour is pinned by `GraphQlRepositoryTest` (skipped items, partial data, null character, errors-only).
