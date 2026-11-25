package com.roldanmatias.rickverse

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Launch -> list -> detail -> back, fully hermetic (fake repositories via `@TestInstallIn`).
 * Matchers are semantic (text, content description), which doubles as an accessibility check.
 * Run on an emulator with animations disabled: `./gradlew :app:connectedDebugAndroidTest`.
 */
@HiltAndroidTest
class CharacterFlowE2ETest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun inject() = hilt.inject()

    @Test
    fun listThenDetailThenBack() {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTextCount("Rick Sanchez") > 0
        }
        compose.onNodeWithText("Morty Smith").assertIsDisplayed()

        compose.onNodeWithText("Rick Sanchez").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodesWithTextCount("Origin: Earth (C-137)") > 0 }
        compose.onNodeWithText("Origin: Earth (C-137)").assertIsDisplayed()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Morty Smith").assertIsDisplayed()
    }

    @Test
    fun selectingGraphQlInSettingsKeepsTheScreenUsable() {
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("GraphQL").performClick()

        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodesWithTextCount("Settings") > 0 }
        compose.onNodeWithText("GraphQL cache policy").assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.onAllNodesWithTextCount(text: String) =
        onAllNodes(androidx.compose.ui.test.hasText(text)).fetchSemanticsNodes().size
}
