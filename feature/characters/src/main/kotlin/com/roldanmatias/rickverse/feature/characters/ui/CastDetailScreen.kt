package com.roldanmatias.rickverse.feature.characters.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roldanmatias.rickverse.core.designsystem.component.AvatarImage
import com.roldanmatias.rickverse.core.designsystem.component.StatusBadge
import com.roldanmatias.rickverse.core.domain.model.CastMemberDetail
import com.roldanmatias.rickverse.core.ui.ErrorState
import com.roldanmatias.rickverse.core.ui.OfflineBanner
import com.roldanmatias.rickverse.core.ui.RickVerseTopBar
import com.roldanmatias.rickverse.feature.characters.R
import com.roldanmatias.rickverse.feature.characters.presentation.CastDetailUiState
import com.roldanmatias.rickverse.feature.characters.presentation.CastDetailViewModel

@Composable
public fun CastDetailRoute(onBack: () -> Unit, onEpisodeClick: (Int) -> Unit, modifier: Modifier = Modifier) {
    val viewModel: CastDetailViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CastDetailScreen(state, viewModel::retry, onBack, onEpisodeClick, modifier)
}

@Composable
internal fun CastDetailScreen(
    state: CastDetailUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onEpisodeClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            val title = (state as? CastDetailUiState.Loaded)?.detail?.member?.name
                ?: stringResource(R.string.detail_title)
            RickVerseTopBar(title = title, onBack = onBack)
        },
    ) { padding ->
        when (state) {
            CastDetailUiState.Loading -> {
                Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                }
            }

            is CastDetailUiState.Failed -> ErrorState(state.error, onRetry, Modifier.padding(padding))

            is CastDetailUiState.Loaded -> DetailContent(state.detail, onEpisodeClick, padding)
        }
    }
}

@Composable
private fun DetailContent(detail: CastMemberDetail, onEpisodeClick: (Int) -> Unit, padding: PaddingValues) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
        if (detail.fromCache) item(key = "offline", contentType = "banner") { OfflineBanner() }
        item(key = "header", contentType = "header") {
            Header(detail)
        }
        item {
            Text(
                text = stringResource(R.string.detail_episodes, detail.episodes.size),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp).semantics { heading() },
            )
        }
        items(detail.episodes, key = { it.id }) { episode ->
            Column(Modifier.fillMaxWidth().clickable { onEpisodeClick(episode.id) }.padding(16.dp, 12.dp)) {
                Text(episode.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    episode.code,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun Header(detail: CastMemberDetail) {
    val member = detail.member
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        AvatarImage(
            url = member.imageUrl,
            contentDescription = stringResource(R.string.characters_avatar_description, member.name),
            size = 120.dp,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            StatusBadge(stringResource(member.status.labelRes()), member.status.tone())
            Text("${member.species} · ${detail.gender}", style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.detail_origin, detail.originName), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.detail_location, detail.locationName),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
