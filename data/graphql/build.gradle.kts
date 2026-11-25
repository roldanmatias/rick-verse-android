plugins {
    id("rickverse.android.library")
    id("rickverse.hilt")
    alias(libs.plugins.apollo)
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.common)
    implementation(projects.core.network)
    implementation(libs.apollo.runtime)
    implementation(libs.apollo.cache)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(projects.core.testing)
    testImplementation(libs.okhttp.mockwebserver)
}

apollo {
    service("rickandmorty") {
        packageName.set("com.roldanmatias.rickverse.data.graphql.generated")
        // ADR 0005: generated types are `internal`, so Apollo never leaks out of this module.
        generateAsInternal.set(true)
        // ID stays a String here; the mappers convert to Int (a custom scalar mapping would hide that decision).
        // Fail the build if an operation selects something the schema does not have.
        failOnWarnings.set(true)
        // `./gradlew :data:graphql:downloadApolloSchema` refreshes the committed schema from the live API.
        introspection {
            endpointUrl.set("https://rickandmortyapi.com/graphql")
            schemaFile.set(file("src/main/graphql/schema.graphqls"))
        }
    }
}
