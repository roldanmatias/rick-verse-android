package com.roldanmatias.rickverse.core.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import com.roldanmatias.rickverse.core.domain.model.CachePolicy
import com.roldanmatias.rickverse.core.domain.model.DataSource
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.newStore() =
        PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { folder.newFile("settings.preferences_pb").also { it.delete() } },
        )

    @Test
    fun `defaults are REST and CacheFirst`() = runTest(UnconfinedTestDispatcher()) {
        val repository = DataStoreSettingsRepository(newStore())

        repository.dataSource.test { assertEquals(DataSource.Rest, awaitItem()) }
        repository.cachePolicy.test { assertEquals(CachePolicy.CacheFirst, awaitItem()) }
    }

    @Test
    fun `changes are emitted to collectors`() = runTest(UnconfinedTestDispatcher()) {
        val repository = DataStoreSettingsRepository(newStore())

        repository.dataSource.test {
            assertEquals(DataSource.Rest, awaitItem())
            repository.setDataSource(DataSource.GraphQl)
            assertEquals(DataSource.GraphQl, awaitItem())
        }
    }

    @Test
    fun `an unknown stored value falls back to the default instead of crashing`() = runTest(
        UnconfinedTestDispatcher(),
    ) {
        val store = newStore()
        store.edit { it[stringPreferencesKey("data_source")] = "Soap" }

        DataStoreSettingsRepository(store).dataSource.test { assertEquals(DataSource.Rest, awaitItem()) }
    }

    @Test
    fun `values persist across repository instances on the same store`() = runTest(UnconfinedTestDispatcher()) {
        val store = newStore()
        DataStoreSettingsRepository(store).setCachePolicy(CachePolicy.NetworkOnly)

        DataStoreSettingsRepository(store).cachePolicy.test { assertEquals(CachePolicy.NetworkOnly, awaitItem()) }
    }
}
