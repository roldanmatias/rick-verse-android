package com.roldanmatias.rickverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.roldanmatias.rickverse.core.designsystem.theme.RickVerseTheme
import com.roldanmatias.rickverse.core.ui.LocalDataSourceLabel
import com.roldanmatias.rickverse.navigation.RootNavigation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RickVerseTheme {
                // Becomes reactive to the Settings data source in Phase 7.
                CompositionLocalProvider(LocalDataSourceLabel provides "REST") {
                    RootNavigation()
                }
            }
        }
    }
}
