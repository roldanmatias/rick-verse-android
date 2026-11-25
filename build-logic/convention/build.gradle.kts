
plugins {
    `kotlin-dsl`
}

group = "com.roldanmatias.rickverse.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

// Plugins are compileOnly: the real versions come from the root build classpath
// (declared `apply false` in /build.gradle.kts), which keeps a single source of truth.
dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.spotless.gradlePlugin)
    compileOnly(libs.kover.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "rickverse.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "rickverse.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("kotlinLibrary") {
            id = "rickverse.kotlin.library"
            implementationClass = "KotlinLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "rickverse.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "rickverse.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("hilt") {
            id = "rickverse.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("quality") {
            id = "rickverse.quality"
            implementationClass = "QualityConventionPlugin"
        }
    }
}
