package network.bisq.mobile.test.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import network.bisq.mobile.domain.coroutines.DispatcherProvider

class StandardTestDispatcherProvider(
    private val dispatcher: CoroutineDispatcher = StandardTestDispatcher(),
) : DispatcherProvider {
    override val main get() = dispatcher
    override val io get() = dispatcher
    override val default get() = dispatcher
    override val unconfined get() = dispatcher
}
