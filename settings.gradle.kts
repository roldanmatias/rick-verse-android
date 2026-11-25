pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "RickVerse-Android"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")

// Core
include(":core:common")
include(":core:domain")
include(":core:network")
include(":core:preferences")
include(":core:designsystem")
include(":core:ui")
include(":core:testing")

// Data
include(":data:rest")
include(":data:graphql")
include(":architecture-tests")
include(":contract-tests")
include(":data:local")

// Features
include(":feature:characters")
include(":feature:episodes")
include(":feature:settings")

include(":benchmark")
