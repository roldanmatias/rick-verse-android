// Root build: only declares plugins on the classpath once (so convention plugins and modules share one
// version of each) and aggregates Kover coverage across modules. No module configuration lives here.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.apollo) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)
}

dependencies {
    // Aggregate coverage from every module that applies the convention plugins.
    // :benchmark is a com.android.test module (no Kover): it measures the app, it is not measured.
    subprojects.filter { it.buildFile.exists() && it.path != ":benchmark" }.forEach { kover(it) }
    // Dokka documents production modules only (not test-only modules or the test fixtures).
    val notDocumented = setOf(":benchmark", ":architecture-tests", ":contract-tests", ":core:testing")
    subprojects
        .filter { it.buildFile.exists() && it.path !in notDocumented && it.path != ":app" }
        .forEach { dokka(it) }
}

dokka {
    moduleName.set("RickVerse")
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    "*.BuildConfig",
                    "*.R", "*.R\$*",
                    "*_Factory", "*_Factory\$*", "*_HiltModules*", "*_MembersInjector",
                    "*Hilt_*", "dagger.hilt.*", "hilt_aggregated_deps.*",
                    "*ComposableSingletons*",
                    "*.MainActivity*", "*.RickVerseApp*",
                )
                // Test fixtures are not production code; thin wrappers around the platform add no signal.
                packages("com.roldanmatias.rickverse.core.testing", "*.ui.theme")
                // Composition code and wiring: covered by UI/screenshot/e2e tests, not by line coverage (the guide's
                // advice: do not chase coverage on composables). Logic lives in ViewModels, mappers and use cases.
                packages(
                    "com.roldanmatias.rickverse.feature.*.ui",
                    "com.roldanmatias.rickverse.core.designsystem.*",
                    "com.roldanmatias.rickverse.navigation",
                    "*.di",
                    // Apollo codegen output is not our code.
                    "com.roldanmatias.rickverse.data.graphql.generated*",
                )
                classes("*.core.ui.*Kt", "*.core.ui.DataSourceChip*", "*.DataSourceScopesViewModel*", "*.Route*")
                annotatedBy("androidx.compose.runtime.Composable")
                classes("*.DefaultDispatcherProvider")
                annotatedBy("androidx.compose.ui.tooling.preview.Preview")
            }
        }
        verify {
            rule("Minimum line coverage") {
                minBound(80)
            }
        }
    }
}
