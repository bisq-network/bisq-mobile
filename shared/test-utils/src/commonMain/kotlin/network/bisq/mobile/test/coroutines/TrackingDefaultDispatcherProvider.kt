package network.bisq.mobile.test.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import network.bisq.mobile.domain.coroutines.DispatcherProvider
import kotlin.coroutines.CoroutineContext

/**
 * Like [StandardTestDispatcherProvider], every dispatcher runs on [dispatcher], so virtual time still
 * drives everything. [default] also marks while it runs a block: read [isRunningOnDefault] from code
 * the subject calls (a mock answer, a fake) to check that work was sent off main.
 */
class TrackingDefaultDispatcherProvider(
    private val dispatcher: CoroutineDispatcher,
) : DispatcherProvider {
    var isRunningOnDefault = false
        private set

    private val trackingDefault =
        object : CoroutineDispatcher() {
            override fun dispatch(
                context: CoroutineContext,
                block: Runnable,
            ) {
                dispatcher.dispatch(context) {
                    isRunningOnDefault = true
                    try {
                        block.run()
                    } finally {
                        isRunningOnDefault = false
                    }
                }
            }
        }

    override val main get() = dispatcher
    override val io get() = dispatcher
    override val default: CoroutineDispatcher get() = trackingDefault
    override val unconfined get() = dispatcher
}
