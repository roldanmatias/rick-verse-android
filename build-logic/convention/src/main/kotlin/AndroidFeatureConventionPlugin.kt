import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * `rickverse.android.feature`: library + Compose + Hilt + lifecycle + Domain/UI dependencies + test dependencies.
 * A feature module's build file is then just `plugins { id("rickverse.android.feature") }`.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("rickverse.android.library")
        pluginManager.apply("rickverse.android.compose")
        pluginManager.apply("rickverse.hilt")

        dependencies {
            add("implementation", project(":core:domain"))
            add("implementation", project(":core:ui"))
            add("implementation", project(":core:designsystem"))
            add("implementation", libs.bundle("lifecycle-compose"))
            add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("kotlinx-coroutines-android"))
            add("implementation", libs.library("androidx-lifecycle-viewmodel-savedstate"))
            add("implementation", libs.library("coil-compose"))

            add("testImplementation", project(":core:testing"))
            add("testImplementation", libs.library("mockk"))
            // Compose UI tests run on the JVM through Robolectric: no emulator, no Hilt, no ViewModel.
            add("testImplementation", libs.library("robolectric"))
            add("testImplementation", libs.library("androidx-compose-ui-test-junit4"))
            add("testImplementation", platform(libs.library("androidx-compose-bom")))
            add("testImplementation", libs.library("androidx-test-ext-junit"))
            add("androidTestImplementation", libs.library("androidx-test-ext-junit"))
        }
    }
}
