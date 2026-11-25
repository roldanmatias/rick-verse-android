plugins {
    id("rickverse.kotlin.library")
}

dependencies {
    api(projects.core.domain)
    api(projects.core.common)
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
    api(libs.okhttp.mockwebserver)
}
