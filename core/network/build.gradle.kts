plugins {
    id("rickverse.kotlin.library")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.okhttp)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.retrofit) // only for HttpException in NetworkErrors

    testImplementation(projects.core.testing)
    testImplementation(libs.retrofit)
    testImplementation(libs.retrofit.converter.kotlinx.serialization)
    testImplementation(libs.okhttp.mockwebserver)
}
