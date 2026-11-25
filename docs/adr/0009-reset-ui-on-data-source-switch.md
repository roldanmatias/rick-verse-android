# 0009. Resetting UI state on data source switch: epoch-scoped ViewModelStore

Status: accepted

## Context
After switching REST <-> GraphQL, every screen must reload from the new source and the user starts from the first
screen. Options: (a) `key(dataSource) { ... }` and reset the back stack; (b) each ViewModel reacts with
`flatMapLatest` on the data source flow.

## Decision
A variant of (a). `key(dataSource)` rebuilds composition (new `NavController`, so a fresh back stack), **and** the
subtree is given a `ViewModelStoreOwner` owned by an Activity-scoped `DataSourceScopes` ViewModel, one store per
data source ("epoch"). After a switch the old epoch's store is cleared, destroying all of its ViewModels.

Why not plain `key`: ViewModels are scoped to a `ViewModelStoreOwner` (the Activity or a back stack entry), not to
composition. Navigation keeps entry ViewModels in a `NavControllerViewModel` held by the Activity, so after `key`
rebuilt the UI the old ViewModels (with the old source's data) would still be found. Why the holder is a ViewModel:
a plain `remember`ed store would be lost on rotation and every screen would reload.

## Consequences
* ViewModels stay unaware of the switch (no `flatMapLatest` in each of them); the rule lives in one place.
* Cost: a custom `ViewModelStoreOwner` supplied through `LocalViewModelStoreOwner`. It does not provide default
  factories or saved state: Hilt ViewModels get them from their `NavBackStackEntry`, which is what we use.
* The reactive alternative is better if a screen must preserve state across a switch (not a requirement here).
