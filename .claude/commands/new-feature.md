---
description: Scaffold a feature module (convention plugin, route, ViewModel, fakes, tests)
argument-hint: <name>
---
Create `:feature:$ARGUMENTS` by copying the structure of `:feature:episodes` (smallest existing feature):
`build.gradle.kts` using `rickverse.android.feature`, `presentation/` (ViewModel, UiState, Intent), `ui/` (screen +
previews for every state), `navigation/` route, strings with an `values-es` translation, and tests using fakes.
Register it in `settings.gradle.kts` and `:app`. Respect the dependency rule in CLAUDE.md, then run the full gate.
