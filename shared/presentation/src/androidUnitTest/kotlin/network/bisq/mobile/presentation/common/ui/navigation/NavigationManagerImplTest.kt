package network.bisq.mobile.presentation.common.ui.navigation

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManagerImpl
import kotlin.test.Test
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationManagerImplTest {
    private val testDispatcher = StandardTestDispatcher()

    // We cannot easily instantiate a real NavHostController in pure JVM tests without
    // Android instrumentation, so focus on verifying that APIs behave safely in the
    // absence of any controller / graph instead of constructing controllers here.

    @Test
    fun showBackButton_false_whenNoControllerOrGraph() =
        runTest(testDispatcher) {
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val navigationManager = NavigationManagerImpl(jobsManager)

            // No controller set -> should safely report false without throwing
            assertFalse(navigationManager.showBackButton())
        }

    // Minimal TestCoroutineJobsManager mirroring the pattern used in OfferbookPresenterFilterTest
    private class TestCoroutineJobsManager(
        private val dispatcher: CoroutineDispatcher,
        override var coroutineExceptionHandler: ((Throwable) -> Unit)? = null,
    ) : CoroutineJobsManager {
        private val scope = CoroutineScope(dispatcher + SupervisorJob())

        override suspend fun dispose() {
            scope.cancel()
        }

        override fun getScope(): CoroutineScope = scope
    }
}
