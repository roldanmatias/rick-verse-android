import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension

/** `rickverse.android.library`: AGP library + Kotlin, Java 17, `explicitApi()`, unit-test dependencies. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("rickverse.quality")

        configureKotlinCompiler()
        configureTestTasks()
        extensions.configure<KotlinBaseExtension> { explicitApi() }
        extensions.configure<LibraryExtension> {
            configureAndroidCommon(defaultNamespace())
        }
        dependencies {
            add("testImplementation", libs.bundle("unit-test"))
        }
    }
}
