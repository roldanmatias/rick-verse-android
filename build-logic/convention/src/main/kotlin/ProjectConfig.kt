import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.kotlin.dsl.getByType

/** Single source of truth for SDK levels and JVM target. Change here, every module follows. */
internal object ProjectConfig {
    const val BASE_NAMESPACE = "com.roldanmatias.rickverse"
    const val MIN_SDK = 26
    const val COMPILE_SDK = 37
    const val COMPILE_SDK_MINOR = 1 // androidx.compose 1.13 requires API 37.1
    const val TARGET_SDK = 37
    const val JVM_TARGET = 17
}

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow { IllegalStateException("Missing library alias '$alias' in libs.versions.toml") }

internal fun VersionCatalog.bundle(alias: String) =
    findBundle(alias).orElseThrow { IllegalStateException("Missing bundle alias '$alias' in libs.versions.toml") }

internal fun VersionCatalog.versionOf(alias: String): String =
    findVersion(alias).orElseThrow { IllegalStateException("Missing version alias '$alias' in libs.versions.toml") }
        .requiredVersion

/** `:core:ui` -> `com.roldanmatias.rickverse.core.ui` */
internal fun Project.defaultNamespace(): String =
    ProjectConfig.BASE_NAMESPACE + path.replace(':', '.').replace('-', '_')
