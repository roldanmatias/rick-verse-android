package com.roldanmatias.rickverse.feature.episodes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roldanmatias.rickverse.core.designsystem.component.SkeletonList
import com.roldanmatias.rickverse.core.ui.ErrorState
import com.roldanmatias.rickverse.core.ui.PaginationFooter
import com.roldanmatias.rickverse.core.ui.RickVerseTopBar
import com.roldanmatias.rickverse.feature.episodes.R
import com.roldanmatias.rickverse.feature.episodes.presentation.EpisodeListUiState
import com.roldanmatias.rickverse.feature.episodes.presentation.EpisodeListViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

private const val PREFETCH_DISTANCE = 5

@Composable
public fun EpisodeListRoute(onEpisodeClick: (Int) -> Unit, modifier: Modifier = Modifier) {
    val viewModel: EpisodeListViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    EpisodeListScreen(state, viewModel::retry, viewModel::loadMore, onEpisodeClick, modifier)
}

@Composable
internal fun EpisodeListScreen(
    state: EpisodeListUiState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onEpisodeClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier, topBar = { RickVerseTopBar(stringResource(R.string.episodes_title)) }) { padding ->
        val contentModifier = Modifier.padding(padding).fillMaxSize()
        when {
            state.loading -> SkeletonList(contentModifier)
            state.error != null -> ErrorState(state.error, onRetry, contentModifier)
            else -> EpisodeList(state, onRetry = onLoadMore, onLoadMore = onLoadMore, onEpisodeClick, contentModifier)
        }
    }
}

@Composable
private fun EpisodeList(
    state: EpisodeListUiState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onEpisodeClick: (Int) -> Unit,
    modifier: Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState, state.items.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { last -> if (last >= state.items.size - PREFETCH_DISTANCE) onLoadMore() }
    }
    LazyColumn(modifier, state = listState) {
        items(state.items, key = { it.id }) { episode ->
            val description = stringResource(R.string.episode_row_description, episode.code, episode.name)
            Column(
                Modifier.fillMaxWidth()
                    .clickable { onEpisodeClick(episode.id) }
                    .padding(16.dp, 12.dp)
                    .semantics(mergeDescendants = true) { contentDescription = description },
            ) {
                Text(episode.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    episode.code,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { PaginationFooter(state.pagination, stringResource(R.string.episodes_end), onRetry) }
    }
}
