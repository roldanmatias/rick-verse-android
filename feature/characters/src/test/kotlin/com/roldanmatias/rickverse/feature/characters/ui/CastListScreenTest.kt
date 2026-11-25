package com.roldanmatias.rickverse.feature.characters.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.roldanmatias.rickverse.core.designsystem.theme.RickVerseTheme
import com.roldanmatias.rickverse.core.domain.error.DomainError
import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.core.testing.TestData
import com.roldanmatias.rickverse.core.ui.pagination.PaginationState
import com.roldanmatias.rickverse.feature.characters.presentation.CastListIntent
import com.roldanmatias.rickverse.feature.characters.presentation.CastListUiState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Runs on the JVM through Robolectric: no emulator, no Hilt, no ViewModel. Each state is rendered directly. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class CastListScreenTest {
    private val intents = mutableListOf<CastListIntent>()
    private val clicked = mutableListOf<Int>()

    private fun screenTest(state: CastListUiState, block: ComposeUiTest.() -> Unit) =
        runComposeUiTest {
            setContent {
                RickVerseTheme(dynamicColor = false) {
                    CastListScreen(state = state, onIntent = { intents += it }, onCastClick = { clicked += it })
                }
            }
            block()
        }

    private val loaded =
        CastListUiState(
            phase = CastListUiState.Phase.Loaded,
            items =
            listOf(
                TestData.castMember(id = 1, name = "Rick Sanchez", species = "Human"),
                TestData.castMember(2, "Morty Smith", CastMember.Status.Dead, "Human"),
            ),
        )

    @Test
    fun `content renders one merged accessibility node per row`() =
        screenTest(loaded) {
            onNodeWithContentDescription("Rick Sanchez, Alive, Human").assertIsDisplayed()
            onNodeWithContentDescription("Morty Smith, Dead, Human").assertIsDisplayed()
        }

    @Test
    fun `clicking a row reports its id`() =
        screenTest(loaded) {
            onNodeWithContentDescription("Morty Smith, Dead, Human").performClick()

            assertEquals(listOf(2), clicked)
        }

    @Test
    fun `error state shows the message and Retry emits the intent`() =
        screenTest(CastListUiState(phase = CastListUiState.Phase.Failed(DomainError.Offline))) {
            onNodeWithText("You're offline. Check your connection and try again.").assertIsDisplayed()
            onNodeWithText("Retry").performClick()

            assertEquals(listOf<CastListIntent>(CastListIntent.Retry), intents)
        }

    @Test
    fun `empty state shows the query`() =
        screenTest(CastListUiState(phase = CastListUiState.Phase.Empty, query = "zzz")) {
            onNodeWithText("No characters found").assertIsDisplayed()
            onNodeWithText("Nothing matches \"zzz\". Try a different search or filter.").assertIsDisplayed()
        }

    @Test
    fun `typing in the search field emits QueryChanged`() =
        screenTest(loaded) {
            onNodeWithText("Search characters").performTextInput("rick")

            assertEquals(CastListIntent.QueryChanged("rick"), intents.last())
        }

    @Test
    fun `selecting a filter chip emits StatusSelected`() =
        screenTest(loaded) {
            // "Dead" also appears as a row badge, so target the chip by its role: no test tags needed.
            onNode(hasText("Dead") and isSelectable()).performClick()

            assertEquals(CastListIntent.StatusSelected(CastMember.Status.Dead), intents.single())
        }

    @Test
    fun `pagination footer failure shows Retry that emits RetryNextPage`() =
        screenTest(loaded.copy(pagination = PaginationState.Failed(DomainError.Timeout))) {
            onNodeWithText("Couldn't load more").assertIsDisplayed()
            onNodeWithText("Retry").performClick()

            assertEquals(CastListIntent.RetryNextPage, intents.last())
        }

    @Test
    fun `end of list shows the exhausted footer`() =
        screenTest(loaded.copy(pagination = PaginationState.Exhausted)) {
            onNodeWithText("That's everyone 👽").assertIsDisplayed()
        }
}
