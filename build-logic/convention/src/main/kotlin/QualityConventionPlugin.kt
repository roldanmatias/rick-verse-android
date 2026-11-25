import com.diffplug.gradle.spotless.SpotlessExtension
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

/** `rickverse.quality`: detekt, Spotless (ktlint) and Kover on every module. */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("dev.detekt")
        pluginManager.apply("com.diffplug.spotless")
        pluginManager.apply("org.jetbrains.kotlinx.kover")
        pluginManager.apply("org.jetbrains.dokka") // documents public APIs; aggregated at the root

        extensions.configure<DetektExtension> {
            buildUponDefaultConfig.set(true)
            parallel.set(true)
            config.setFrom(rootProject.file("config/detekt/detekt.yml"))
            source.setFrom(files("src/main/kotlin", "src/test/kotlin", "src/androidTest/kotlin"))
        }

        tasks.withType<Detekt>().configureEach {
            jvmTarget.set(ProjectConfig.JVM_TARGET.toString())
            reports {
                sarif.required.set(true)
                html.required.set(true)
            }
        }

        extensions.configure<SpotlessExtension> {
            kotlin {
                target("src/**/*.kt")
                targetExclude("**/build/**")
                ktlint(libs.versionOf("ktlint")).editorConfigOverride(KTLINT_RULES)
            }
            kotlinGradle {
                target("*.gradle.kts")
                ktlint(libs.versionOf("ktlint")).editorConfigOverride(KTLINT_RULES)
            }
        }
        Unit
    }
}

/**
 * ktlint rules, mirrored in `.editorconfig` for the IDE. Set here as well so the Gradle build never depends on
 * ktlint discovering that file. `intellij_idea` keeps the idiomatic `class X @Inject constructor(...)` header.
 */
private val KTLINT_RULES = mapOf(
    "ktlint_code_style" to "intellij_idea",
    "max_line_length" to "120",
    "ij_kotlin_allow_trailing_comma" to "true",
    "ij_kotlin_allow_trailing_comma_on_call_site" to "true",
    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
    "ktlint_standard_filename" to "disabled",
    "ktlint_standard_function-signature" to "disabled",
)
