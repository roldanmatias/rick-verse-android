import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Shared Kotlin/Java compiler settings for JVM and Android modules.
 * Compiles with whatever JDK runs Gradle but targets Java 17 bytecode (`--release 17`),
 * so no extra toolchain download is needed locally or on CI.
 */
internal fun Project.configureKotlinCompiler() {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            allWarningsAsErrors.set(true)
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn",
                "-Xjsr305=strict",
            )
        }
    }
}

/** Modules legitimately start with no tests (empty skeleton, pure-UI modules): don't fail on "no tests found". */
internal fun Project.configureTestTasks() {
    tasks.withType<Test>().configureEach {
        failOnNoDiscoveredTests.set(false)
    }
}

/** Pure-JVM modules only: Android modules get their Java level from `compileOptions` (AGP forbids `--release`). */
internal fun Project.configureJavaRelease() {
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(ProjectConfig.JVM_TARGET)
    }
}
