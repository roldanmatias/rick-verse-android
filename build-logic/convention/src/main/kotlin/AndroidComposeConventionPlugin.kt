import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/** `rickverse.android.compose`: Compose compiler plugin + BOM + tooling. Works on app and library modules. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        pluginManager.withPlugin("com.android.application") {
            extensions.configure<ApplicationExtension> { buildFeatures.compose = true }
        }
        pluginManager.withPlugin("com.android.library") {
            extensions.configure<LibraryExtension> { buildFeatures.compose = true }
        }

        dependencies {
            val bom = libs.library("androidx-compose-bom")
            add("implementation", platform(bom))
            add("androidTestImplementation", platform(bom))
            add("implementation", libs.bundle("compose-ui"))
            add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
            add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
            add("androidTestImplementation", libs.library("androidx-compose-ui-test-junit4"))
        }

        // Opt-in Compose compiler reports: ./gradlew assembleRelease -Prickverse.composeMetrics=true
        extensions.configure<ComposeCompilerGradlePluginExtension> {
            if (providers.gradleProperty("rickverse.composeMetrics").orNull == "true") {
                val out = layout.buildDirectory.dir("compose_compiler")
                reportsDestination.set(out)
                metricsDestination.set(out)
            }
            stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("config/compose/stability.conf"))
        }
    }
}
