package com.roldanmatias.rickverse.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.roldanmatias.rickverse.core.domain.model.CachePolicy
import com.roldanmatias.rickverse.core.domain.model.DataSource
import com.roldanmatias.rickverse.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Settings persisted in Preferences DataStore (coroutine/Flow based, transactional, no main-thread I/O: the
 * replacement for `SharedPreferences`). Values are stored by enum *name*; an unknown or missing value falls back to
 * the default, so a future rename can never crash startup.
 */
internal class DataStoreSettingsRepository @Inject constructor(private val store: DataStore<Preferences>) :
    SettingsRepository {
    override val dataSource: Flow<DataSource> =
        store.data.map { it[DATA_SOURCE].toEnum(DataSource.Rest) }.distinctUntilChanged()

    override val cachePolicy: Flow<CachePolicy> =
        store.data.map { it[CACHE_POLICY].toEnum(CachePolicy.CacheFirst) }.distinctUntilChanged()

    override suspend fun setDataSource(value: DataSource) {
        store.edit { it[DATA_SOURCE] = value.name }
    }

    override suspend fun setCachePolicy(value: CachePolicy) {
        store.edit { it[CACHE_POLICY] = value.name }
    }

    private companion object {
        val DATA_SOURCE = stringPreferencesKey("data_source")
        val CACHE_POLICY = stringPreferencesKey("cache_policy")
    }
}

private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
    enumValues<E>().firstOrNull { it.name == this } ?: default
