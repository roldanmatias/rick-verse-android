package com.roldanmatias.rickverse.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Rounded avatar loaded with Coil, with placeholder and error states.
 * [contentDescription] is required, not defaulted: a decorative image must say so explicitly with `null`.
 */
@Composable
public fun AvatarImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    cornerRadius: Dp = 12.dp,
) {
    val placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        placeholder = placeholder,
        error = ColorPainter(MaterialTheme.colorScheme.errorContainer),
        contentScale = ContentScale.Crop,
        modifier =
            modifier.size(size).clip(
                androidx.compose.foundation.shape
                    .RoundedCornerShape(cornerRadius),
            ),
    )
}
