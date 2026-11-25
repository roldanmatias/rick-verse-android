plugins {
    id("rickverse.kotlin.library")
}

dependencies {
    testImplementation(projects.core.network)
    testImplementation(libs.okhttp)
    testImplementation(libs.kotlinx.serialization.json)
}

// These tests talk to the REAL API. They are skipped (JUnit Assume) in a normal `./gradlew test`, and these tasks flip
// the switch. Deliberately not wired into `check`: a third-party outage must never break a pull request.
fun registerLiveTask(name: String, description: String, envVar: String, pattern: String) {
    tasks.register<Test>(name) {
        this.description = description
        group = "verification"
        val tests = sourceSets.getByName("test")
        testClassesDirs = tests.output.classesDirs
        classpath = tests.runtimeClasspath
        environment(envVar, "1")
        filter.includeTestsMatching(pattern)
        outputs.upToDateWhen { false } // always hit the network
    }
}

registerLiveTask("contractTest", "Checks live API assumptions.", "RUN_CONTRACT_TESTS", "*ContractTest")
registerLiveTask(
    "benchmarkTest",
    "Counts requests per strategy against the live API.",
    "RUN_BENCHMARKS",
    "*BenchmarkTests",
)
