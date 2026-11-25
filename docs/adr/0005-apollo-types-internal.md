# 0005. Apollo-generated types stay `internal` to `:data:graphql`

Status: accepted

## Context
Apollo generates Kotlin classes (`CastPageQuery.Data`, fragments, `Optional<T>`) from the schema. If those leak into
Domain, UI or tests elsewhere, switching REST/GraphQL stops being a binding change and every consumer depends on the
GraphQL client and on a schema that has every field nullable.

## Decision
`generateAsInternal.set(true)` in the Apollo service block. Only the repositories and mappers in `:data:graphql` see
the generated types; they expose Domain models and `DomainResult`, exactly like `:data:rest`. The `ID` scalar stays a
`String` in generated code and is converted to `Int` in the mapper (one visible, tested conversion instead of a hidden
custom-scalar adapter).

## Consequences
* Generated code is not committed (it is written to `build/` by `generateApolloSources`; CI runs it as part of the build).
  The committed inputs are `schema.graphqls`, `extra.graphqls` and the `.graphql` operations.
* Tests in `:data:graphql` can still use the generated types (same module).
* Cost: no sharing of generated types across modules, which is the point.
