package com.roldanmatias.rickverse.feature.characters.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.feature.characters.R

/** Chips for All / Alive / Dead / Unknown. Changing the filter gives a haptic tick. */
@Composable
internal fun StatusFilterBar(
    selected: CastMember.Status?,
    onSelected: (CastMember.Status?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val select: (CastMember.Status?) -> Unit = { status ->
        if (status != selected) {
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
            onSelected(status)
        }
    }
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            FilterChip(
                selected = selected == null,
                onClick = { select(null) },
                label = { Text(stringResource(R.string.characters_filter_all)) },
            )
        }
        items(CastMember.Status.entries, key = { it.name }) { status ->
            FilterChip(
                selected = selected == status,
                onClick = { select(status) },
                label = { Text(stringResource(status.labelRes())) },
            )
        }
    }
}
