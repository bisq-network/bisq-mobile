package network.bisq.mobile.test.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import network.bisq.mobile.domain.coroutines.DispatcherProvider

class StandardTestDispatcherProvider(
    dispatcher: CoroutineDispatcher = StandardTestDispatcher(),
) : DispatcherProvider {
    private val testDispatcher = dispatcher
    override val main get() = testDispatcher
    override val io get() = testDispatcher
    override val default get() = testDispatcher
    override val unconfined get() = testDispatcher
}
