# 0002. `DomainResult` instead of exceptions or `kotlin.Result`

Status: accepted

## Context
Repositories can fail in ways the UI must react to (offline, not found, server error). Kotlin has no typed throws
(unlike Swift's `throws(DomainError)`), so the failure type has to live somewhere else. Options:

1. `DomainResult<T>`: a sealed `Success` / `Failure(DomainError)` type.
2. `kotlin.Result<T>`: cannot carry a typed error, so callers `catch` a generic `Throwable`.
3. Throw domain exceptions: failure is invisible in the function signature.

## Decision
Use **`DomainResult<T>`** with a sealed `DomainError`. The compiler forces callers to handle both branches with an
exhaustive `when`. Cancellation is not part of the model: `runCatchingDomain` **always rethrows
`CancellationException`**, so a cancelled request is never converted into an error state.

## Consequences
* Failure is part of every repository signature, and the error taxonomy is closed and testable.
* Slight ceremony: `map`/`fold` helpers and wrapping at the data layer boundary.
* Exceptions still exist inside data modules; they are mapped to `DomainError` in exactly one place per module
  (`ErrorMapper`), so no infrastructure type (`HttpException`, `ApolloException`) leaks upward.
* `try/catch (e: Exception)` around a suspend call is the classic way to break cancellation; detekt's
  `TooGenericExceptionCaught` is enabled so any such catch must be justified with a suppression.
