package network.bisq.mobile.client.test_utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import network.bisq.mobile.domain.utils.CoroutineJobsManager

class TestCoroutineJobsManager(
    dispatcher: CoroutineDispatcher,
    override var coroutineExceptionHandler: ((Throwable) -> Unit)? = null,
) : CoroutineJobsManager {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private val jobs = mutableSetOf<Job>()

    override suspend fun dispose() {
        scope.cancel()
        jobs.clear()
    }

    override fun getScope(): CoroutineScope = scope
}
