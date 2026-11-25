package com.roldanmatias.rickverse.feature.characters.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.roldanmatias.rickverse.core.designsystem.component.AvatarImage
import com.roldanmatias.rickverse.core.designsystem.component.StatusBadge
import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.feature.characters.R

/**
 * One list row. The whole row is a **single** accessibility node with a meaningful label
 * ("Rick Sanchez, Alive, Human"); the avatar is decorative because its information is already in the label.
 */
@Composable
internal fun CastRow(member: CastMember, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val status = stringResource(member.status.labelRes())
    val description = stringResource(R.string.characters_row_description, member.name, status, member.species)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .clickable(onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = description }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AvatarImage(url = member.imageUrl, contentDescription = null)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge(text = status, tone = member.status.tone())
                Text(
                    text = member.species,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
