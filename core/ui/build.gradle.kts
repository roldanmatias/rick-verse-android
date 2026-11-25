plugins {
    id("rickverse.android.library")
    id("rickverse.android.compose")
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.designsystem)
    implementation(libs.bundles.lifecycle.compose)

    testImplementation(projects.core.testing)
}
