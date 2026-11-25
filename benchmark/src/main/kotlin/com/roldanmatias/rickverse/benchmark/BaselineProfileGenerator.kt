package com.roldanmatias.rickverse.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the critical user journey (cold start, then scrolling the cast list) so ART can precompile those paths.
 * Output is merged into the app by the Baseline Profile Gradle plugin: `./gradlew :app:generateBaselineProfile`.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() =
        rule.collect(packageName = TARGET_PACKAGE, includeInStartupProfile = true) {
            startActivityAndWait()
            waitForCastList()
            scrollCastList()
        }
}
