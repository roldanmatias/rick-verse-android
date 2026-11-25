package com.roldanmatias.rickverse.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.roldanmatias.rickverse.core.domain.error.DomainError

/**
 * User-facing copy lives here, in the UI layer: Domain and ViewModels carry only the typed [DomainError],
 * so wording and localisation never leak into business code.
 */
@StringRes
public fun DomainError.messageRes(): Int =
    when (this) {
        DomainError.Offline -> R.string.error_offline
        DomainError.Timeout -> R.string.error_timeout
        DomainError.NotFound -> R.string.error_not_found
        is DomainError.Server -> R.string.error_server
        is DomainError.InvalidData -> R.string.error_invalid_data
        is DomainError.Unknown -> R.string.error_unknown
    }

@Composable
public fun DomainError.toMessage(): String = stringResource(messageRes())
