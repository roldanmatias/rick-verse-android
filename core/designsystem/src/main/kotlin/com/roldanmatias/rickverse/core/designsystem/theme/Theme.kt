package com.roldanmatias.rickverse.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(primary = PortalGreen, secondary = SpaceBlue, tertiary = Slime)
private val DarkColors = darkColorScheme(primary = PortalGreenLight, secondary = SpaceBlueLight, tertiary = SlimeLight)

/** Colours with a meaning beyond Material's scheme. */
@Immutable
public data class StatusColors(val positive: Color, val negative: Color, val neutral: Color)

private val LightStatusColors = StatusColors(StatusAlive, StatusDead, StatusNeutral)
private val DarkStatusColors = StatusColors(StatusAliveDark, StatusDeadDark, StatusNeutralDark)

private val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

/** Access the semantic [StatusColors] of the current theme. */
public object RickVerseTheme {
    public val statusColors: StatusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStatusColors.current
}

/**
 * App theme: Material 3, dark/light, and **dynamic colour** (Material You) on Android 12+ with a fixed
 * fallback palette below.
 */
@Composable
public fun RickVerseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> {
                DarkColors
            }

            else -> {
                LightColors
            }
        }
    CompositionLocalProvider(LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors) {
        MaterialTheme(colorScheme = colorScheme, typography = RickVerseTypography, content = content)
    }
}
