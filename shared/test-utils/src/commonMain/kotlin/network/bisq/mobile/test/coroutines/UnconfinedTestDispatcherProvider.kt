package network.bisq.mobile.test.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import network.bisq.mobile.domain.coroutines.DispatcherProvider

// TODO: Not consumed yet. Kept intentionally as part of the shared test toolkit for the
//  upcoming DispatcherProvider migration, where some presenter tests will need an unconfined
//  (eager) dispatcher instead of the standard (queued) StandardTestDispatcherProvider — e.g.
//  flows whose emissions must run immediately without advanceUntilIdle(). Wire up with its
//  first consumer, or remove and re-add then if it is still unused.
@OptIn(ExperimentalCoroutinesApi::class)
class UnconfinedTestDispatcherProvider : DispatcherProvider {
    private val testDispatcher = UnconfinedTestDispatcher()

    override val main: CoroutineDispatcher get() = testDispatcher
    override val io: CoroutineDispatcher get() = testDispatcher
    override val default: CoroutineDispatcher get() = testDispatcher
    override val unconfined: CoroutineDispatcher get() = testDispatcher
}
