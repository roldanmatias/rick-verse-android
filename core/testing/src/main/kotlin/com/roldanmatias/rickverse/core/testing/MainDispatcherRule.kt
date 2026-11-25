package com.roldanmatias.rickverse.core.testing

import com.roldanmatias.rickverse.core.common.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Replaces `Dispatchers.Main` with a [TestDispatcher] for the duration of a test (ViewModels use `viewModelScope`,
 * which runs on Main). Use a [StandardTestDispatcher] by default so virtual time (`advanceTimeBy`) is under the
 * test's control.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public class MainDispatcherRule(
    public val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

/** A [DispatcherProvider] where every dispatcher is the same [dispatcher]. */
public class TestDispatcherProvider(
    dispatcher: CoroutineDispatcher,
) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}
