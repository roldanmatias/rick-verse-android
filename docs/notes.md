# Notes

Personal interview notes, written in my own words after each phase. See `GUIDE.md` for the question bank.

## Phase 0: build setup

**Why convention plugins and a version catalog?**
SDK levels, Kotlin options and lint rules live in exactly one place (`build-logic`). Modules stay consistent and each
`build.gradle.kts` is a handful of lines, so adding a module is cheap. Versions live in `libs.versions.toml`, which
gives type-safe accessors and one file for Dependabot to update.

**Configuration cache: what breaks it?**
Reading `Project` or other build-time objects inside task actions, reading environment variables/files without
declaring them as inputs, and plugins that are not cache-compatible. All convention plugins here only touch the
`Project` at configuration time, and use `providers.gradleProperty` for opt-in flags.

**`api` vs `implementation`**: default to `implementation` so transitive types do not leak. `:core:domain` uses `api`
for coroutines (Flow is in its public interfaces) and `javax.inject` (qualifiers), on purpose.

**Environment findings (record of what differed from the guide)**
* AGP 9 has built-in Kotlin, so `org.jetbrains.kotlin.android` is not applied. The AGP 9 DSL is non-generic
  (`CommonExtension`) and has no lambda blocks, so convention plugins use property access (`lint.apply { }`).
* Compose BOM 2026.11 needs `compileSdk` 37.1 (`release(37) { minorApiLevel = 1 }`).
* detekt 1.23 cannot parse the JDK 25 version string, so the build uses detekt 2.0 (`dev.detekt`), whose config keys differ.
* AGP forbids `javac --release`; pure JVM modules use it, Android modules use `compileOptions`.
