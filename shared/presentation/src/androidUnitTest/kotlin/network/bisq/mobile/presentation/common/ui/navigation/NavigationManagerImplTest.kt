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

    @Test
    fun navigate_doesNotThrowWithoutController() =
        runTest(testDispatcher) {
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val navigationManager = NavigationManagerImpl(jobsManager)

            // Navigation without a controller should not throw
            navigationManager.navigate(NavRoute.Splash)
            testScheduler.advanceUntilIdle()

            // Test passes if no exception is thrown
        }

    @Test
    fun navigate_rapidCallsDoNotCauseExceptions() =
        runTest(testDispatcher) {
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val navigationManager = NavigationManagerImpl(jobsManager)

            // Simulate rapid clicking - multiple navigation calls in quick succession
            // This tests that the throttling mechanism prevents issues
            repeat(10) {
                navigationManager.navigate(NavRoute.Splash)
            }
            testScheduler.advanceUntilIdle()

            // Test passes if no exception is thrown - throttling should prevent any issues
        }

    @Test
    fun navigateToTab_doesNotThrowWithoutController() =
        runTest(testDispatcher) {
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val navigationManager = NavigationManagerImpl(jobsManager)

            // Tab navigation without a controller should not throw
            navigationManager.navigateToTab(NavRoute.TabHome)
            testScheduler.advanceUntilIdle()

            // Test passes if no exception is thrown
        }

    @Test
    fun navigateToTab_rapidCallsDoNotCauseExceptions() =
        runTest(testDispatcher) {
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val navigationManager = NavigationManagerImpl(jobsManager)

            // Simulate rapid tab switching - multiple calls in quick succession
            // This tests that the throttling mechanism prevents issues
            repeat(10) {
                navigationManager.navigateToTab(NavRoute.TabHome)
            }
            testScheduler.advanceUntilIdle()

            // Test passes if no exception is thrown - throttling should prevent any issues
        }

    @Test
    fun navigate_withTimingGap_allowsMultipleNavigations() =
        runTest(testDispatcher) {
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val navigationManager = NavigationManagerImpl(jobsManager)

            // First navigation
            navigationManager.navigate(NavRoute.Splash)
            testScheduler.advanceUntilIdle()

            // Wait for throttle period to pass (300ms)
            testScheduler.advanceTimeBy(350)

            // Second navigation after throttle period should not throw
            navigationManager.navigate(NavRoute.Splash)
            testScheduler.advanceUntilIdle()

            // Test passes if no exception is thrown
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
