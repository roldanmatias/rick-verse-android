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
                ktlint(libs.versionOf("ktlint"))
            }
            kotlinGradle {
                target("*.gradle.kts")
                ktlint(libs.versionOf("ktlint"))
            }
        }
        Unit
    }
}
