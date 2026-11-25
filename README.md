# RickVerse-Android

A Jetpack Compose app that talks to **the same backend through two different APIs**, **REST** (Retrofit + OkHttp + kotlinx.serialization) and **GraphQL** (Apollo Kotlin). Both sit behind one Domain interface, so the app can switch between them at runtime.

Built with Clean Architecture + MVVM, a multi-module Gradle build, convention plugins, and Hilt. The full build guide is in [`docs/GUIDE.md`](docs/GUIDE.md); design decisions are in [`docs/adr`](docs/adr).

> Work in progress. This README grows with each phase (see the guide, "Phase 13").

## Everyday commands

CI runs exactly these, so what passes locally passes in CI.

| Command | Does |
|---|---|
| `./gradlew assembleDebug` | Build the app |
| `./gradlew test` | All JVM unit tests |
| `./gradlew detekt spotlessCheck lint` | Static analysis, formatting, Android Lint |
| `./gradlew spotlessApply` | Auto-format with ktlint |
| `./gradlew koverVerify` | Coverage gate |
| `./gradlew :core:domain:dependencies --configuration runtimeClasspath` | **Proof of the dependency rule**: only Kotlin stdlib, coroutines and `javax.inject` |

Compose compiler reports: `./gradlew assembleRelease -Prickverse.composeMetrics=true` (output in `build/compose_compiler`).

## Build setup

* **Version catalog**: `gradle/libs.versions.toml` is the single place for versions, library aliases and bundles.
* **Convention plugins** (`build-logic/convention`, an included build): every module's `build.gradle.kts` is a few lines.

| Plugin id | Applies |
|---|---|
| `rickverse.android.application` | AGP application, SDK levels, Java 17, R8 for release, quality |
| `rickverse.android.library` | AGP library, Java 17, `explicitApi()`, quality |
| `rickverse.kotlin.library` | Pure JVM module (`:core:domain`, `:core:common`, `:core:network`, `:core:testing`) |
| `rickverse.android.compose` | Compose compiler plugin, BOM, tooling |
| `rickverse.android.feature` | Library + Compose + Hilt + lifecycle + Domain/UI dependencies + test dependencies |
| `rickverse.hilt` | Hilt + KSP |
| `rickverse.quality` | detekt, Spotless (ktlint), Kover |

Requires a JDK 21+ to run Gradle (Android Studio's bundled JDK works); bytecode targets Java 17.
