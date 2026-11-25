plugins {
    id("rickverse.android.feature")
    alias(libs.plugins.roborazzi)
}

dependencies {
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit)
}
