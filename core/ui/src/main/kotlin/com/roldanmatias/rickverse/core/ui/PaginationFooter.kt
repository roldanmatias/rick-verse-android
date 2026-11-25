package com.roldanmatias.rickverse.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.roldanmatias.rickverse.core.ui.pagination.PaginationState

/**
 * The row under a paginated list: spinner while loading, "Couldn't load more" + Retry on failure,
 * [endMessage] when exhausted, nothing when idle.
 */
@Composable
public fun PaginationFooter(
    state: PaginationState,
    endMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowModifier = modifier.fillMaxWidth().padding(16.dp)
    when (state) {
        PaginationState.Idle -> {
            // Nothing to show until a page is requested.
        }

        PaginationState.Loading -> {
            val description = stringResource(R.string.pagination_loading)
            Row(rowModifier, horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(Modifier.size(24.dp).semantics { contentDescription = description })
            }
        }

        is PaginationState.Failed -> {
            Row(
                rowModifier,
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.pagination_failed), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
            }
        }

        PaginationState.Exhausted -> {
            Row(rowModifier, horizontalArrangement = Arrangement.Center) {
                Text(endMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
