package com.roldanmatias.rickverse.feature.settings

import app.cash.turbine.test
import com.roldanmatias.rickverse.core.network.NetworkEvent
import com.roldanmatias.rickverse.core.network.NetworkLog
import com.roldanmatias.rickverse.core.network.NetworkSource
import com.roldanmatias.rickverse.core.testing.MainDispatcherRule
import com.roldanmatias.rickverse.feature.settings.presentation.NetworkLogViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NetworkLogViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val log = NetworkLog()

    private fun event(source: NetworkSource, cache: Boolean = false) =
        NetworkEvent(log.nextId(), 0, source, "label", 200, 10, 100, cache)

    @Test
    fun `filters by source and counts network calls vs cache hits`() = runTest(mainRule.dispatcher) {
        log.record(event(NetworkSource.Rest))
        log.record(event(NetworkSource.GraphQl))
        log.record(event(NetworkSource.GraphQl, cache = true))
        val viewModel = NetworkLogViewModel(log)

        viewModel.uiState.test {
            val all = awaitItem().let { if (it.events.isEmpty()) awaitItem() else it }
            assertEquals(3, all.events.size)
            viewModel.onFilterSelected(NetworkSource.GraphQl)
            val graphQl = awaitItem()
            assertEquals(2, graphQl.events.size)
            assertEquals(1, graphQl.totalRequests)
            assertEquals(1, graphQl.cacheHits)
        }
    }

    @Test
    fun `clear empties the log`() = runTest(mainRule.dispatcher) {
        log.record(event(NetworkSource.Rest))
        val viewModel = NetworkLogViewModel(log)

        viewModel.onClear()

        assertEquals(0, log.events.value.size)
    }
}
