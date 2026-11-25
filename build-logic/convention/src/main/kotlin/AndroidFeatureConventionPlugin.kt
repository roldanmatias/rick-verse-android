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
            add("implementation", libs.library("androidx-hilt-navigation-compose"))
            add("implementation", libs.library("kotlinx-coroutines-android"))

            add("testImplementation", project(":core:testing"))
            add("testImplementation", libs.library("mockk"))
            add("androidTestImplementation", libs.library("androidx-test-ext-junit"))
        }
    }
}
