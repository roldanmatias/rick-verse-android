plugins {
    id("rickverse.android.application")
    id("rickverse.android.compose")
    id("rickverse.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    defaultConfig {
        applicationId = "com.roldanmatias.rickverse"
        versionCode = 1
        versionName = "0.1.0"
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

    testImplementation(projects.core.testing)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
