package com.roldanmatias.rickverse.feature.characters.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.roldanmatias.rickverse.core.designsystem.theme.RickVerseTheme
import com.roldanmatias.rickverse.core.domain.error.DomainError
import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.core.testing.TestData
import com.roldanmatias.rickverse.feature.characters.presentation.CastListUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Visual regression of the key states in light, dark and 2.0x font scale.
 *
 * Normal `./gradlew test` renders but compares nothing. Use:
 * * `./gradlew :feature:characters:recordRoborazziDebug` to (re)write the golden PNGs in `src/test/screenshots`
 * * `./gradlew :feature:characters:verifyRoborazziDebug` to fail on any pixel difference
 * Goldens are rendered with Robolectric's native graphics, so record and verify on the same OS (CI: Linux).
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h720dp-xhdpi")
class CastListScreenshotTest {
    private val loaded =
        CastListUiState(
            phase = CastListUiState.Phase.Loaded,
            items =
            listOf(
                TestData.castMember(id = 1, name = "Rick Sanchez", species = "Human"),
                TestData.castMember(2, "Morty Smith", CastMember.Status.Alive, "Human"),
                TestData.castMember(3, "Summer Smith", CastMember.Status.Dead, "A very long species name to wrap"),
            ),
        )

    private fun shot(name: String, dark: Boolean = false, fontScale: Float = 1f, state: CastListUiState) =
        runComposeUiTest {
            setContent {
                Themed(dark, fontScale) { CastListScreen(state = state, onIntent = {}, onCastClick = {}) }
            }
            onRoot().captureRoboImage("src/test/screenshots/$name.png")
        }

    @Composable
    private fun Themed(dark: Boolean, fontScale: Float, content: @Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            RickVerseTheme(darkTheme = dark, dynamicColor = false, content = content)
        }
    }

    @Test
    fun loadedLight() = shot("cast_loaded_light", state = loaded)

    @Test
    fun loadedDark() = shot("cast_loaded_dark", dark = true, state = loaded)

    @Test
    fun loadedLargeFont() = shot("cast_loaded_font2x", fontScale = 2f, state = loaded)

    @Test
    fun loadingSkeleton() = shot("cast_loading", state = CastListUiState())

    @Test
    fun errorOffline() =
        shot("cast_error", state = CastListUiState(phase = CastListUiState.Phase.Failed(DomainError.Offline)))
}
