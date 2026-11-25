# 0001. Multi-module Clean Architecture with convention plugins

Status: accepted

## Context
The app must demonstrate Clean Architecture where the dependency rule is enforced by the compiler instead of by
discipline, with two interchangeable data sources (REST and GraphQL) behind one interface. A single-module app can only
rely on package conventions and code review.

## Decision
* One Gradle module per layer (`:core:domain`, `:data:rest`, `:data:graphql`, `:feature:*`, `:app`).
* `:core:domain` is a **pure Kotlin/JVM** module, so it cannot import `android.*`, Retrofit, OkHttp or Apollo.
* Only `:app` knows every module, because Hilt assembles the graph there.
* Build logic is shared through **convention plugins** (`build-logic`, an included build) and a **version catalog**.
* Domain accepts a tiny `javax.inject` dependency for `@Inject` on use cases and for `@Rest`/`@GraphQl` qualifiers.
  The alternative (providing use cases from a Hilt module in `:app`) moves boilerplate to the composition root without
  adding any real isolation, because `javax.inject` is a pure-Java annotation package with no behaviour.

## Consequences
* Illegal dependencies fail the build (the module graph is the first architecture test; Konsist and a grep script add more).
* Parallel and incremental builds scale with team size.
* Cost: more configuration, which convention plugins reduce to a few lines per module.
