package com.roldanmatias.rickverse.feature.episodes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
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
import com.roldanmatias.rickverse.core.domain.model.EpisodeDetail
import com.roldanmatias.rickverse.core.ui.ErrorState
import com.roldanmatias.rickverse.core.ui.RickVerseTopBar
import com.roldanmatias.rickverse.feature.episodes.R
import com.roldanmatias.rickverse.feature.episodes.presentation.EpisodeDetailUiState
import com.roldanmatias.rickverse.feature.episodes.presentation.EpisodeDetailViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
public fun EpisodeDetailRoute(onBack: () -> Unit, onCastClick: (Int) -> Unit, modifier: Modifier = Modifier) {
    val viewModel: EpisodeDetailViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    EpisodeDetailScreen(state, viewModel::retry, onBack, onCastClick, modifier)
}

@Composable
internal fun EpisodeDetailScreen(
    state: EpisodeDetailUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onCastClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            val title = (state as? EpisodeDetailUiState.Loaded)?.detail?.episode?.code
                ?: stringResource(R.string.episode_title)
            RickVerseTopBar(title = title, onBack = onBack)
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding).fillMaxSize()
        when (state) {
            EpisodeDetailUiState.Loading -> {
                Column(contentModifier, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                }
            }

            is EpisodeDetailUiState.Failed -> ErrorState(state.error, onRetry, contentModifier)

            is EpisodeDetailUiState.Loaded -> Content(state.detail, onCastClick, contentModifier)
        }
    }
}

@Composable
private fun Content(detail: EpisodeDetail, onCastClick: (Int) -> Unit, modifier: Modifier) {
    LazyColumn(modifier) {
        item {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(detail.episode.name, style = MaterialTheme.typography.headlineSmall)
                detail.episode.airDate?.let { date ->
                    val formatted = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                    Text(stringResource(R.string.episode_aired, formatted), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Text(
                stringResource(R.string.episode_cast, detail.cast.size),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp, 8.dp).semantics { heading() },
            )
        }
        items(detail.cast, key = { it.id }) { member ->
            Row(
                Modifier.fillMaxWidth().clickable { onCastClick(member.id) }.padding(16.dp, 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarImage(url = member.imageUrl, contentDescription = null, size = 48.dp)
                Text(member.name, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
