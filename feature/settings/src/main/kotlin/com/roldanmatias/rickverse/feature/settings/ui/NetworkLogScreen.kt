package com.roldanmatias.rickverse.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roldanmatias.rickverse.core.network.NetworkEvent
import com.roldanmatias.rickverse.core.network.NetworkSource
import com.roldanmatias.rickverse.core.ui.RickVerseTopBar
import com.roldanmatias.rickverse.feature.settings.R
import com.roldanmatias.rickverse.feature.settings.presentation.NetworkLogUiState
import com.roldanmatias.rickverse.feature.settings.presentation.NetworkLogViewModel

@Composable
public fun NetworkLogRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: NetworkLogViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NetworkLogScreen(state, viewModel::onFilterSelected, viewModel::onClear, onBack, modifier)
}

@Composable
internal fun NetworkLogScreen(
    state: NetworkLogUiState,
    onFilter: (NetworkSource?) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RickVerseTopBar(stringResource(R.string.network_log_title), onBack = onBack) {
                TextButton(onClick = onClear) { Text(stringResource(R.string.network_log_clear)) }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterOption(null, state.filter, R.string.network_log_filter_all, onFilter)
                FilterOption(NetworkSource.Rest, state.filter, R.string.network_log_filter_rest, onFilter)
                FilterOption(NetworkSource.GraphQl, state.filter, R.string.network_log_filter_graphql, onFilter)
            }
            Text(
                stringResource(R.string.network_log_summary, state.totalRequests, state.cacheHits),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(16.dp, 8.dp),
            )
            if (state.events.isEmpty()) {
                Text(stringResource(R.string.network_log_empty), Modifier.padding(16.dp))
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.events, key = { it.id }) { event ->
                        EventRow(event)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterOption(
    option: NetworkSource?,
    selected: NetworkSource?,
    label: Int,
    onFilter: (NetworkSource?) -> Unit,
) {
    FilterChip(selected = option == selected, onClick = { onFilter(option) }, label = { Text(stringResource(label)) })
}

@Composable
private fun EventRow(event: NetworkEvent) {
    val description = stringResource(R.string.network_log_row, event.label, event.status, event.durationMs)
    Column(Modifier.fillMaxWidth().padding(16.dp, 8.dp).semantics { contentDescription = description }) {
        Text(event.label, style = MaterialTheme.typography.bodyMedium)
        val detail = if (event.servedFromCache) {
            stringResource(R.string.network_log_cache) + " · ${event.durationMs} ms"
        } else {
            "${event.source.name} · ${event.status} · ${event.durationMs} ms · ${event.bytes} B"
        }
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
