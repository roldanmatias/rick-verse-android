package com.roldanmatias.rickverse.feature.characters.ui

import androidx.annotation.StringRes
import com.roldanmatias.rickverse.core.designsystem.component.BadgeTone
import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.feature.characters.R

/** Presentation of a domain status: wording and tone live in the UI layer, not in Domain. */
@StringRes
internal fun CastMember.Status.labelRes(): Int =
    when (this) {
        CastMember.Status.Alive -> R.string.status_alive
        CastMember.Status.Dead -> R.string.status_dead
        CastMember.Status.Unknown -> R.string.status_unknown
    }

internal fun CastMember.Status.tone(): BadgeTone =
    when (this) {
        CastMember.Status.Alive -> BadgeTone.Positive
        CastMember.Status.Dead -> BadgeTone.Negative
        CastMember.Status.Unknown -> BadgeTone.Neutral
    }
