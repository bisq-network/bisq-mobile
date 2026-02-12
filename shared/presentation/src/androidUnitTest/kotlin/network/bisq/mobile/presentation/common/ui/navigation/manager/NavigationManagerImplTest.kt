package network.bisq.mobile.presentation.common.ui.navigation.manager

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavUri
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.TabNavRoute
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * JVM unit tests for NavigationManagerImpl.
 *
 * These tests verify that NavigationManagerImpl behaves safely when no NavController
 * is available, and that concurrent navigation calls are properly serialized.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NavigationManagerImplTest {
    private val testDispatcher = StandardTestDispatcher()
    private val capturedLogs = mutableListOf<String>()
    private lateinit var originalWriters: List<LogWriter>

    @Before
    fun setupLogCapturing() {
        capturedLogs.clear()
        val testWriter =
            object : LogWriter() {
                override fun log(
                    severity: Severity,
                    message: String,
                    tag: String,
                    throwable: Throwable?,
                ) {
                    if (severity == Severity.Error) {
                        capturedLogs.add(message)
                    }
                }
            }
        originalWriters = Logger.config.logWriterList.toList()
        Logger.setLogWriters(testWriter)
    }

    @After
    fun restoreLogWriters() {
        Logger.setLogWriters(*originalWriters.toTypedArray())
    }

    // ========== Navigation State Query Tests ==========

    @Test
    fun `when no controller set then show back button returns false`() =
        runTest(testDispatcher) {
            // Given
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val navigationManager = NavigationManagerImpl(jobsManager)

            // When/Then
            assertFalse(navigationManager.showBackButton())
        }

    @Test
    fun `when no controller set then is at main screen returns false`() =
        runTest(testDispatcher) {
            // Given
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val navigationManager = NavigationManagerImpl(jobsManager)

            // When/Then
            assertFalse(navigationManager.isAtMainScreen())
        }

    @Test
    fun `when no controller set then is at home tab returns false`() =
        runTest(testDispatcher) {
            // Given
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val navigationManager = NavigationManagerImpl(jobsManager)

            // When/Then
            assertFalse(navigationManager.isAtHomeTab())
        }

    @Test
    fun `when initial state then current tab is null`() =
        runTest(testDispatcher) {
            // Given
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val navigationManager = NavigationManagerImpl(jobsManager)

            // When/Then
            assertEquals(null, navigationManager.currentTab.value)
        }

    // ========== Navigate Tests ==========

    @Test
    fun `when navigate without controller then onCompleted is invoked`() =
        runTest(testDispatcher) {
            // Given
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val navigationManager = NavigationManagerImpl(jobsManager)
            var completed = false

            // When
            navigationManager.navigate(NavRoute.Splash) {
                completed = true
            }
            advanceUntilIdle()

            // Then
            assertTrue(completed, "onCompleted should be invoked even without controller")
        }

    @Test
    fun `when navigate with mock controller then navigate is called`() =
        runTest(testDispatcher) {
            // Given
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val navigationManager = NavigationManagerImpl(jobsManager)
            val mockController = mockk<NavHostController>(relaxed = true)

            navigationManager.setRootNavController(mockController)
            advanceUntilIdle()

            // When
            navigationManager.navigate(NavRoute.Splash)
            advanceUntilIdle()

            // Then
            verify(exactly = 1) { mockController.navigate<NavRoute>(NavRoute.Splash, any<NavOptionsBuilder.() -> Unit>()) }
        }

    @Test
    fun `when navigate to tab with mocks then navigates on both controllers`() =
        runTest(testDispatcher) {
            // Given
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val navigationManager = NavigationManagerImpl(jobsManager)
            val mockRootController = mockk<NavHostController>(relaxed = true)
            val mockTabController = mockk<NavHostController>(relaxed = true)

            // Setup root controller - TabContainer NOT in backstack
            every { mockRootController.currentBackStack } returns MutableStateFlow(emptyList())

            navigationManager.setRootNavController(mockRootController)
            navigationManager.setTabNavController(mockTabController)
            advanceUntilIdle()

            // When
            navigationManager.navigateToTab(NavRoute.TabHome)
            advanceUntilIdle()

            // Then - verify both controllers received navigate calls
            verify(exactly = 1) { mockRootController.navigate<NavRoute>(NavRoute.TabContainer, any<NavOptionsBuilder.() -> Unit>()) }
            verify(exactly = 1) { mockTabController.navigate<TabNavRoute>(NavRoute.TabHome, any<NavOptionsBuilder.() -> Unit>()) }
        }

    @Test
    fun `when navigate back with mocks then popBackStack is called`() =
        runTest(testDispatcher) {
            // Given
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val navigationManager = NavigationManagerImpl(jobsManager)
            val mockController = mockk<NavHostController>(relaxed = true)

            // Setup backstack with 2 entries (size > 1)
            val mockBackStackEntry = mockk<NavBackStackEntry>()
            every { mockController.currentBackStack } returns MutableStateFlow(listOf(mockBackStackEntry, mockBackStackEntry))

            navigationManager.setRootNavController(mockController)
            advanceUntilIdle()

            // When
            navigationManager.navigateBack()
            advanceUntilIdle()

            // Then
            verify(exactly = 1) { mockController.popBackStack() }
        }

    @Test
    fun `when navigate back to with mocks then popBackStack with route is called`() =
        runTest(testDispatcher) {
            // Given
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val navigationManager = NavigationManagerImpl(jobsManager)
            val mockController = mockk<NavHostController>(relaxed = true)

            navigationManager.setRootNavController(mockController)
            advanceUntilIdle()

            // When
            navigationManager.navigateBackTo(NavRoute.Splash, shouldInclusive = false, shouldSaveState = false)
            advanceUntilIdle()

            // Then
            verify(exactly = 1) { mockController.popBackStack(NavRoute.Splash, inclusive = false, saveState = false) }
        }

    @Test
    fun `when rapid navigation calls then all calls are processed`() =
        runTest(testDispatcher) {
            // Given
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val navigationManager = NavigationManagerImpl(jobsManager)
            val mockController = mockk<NavHostController>(relaxed = true)

            navigationManager.setRootNavController(mockController)
            advanceUntilIdle()

            // When - simulate rapid clicking (10 calls)
            repeat(10) {
                navigationManager.navigate(NavRoute.Splash)
            }
            advanceUntilIdle()

            // Then - verify mutex serialized all calls without dropping any
            verify(exactly = 10) { mockController.navigate<NavRoute>(NavRoute.Splash, any<NavOptionsBuilder.() -> Unit>()) }
        }

    // ========== NavigateToTab Tests ==========

    @Test
    fun `when rapid tab navigation calls then all calls are processed`() =
        runTest(testDispatcher) {
            // Given
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val navigationManager = NavigationManagerImpl(jobsManager)
            val mockRootController = mockk<NavHostController>(relaxed = true)
            val mockTabController = mockk<NavHostController>(relaxed = true)

            // Setup root controller with empty backstack
            every { mockRootController.currentBackStack } returns MutableStateFlow(emptyList())

            navigationManager.setRootNavController(mockRootController)
            navigationManager.setTabNavController(mockTabController)
            advanceUntilIdle()

            // When - simulate rapid tab switching (10 calls)
            repeat(10) {
                navigationManager.navigateToTab(NavRoute.TabHome)
            }
            advanceUntilIdle()

            // Then - verify both controllers processed all calls
            verify(exactly = 10) { mockRootController.navigate<NavRoute>(NavRoute.TabContainer, any<NavOptionsBuilder.() -> Unit>()) }
            verify(exactly = 10) { mockTabController.navigate<TabNavRoute>(NavRoute.TabHome, any<NavOptionsBuilder.() -> Unit>()) }
        }

    // ========== NavigateFromUri Tests ==========

    @Test
    fun `when navigate from uri with deep link then navigate is called`() =
        runTest(testDispatcher) {
            // Given
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val navigationManager = NavigationManagerImpl(jobsManager)
            val mockController = mockk<NavHostController>(relaxed = true)
            val mockGraph = mockk<NavGraph>(relaxed = true)
            val mockNavUri = mockk<NavUri>(relaxed = true)

            // Mock NavUri factory function
            mockkStatic(::NavUri)
            every { NavUri(any<String>()) } returns mockNavUri

            // Setup: graph has deep link for the mock NavUri
            every { mockController.graph } returns mockGraph
            every { mockGraph.hasDeepLink(mockNavUri) } returns true

            navigationManager.setRootNavController(mockController)
            advanceUntilIdle()

            // When
            navigationManager.navigateFromUri("https://bisq.network/test")
            advanceUntilIdle()

            // Then - verify root controller navigated via deep link
            verify(exactly = 1) { mockController.navigate(mockNavUri, any<NavOptions>()) }

            // Clean up static mock
            unmockkStatic(::NavUri)
        }

    // ========== NavigateBack Tests ==========

    @Test
    fun `when navigate back without controller then onCompleted is invoked`() =
        runTest(testDispatcher) {
            // Given
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val navigationManager = NavigationManagerImpl(jobsManager)
            var completed = false

            // When
            navigationManager.navigateBack {
                completed = true
            }
            advanceUntilIdle()

            // Then
            assertTrue(completed, "onCompleted should be invoked even without controller")
        }

    // ========== Test Helpers ==========

    @Suppress("CanBeParameter")
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
