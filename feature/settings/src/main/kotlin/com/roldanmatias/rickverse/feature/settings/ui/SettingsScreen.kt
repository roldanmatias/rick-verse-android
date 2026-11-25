package com.roldanmatias.rickverse.feature.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roldanmatias.rickverse.core.domain.model.CachePolicy
import com.roldanmatias.rickverse.core.domain.model.DataSource
import com.roldanmatias.rickverse.core.ui.RickVerseTopBar
import com.roldanmatias.rickverse.feature.settings.R
import com.roldanmatias.rickverse.feature.settings.presentation.SettingsUiState
import com.roldanmatias.rickverse.feature.settings.presentation.SettingsViewModel

@Composable
public fun SettingsRoute(onOpenNetworkLog: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onDataSource = viewModel::onDataSourceSelected,
        onCachePolicy = viewModel::onCachePolicySelected,
        onClearCache = viewModel::onClearCache,
        onCacheClearedShown = viewModel::onCacheClearedShown,
        onOpenNetworkLog = onOpenNetworkLog,
        modifier = modifier,
    )
}

@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    onDataSource: (DataSource) -> Unit,
    onCachePolicy: (CachePolicy) -> Unit,
    onClearCache: () -> Unit,
    onCacheClearedShown: () -> Unit,
    onOpenNetworkLog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val clearedMessage = stringResource(R.string.settings_cache_cleared)
    LaunchedEffect(state.cacheCleared) {
        if (state.cacheCleared) {
            snackbar.showSnackbar(clearedMessage)
            onCacheClearedShown()
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = { RickVerseTopBar(stringResource(R.string.settings_title)) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Section(stringResource(R.string.settings_data_source))
            Options(
                options = DataSource.entries,
                selected = state.dataSource,
                label = { stringResource(it.labelRes()) },
                onSelect = onDataSource,
            )
            Text(
                stringResource(R.string.settings_data_source_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Section(stringResource(R.string.settings_cache_policy), Modifier.padding(top = 24.dp))
            Options(
                options = CachePolicy.entries,
                selected = state.cachePolicy,
                label = { stringResource(it.labelRes()) },
                onSelect = onCachePolicy,
            )
            OutlinedButton(onClick = onClearCache, Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.settings_clear_cache))
            }
            Section(stringResource(R.string.settings_diagnostics), Modifier.padding(top = 24.dp))
            OutlinedButton(onClick = onOpenNetworkLog) { Text(stringResource(R.string.settings_network_log)) }
        }
    }
}

@Composable
private fun Section(title: String, modifier: Modifier = Modifier) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = modifier.semantics { heading() })
}

@Composable
private fun <T> Options(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    Column(Modifier.selectableGroup()) {
        options.forEach { option ->
            Row(
                Modifier.fillMaxWidth()
                    .selectable(selected = option == selected, role = Role.RadioButton, onClick = { onSelect(option) })
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = option == selected, onClick = null)
                Text(label(option), Modifier.padding(start = 16.dp))
            }
        }
    }
}

private fun DataSource.labelRes() =
    when (this) {
        DataSource.Rest -> R.string.settings_data_source_rest
        DataSource.GraphQl -> R.string.settings_data_source_graphql
    }

private fun CachePolicy.labelRes() =
    when (this) {
        CachePolicy.CacheFirst -> R.string.settings_cache_cache_first
        CachePolicy.NetworkFirst -> R.string.settings_cache_network_first
        CachePolicy.NetworkOnly -> R.string.settings_cache_network_only
        CachePolicy.CacheAndNetwork -> R.string.settings_cache_cache_and_network
    }
