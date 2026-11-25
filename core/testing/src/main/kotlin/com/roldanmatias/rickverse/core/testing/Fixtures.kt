package com.roldanmatias.rickverse.core.testing

/**
 * Loads real captured API responses from `src/test/resources/fixtures/` of the calling module.
 * Fixtures are refreshed with `scripts/refresh-fixtures.sh` and verified nightly by the contract tests.
 */
public object Fixtures {
    public fun load(name: String): String {
        val path = "/fixtures/$name"
        val stream =
            Fixtures::class.java.getResourceAsStream(path)
                ?: error("Fixture not found on the test classpath: $path")
        return stream.bufferedReader().use { it.readText() }
    }
}
