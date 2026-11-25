# 0008. Hilt (and why not Koin)

Status: accepted

## Context
The app needs DI to switch REST/GraphQL implementations in one place and to replace bindings in tests.

## Decision
**Hilt.** Dagger's graph is validated at **compile time**: a missing binding fails the build, not a user's session.
It is the Google-recommended default, integrates with `ViewModel`, `SavedStateHandle` and Navigation, and
`@TestInstallIn` replaces whole modules in instrumented tests.

Koin has less boilerplate and no code generation, but resolves at runtime (a missing binding crashes when first used)
and is slower at scale. Manual DI is viable for a small app but re-implements what Hilt generates.

## Consequences
* Cost: KSP processing and longer builds, annotation noise, and a steeper learning curve.
* Hilt modules live in the module that owns the implementation and are `internal`; only `:app` chooses between
  `@Rest` and `@GraphQl` bindings.
* The use cases are unscoped, repositories are `@Singleton`, ViewModels are `@HiltViewModel`.
