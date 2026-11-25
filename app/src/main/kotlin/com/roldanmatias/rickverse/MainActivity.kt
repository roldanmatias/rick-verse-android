package com.roldanmatias.rickverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.roldanmatias.rickverse.core.designsystem.theme.RickVerseTheme
import com.roldanmatias.rickverse.core.domain.model.DataSource
import com.roldanmatias.rickverse.core.ui.LocalDataSourceLabel
import com.roldanmatias.rickverse.navigation.RootNavigation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val scopes: DataSourceScopesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RickVerseTheme {
                val source by scopes.dataSource.collectAsStateWithLifecycle()
                source?.let { current -> DataSourceEpoch(current) }
            }
        }
    }

    /**
     * Everything below is rebuilt when the data source changes: `key` resets composition state and the navigation
     * back stack, and the epoch-scoped ViewModelStoreOwner destroys the ViewModels (which `key` alone would not:
     * ViewModels outlive composition).
     */
    @androidx.compose.runtime.Composable
    private fun DataSourceEpoch(source: DataSource) {
        key(source) {
            LaunchedEffect(source) { scopes.clearExcept(source) }
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides scopes.ownerFor(source),
                LocalDataSourceLabel provides source.label(),
            ) {
                RootNavigation()
            }
        }
    }
}

private fun DataSource.label() =
    when (this) {
        DataSource.Rest -> "REST"
        DataSource.GraphQl -> "GraphQL"
    }
