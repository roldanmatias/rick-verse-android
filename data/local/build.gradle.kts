plugins {
    id("rickverse.android.library")
    id("rickverse.hilt")
    alias(libs.plugins.room)
}

room {
    // Exported schemas are committed: they are what migration tests run against.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(projects.core.domain)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(projects.core.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.ext.junit)
}

android {
    sourceSets.getByName("test").assets.directories.add("$projectDir/schemas")
}
