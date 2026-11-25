package com.roldanmatias.rickverse.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.roldanmatias.rickverse.core.designsystem.theme.RickVerseTheme

/** A coloured dot **plus text**: state is never conveyed by colour alone (accessibility). */
@Composable
public fun StatusBadge(text: String, tone: BadgeTone, modifier: Modifier = Modifier) {
    val colors = RickVerseTheme.statusColors
    val dot =
        when (tone) {
            BadgeTone.Positive -> colors.positive
            BadgeTone.Negative -> colors.negative
            BadgeTone.Neutral -> colors.neutral
        }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Spacer(Modifier.size(8.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusBadgePreview() {
    RickVerseTheme(dynamicColor = false) {
        Row { StatusBadge("Alive", BadgeTone.Positive) }
    }
}
