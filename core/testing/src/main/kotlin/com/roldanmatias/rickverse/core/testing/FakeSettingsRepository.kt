package com.roldanmatias.rickverse.core.testing

import com.roldanmatias.rickverse.core.domain.model.CachePolicy
import com.roldanmatias.rickverse.core.domain.model.DataSource
import com.roldanmatias.rickverse.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [SettingsRepository]. Flip [dataSource] in a test to simulate the user changing the setting. */
public class FakeSettingsRepository(
    initialDataSource: DataSource = DataSource.Rest,
    initialCachePolicy: CachePolicy = CachePolicy.CacheFirst,
) : SettingsRepository {
    private val dataSourceState = MutableStateFlow(initialDataSource)
    private val cachePolicyState = MutableStateFlow(initialCachePolicy)

    override val dataSource: Flow<DataSource> = dataSourceState
    override val cachePolicy: Flow<CachePolicy> = cachePolicyState

    override suspend fun setDataSource(value: DataSource) {
        dataSourceState.value = value
    }

    override suspend fun setCachePolicy(value: CachePolicy) {
        cachePolicyState.value = value
    }
}
