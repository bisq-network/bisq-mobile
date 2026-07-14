package network.bisq.mobile.test.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import network.bisq.mobile.domain.utils.CoroutineJobsManager

class TestCoroutineJobsManager(
    private val dispatcher: CoroutineDispatcher,
    override var coroutineExceptionHandler: ((Throwable) -> Unit)? = null,
) : CoroutineJobsManager {
    private var scope = CoroutineScope(dispatcher + SupervisorJob())

    override suspend fun dispose() {
        // Test-only: recreate scope after cancel so one Koin instance can be reused across cases.
        scope.cancel()
        scope = CoroutineScope(dispatcher + SupervisorJob())
    }

    override fun getScope(): CoroutineScope = scope
}
