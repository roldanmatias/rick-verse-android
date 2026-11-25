# 0003. ViewModels free of Compose; architecture enforced by Konsist

Status: accepted

## Context
A feature module holds both its ViewModels and its Composables. Without a rule, a ViewModel will eventually import
`androidx.compose.*` (for a `Color`, a `ImageVector`, a `SnackbarHostState`) and become untestable on a plain JVM.

## Decision
* Each feature has two packages: `presentation/` (ViewModel, `UiState`, `Intent`) and `ui/` (Compose).
* `presentation` may import only `androidx.lifecycle`, coroutines, Domain, and plain-Kotlin shared types
  (for example `PaginationState` from `:core:ui`). Never `androidx.compose.*` or `android.*`.
* UI is stateless below the screen: `Screen(state, onIntent)`. Only `*Route` composables see Hilt and the ViewModel.
* A Konsist test (Phase 9) and `scripts/check-architecture.sh` fail the build when a `..presentation..` file imports Compose.

## Consequences
* ViewModels are tested with plain JUnit, virtual time and fakes: no Robolectric, no emulator.
* Screens render from a hand-built `UiState`, so previews and UI tests need no Hilt and no ViewModel.
* One-off effects (snackbars) are modelled as **state** the UI derives, not as a `SharedFlow` that can be lost on rotation.
