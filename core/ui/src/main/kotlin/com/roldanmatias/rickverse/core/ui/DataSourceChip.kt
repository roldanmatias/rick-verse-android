package com.roldanmatias.rickverse.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * "REST" or "GraphQL", provided once by `:app` so every top bar can show which backend is active during a demo
 * without features knowing about data sources. `null` hides the chip (previews, tests).
 */
public val LocalDataSourceLabel: ProvidableCompositionLocal<String?> = staticCompositionLocalOf { null }

/** The app's standard top bar: title plus the data-source chip. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun RickVerseTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = { Text(title) },
        modifier = modifier,
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            }
        },
        actions = {
            actions()
            LocalDataSourceLabel.current?.let { label ->
                val description = stringResource(R.string.data_source_chip_description, label)
                AssistChip(
                    onClick = {},
                    label = { Text(label) },
                    modifier = Modifier.padding(end = 8.dp).semantics { contentDescription = description },
                )
            }
        },
    )
}
