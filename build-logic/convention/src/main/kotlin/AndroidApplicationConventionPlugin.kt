import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** `rickverse.android.application`: AGP application + Kotlin (built into AGP 9), SDK levels, R8, quality. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("rickverse.quality")

        configureKotlinCompiler()
        configureTestTasks()
        extensions.configure<ApplicationExtension> {
            configureAndroidCommon(ProjectConfig.BASE_NAMESPACE)
            defaultConfig {
                targetSdk = ProjectConfig.TARGET_SDK
            }
            buildTypes {
                release {
                    // AGP 9 optimization DSL: R8 full mode with resource shrinking.
                    optimization {
                        enable = true
                    }
                    isShrinkResources = true
                }
            }
        }
        dependencies {
            add("testImplementation", libs.bundle("unit-test"))
        }
    }
}
