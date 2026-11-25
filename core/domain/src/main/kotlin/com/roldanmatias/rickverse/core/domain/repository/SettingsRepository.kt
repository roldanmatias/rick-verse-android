package com.roldanmatias.rickverse.core.domain.repository

import com.roldanmatias.rickverse.core.domain.model.CachePolicy
import com.roldanmatias.rickverse.core.domain.model.DataSource
import kotlinx.coroutines.flow.Flow

/** User preferences, observable so the app can react (for example by switching the active data source). */
public interface SettingsRepository {
    /** The selected backend API. Emits the current value on collection, then every change. */
    public val dataSource: Flow<DataSource>

    /** The selected GraphQL cache policy. */
    public val cachePolicy: Flow<CachePolicy>

    public suspend fun setDataSource(value: DataSource)

    public suspend fun setCachePolicy(value: CachePolicy)
}
