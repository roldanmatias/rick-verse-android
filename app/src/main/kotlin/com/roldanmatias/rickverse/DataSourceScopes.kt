package com.roldanmatias.rickverse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import com.roldanmatias.rickverse.core.domain.model.DataSource
import com.roldanmatias.rickverse.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Activity-level state for the data-source switch (ADR 0004).
 *
 * Every screen's ViewModel lives in a [ViewModelStore] that belongs to one *data-source epoch*. Switching the source
 * clears the old epoch's store, which destroys all of its ViewModels at once; the UI is rebuilt with a fresh store and
 * a fresh back stack. Because this holder is itself a ViewModel, a rotation keeps the current epoch (no reload).
 */
@HiltViewModel
internal class DataSourceScopes @Inject constructor(settings: SettingsRepository) : ViewModel() {
    /** `null` until DataStore has delivered the stored value, so the UI never flashes the wrong source. */
    val dataSource: StateFlow<DataSource?> =
        settings.dataSource.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val stores = mutableMapOf<DataSource, ViewModelStore>()

    fun ownerFor(source: DataSource): ViewModelStoreOwner {
        val store = stores.getOrPut(source) { ViewModelStore() }
        return object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = store
        }
    }

    /** Destroy every epoch except [current]. Called after a switch. */
    fun clearExcept(current: DataSource) {
        stores.keys.filter { it != current }.forEach { stores.remove(it)?.clear() }
    }

    override fun onCleared() {
        stores.values.forEach(ViewModelStore::clear)
        stores.clear()
    }
}
