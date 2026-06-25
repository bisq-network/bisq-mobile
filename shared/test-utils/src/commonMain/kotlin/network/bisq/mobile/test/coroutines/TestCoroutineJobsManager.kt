package network.bisq.mobile.test.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import network.bisq.mobile.domain.utils.CoroutineJobsManager

class TestCoroutineJobsManager(
    dispatcher: CoroutineDispatcher,
    override var coroutineExceptionHandler: ((Throwable) -> Unit)? = null,
) : CoroutineJobsManager {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    override suspend fun dispose() {
        scope.cancel()
    }

    override fun getScope(): CoroutineScope = scope
}
