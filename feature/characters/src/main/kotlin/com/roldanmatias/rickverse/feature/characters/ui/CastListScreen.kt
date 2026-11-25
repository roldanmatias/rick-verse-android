package com.roldanmatias.rickverse.feature.characters.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.roldanmatias.rickverse.core.designsystem.component.SkeletonList
import com.roldanmatias.rickverse.core.ui.EmptyState
import com.roldanmatias.rickverse.core.ui.ErrorState
import com.roldanmatias.rickverse.core.ui.PaginationFooter
import com.roldanmatias.rickverse.core.ui.RickVerseTopBar
import com.roldanmatias.rickverse.core.ui.toMessage
import com.roldanmatias.rickverse.feature.characters.R
import com.roldanmatias.rickverse.feature.characters.presentation.CastListIntent
import com.roldanmatias.rickverse.feature.characters.presentation.CastListUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import com.roldanmatias.rickverse.core.ui.R as CoreUiR

/**
 * Stateless screen: `(state, onIntent)` in, UI out. No ViewModel, no Hilt, so previews, screenshot tests and
 * UI tests can render any state directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CastListScreen(
    state: CastListUiState,
    onIntent: (CastListIntent) -> Unit,
    onCastClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHost = remember { SnackbarHostState() }
    val refreshFailed = (state.phase as? CastListUiState.Phase.Failed)?.takeIf { state.items.isNotEmpty() }
    val refreshFailedMessage = refreshFailed?.error?.toMessage()
    val retryLabel = stringResource(CoreUiR.string.action_retry)
    // A failed refresh keeps the list and surfaces an undoable message instead of replacing the content.
    LaunchedEffect(refreshFailed) {
        if (refreshFailedMessage != null) {
            val result = snackbarHost.showSnackbar(refreshFailedMessage, retryLabel, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) onIntent(CastListIntent.Retry)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { RickVerseTopBar(title = stringResource(R.string.characters_title)) },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SearchField(
                query = state.query,
                onQueryChange = { onIntent(CastListIntent.QueryChanged(it)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            StatusFilterBar(
                selected = state.statusFilter,
                onSelected = { onIntent(CastListIntent.StatusSelected(it)) },
            )
            Content(state = state, onIntent = onIntent, onCastClick = onCastClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    state: CastListUiState,
    onIntent: (CastListIntent) -> Unit,
    onCastClick: (Int) -> Unit,
) {
    when (val phase = state.phase) {
        CastListUiState.Phase.Loading -> {
            SkeletonList(Modifier.fillMaxSize())
        }

        CastListUiState.Phase.Empty -> {
            EmptyState(
                title = stringResource(R.string.characters_empty_title),
                message =
                    if (state.query.isBlank()) {
                        stringResource(R.string.characters_empty_no_query)
                    } else {
                        stringResource(R.string.characters_empty_message, state.query.trim())
                    },
            )
        }

        is CastListUiState.Phase.Failed -> {
            if (state.items.isEmpty()) {
                ErrorState(error = phase.error, onRetry = { onIntent(CastListIntent.Retry) })
            } else {
                CastList(state, onIntent, onCastClick)
            }
        }

        CastListUiState.Phase.Loaded -> {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onIntent(CastListIntent.Refresh) },
            ) { CastList(state, onIntent, onCastClick) }
        }
    }
}

@Composable
private fun CastList(
    state: CastListUiState,
    onIntent: (CastListIntent) -> Unit,
    onCastClick: (Int) -> Unit,
) {
    val listState = rememberLazyListState()

    // Pagination trigger: observe the *last visible item* as a snapshot Flow and de-duplicate, so one threshold
    // crossing sends one intent instead of one per scroll frame.
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.key
        }.filterIsInstance<Int>()
            .distinctUntilChanged()
            .collect { onIntent(CastListIntent.ItemAppeared(it)) }
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(items = state.items, key = { it.id }, contentType = { "cast" }) { member ->
            CastRow(member = member, onClick = { onCastClick(member.id) })
        }
        item(key = "footer", contentType = "footer") {
            PaginationFooter(
                state = state.pagination,
                endMessage = stringResource(R.string.characters_end),
                onRetry = { onIntent(CastListIntent.RetryNextPage) },
            )
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.characters_search_hint)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.characters_search_clear))
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}
