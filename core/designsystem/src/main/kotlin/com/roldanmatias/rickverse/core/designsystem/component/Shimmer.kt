package com.roldanmatias.rickverse.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

private const val SHIMMER_MILLIS = 1_100

/** Animated placeholder brush for skeleton screens. Cheaper to understand than a spinner and avoids layout jumps. */
@Composable
public fun Modifier.shimmer(): Modifier {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    val progress =
        rememberInfiniteTransition(label = "shimmer").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(SHIMMER_MILLIS, easing = LinearEasing), RepeatMode.Restart),
            label = "shimmerProgress",
        )
    return background(
        Brush.horizontalGradient(
            colors = listOf(base, highlight, base),
            startX = -SHIMMER_TRAVEL + progress.value * SHIMMER_TRAVEL * 2,
            endX = progress.value * SHIMMER_TRAVEL * 2,
        ),
    )
}

private const val SHIMMER_TRAVEL = 600f

/** One skeleton row matching the shape of a list row (avatar + two text lines). */
@Composable
public fun SkeletonRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).shimmer())
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Spacer(
                Modifier
                    .height(18.dp)
                    .width(160.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmer(),
            )
            Spacer(
                Modifier
                    .height(14.dp)
                    .width(100.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmer(),
            )
        }
    }
}

/** Eight skeleton rows. Hidden from accessibility services: TalkBack should announce loading once, not eight blanks. */
@Composable
public fun SkeletonList(modifier: Modifier = Modifier, rows: Int = 8) {
    Column(modifier.clearAndSetSemantics { }) { repeat(rows) { SkeletonRow() } }
}
