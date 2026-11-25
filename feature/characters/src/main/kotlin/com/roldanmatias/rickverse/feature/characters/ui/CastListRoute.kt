package com.roldanmatias.rickverse.feature.characters.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roldanmatias.rickverse.feature.characters.presentation.CastListViewModel

/**
 * The only composable here that knows about Hilt and the ViewModel. It collects state **lifecycle-aware**
 * (`collectAsStateWithLifecycle` stops collecting when the app is in the background; plain `collectAsState`
 * would not) and hands plain `(state, onIntent)` to the stateless screen.
 */
@Composable
public fun CastListRoute(onCastClick: (Int) -> Unit, modifier: Modifier = Modifier) {
    val viewModel: CastListViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CastListScreen(state = state, onIntent = viewModel::onIntent, onCastClick = onCastClick, modifier = modifier)
}
