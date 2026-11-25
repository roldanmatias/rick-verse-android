plugins {
    id("rickverse.android.application")
    id("rickverse.android.compose")
    id("rickverse.hilt")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.baselineprofile)
}

android {
    androidResources {
        // Per-app language (Android 13+ system settings): the locale list is generated from the resource folders.
        generateLocaleConfig = true
    }
    defaultConfig {
        applicationId = "com.roldanmatias.rickverse"
        // CI release job passes these (version from the tag, code from the run number); local builds default.
        versionCode = providers.environmentVariable("VERSION_CODE").map(String::toInt).orElse(1).get()
        versionName = providers.environmentVariable("VERSION_NAME").orElse("0.1.0").get()
        testInstrumentationRunner = "com.roldanmatias.rickverse.HiltTestRunner"
    }

    // Signing exists only when the release workflow provides the keystore (decoded from an environment secret).
    // Passwords come from environment variables: never from files in the repo, never echoed to logs.
    val keystorePath = providers.environmentVariable("KEYSTORE_PATH").orNull
    if (keystorePath != null) {
        signingConfigs.create("release").apply {
            storeFile = file(keystorePath)
            storePassword = providers.environmentVariable("KEYSTORE_PASSWORD").get()
            keyAlias = providers.environmentVariable("KEY_ALIAS").get()
            keyPassword = providers.environmentVariable("KEY_PASSWORD").get()
        }
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("release")
    }
}

// The composition root is the only module that sees every other module: Hilt needs that to assemble the graph.
dependencies {
    implementation(projects.core.common)
    implementation(projects.core.domain)
    implementation(projects.core.network)
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
    implementation(projects.core.preferences)
    implementation(projects.data.rest)
    implementation(projects.data.graphql)
    implementation(projects.data.local)
    implementation(projects.feature.characters)
    implementation(projects.feature.episodes)
    implementation(projects.feature.settings)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.lifecycle.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.androidx.profileinstaller)
    baselineProfile(projects.benchmark)
    // Debug-only leak detection: installs itself, ships nothing in release.
    debugImplementation(libs.leakcanary.android)

    testImplementation(projects.core.testing)

    androidTestImplementation(projects.core.testing)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
