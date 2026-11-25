plugins {
    id("rickverse.android.application")
    id("rickverse.android.compose")
}

android {
    defaultConfig {
        applicationId = "com.roldanmatias.rickverse"
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.lifecycle.compose)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
