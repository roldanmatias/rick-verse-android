import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension

/** `rickverse.kotlin.library`: pure JVM module. Cannot see `android.*`: the compiler enforces the rule. */
class KotlinLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        pluginManager.apply("rickverse.quality")

        configureKotlinCompiler()
        configureTestTasks()
        configureJavaRelease()
        extensions.configure<KotlinBaseExtension> { explicitApi() }
        dependencies {
            add("testImplementation", libs.bundle("unit-test"))
        }
    }
}
