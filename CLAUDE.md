# RickVerse-Android: rules for AI assistants

Rick and Morty browser. Kotlin, Compose, Hilt, Retrofit **and** Apollo behind one repository interface.
Why it is built this way: `docs/adr/`. Interview notes: `docs/notes.md`. Open items: `docs/DEVICE-CHECKLIST.md`.

## Modules and the dependency rule
`:app` (composition root) -> `:feature:{characters,episodes,settings}` -> `:core:{domain,ui,designsystem,...}`;
`:data:{rest,graphql,local}` implement Domain interfaces. `:core:domain` is **pure Kotlin/JVM**.
- Domain imports no `android`, `retrofit2`, `okhttp3`, `com.apollographql`.
- `*/presentation/` (ViewModels) never imports `androidx.compose` or `android.`.
- Only `:data:graphql` imports Apollo; Apollo types stay `internal`. Features never import `:data:*`.
- Only `:app` sees every module. Backends are chosen by qualifiers `@Rest`, `@GraphQl`, `@Remote`.
`scripts/check-architecture.sh` and `:architecture-tests` (Konsist) enforce this. Do not weaken them.

## Commands (run before saying "done")
Gradle needs JDK 25: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` on this Mac.
- Everything: `./gradlew spotlessApply assembleDebug test detekt spotlessCheck lint koverVerify --continue`
- One module: `./gradlew :feature:characters:testDebugUnitTest`
- GraphQL codegen: `./gradlew :data:graphql:generateApolloSources`
- Live API checks (never in PRs): `./gradlew :contract-tests:contractTest`
- Architecture: `scripts/check-architecture.sh`; screenshots: `recordRoborazziDebug` / `verifyRoborazziDebug`

## Conventions
- Errors are values: repositories return `DomainResult<T>` / `DomainError` (ADR 0002), not exceptions.
- **Never swallow `CancellationException`** (`runCatchingDomain` rethrows it).
- Inject `DispatcherProvider`; never hardcode `Dispatchers.IO` in production code.
- Tests: fakes over mocks, Turbine/virtual time, MockWebServer with real fixtures. Test names are sentences.
- State: `StateFlow` + sealed UI state; one-off intents in, state out. Keys and `contentType` in lazy lists.
- Strings in resources with an `values-es` translation; keep Lint (warnings as errors) clean.
- Conventional Commits (`feat(rest): ...`); one logical change per commit; PR title follows the same format.
- KDoc on every public Domain type and repository interface.

## Never
- Edit generated code (`build/`, Apollo output), `schema.graphqls` by hand, or committed Room `schemas/*.json`
  (regenerate them with the build).
- Commit secrets, keystores, `local.properties`. Never put an API key in the app.
- Lower the Kover gate (80%), add suppressions to silence detekt/Lint, or disable/skip a test to get green.
- Merge, approve your own PR, or touch signing/release configuration without being asked.
- Invent numbers. Benchmarks are only quoted from a run recorded in `docs/benchmark.md`.

## Fixtures and contracts
Real captured responses live in each module's `src/test/resources`; refresh with `scripts/refresh-fixtures.sh`, then
run the module tests and `contractTest`. Quirks to remember: REST returns 404 for an empty search; the batch episode
endpoint returns a lone object (not an array) for a single id.

## Known gaps (do not claim otherwise)
Nothing has run on a device/emulator; CI workflows have not run on GitHub yet. See the two checklists in `docs/`.
