plugins {
    id("rickverse.kotlin.library")
}

dependencies {
    // Flow appears in repository interfaces, so coroutines are part of Domain's public API.
    api(libs.kotlinx.coroutines.core)
    // Only for @Inject on use cases and the @Rest/@GraphQl qualifiers. See ADR 0001.
    api(libs.javax.inject)

    testImplementation(projects.core.testing)
}
