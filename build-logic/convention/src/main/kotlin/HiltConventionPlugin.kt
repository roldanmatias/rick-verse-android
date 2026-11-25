import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** `rickverse.hilt`: Hilt Gradle plugin + KSP (not kapt: faster, Kotlin 2 / K2 friendly). */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("com.google.dagger.hilt.android")
        dependencies {
            add("implementation", libs.library("hilt-android"))
            add("ksp", libs.library("hilt-compiler"))
            add("kspTest", libs.library("hilt-compiler"))
        }
    }
}
