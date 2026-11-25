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
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.kover)
}

dependencies {
    // Aggregate coverage from every module that applies the convention plugins.
    subprojects.filter { it.buildFile.exists() }.forEach { kover(it) }
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
                classes("*.DefaultDispatcherProvider")
                annotatedBy("androidx.compose.ui.tooling.preview.Preview")
            }
        }
        verify {
            rule("Minimum line coverage") {
                // Start low, ratchet to 80% as the test suite grows (Phase 9).
                minBound(60)
            }
        }
    }
}
