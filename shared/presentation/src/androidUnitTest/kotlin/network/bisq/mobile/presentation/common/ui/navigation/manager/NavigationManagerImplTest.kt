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
import co.touchlab.kermit.loggerConfigInit
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.test_utils.TestCoroutineJobsManager
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
    fun tearDown() {
        Logger.setLogWriters(*originalWriters.toTypedArray())
        unmockkAll() // Clean up all mocks to prevent leaks between tests
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
    fun `when getTabNavController times out then error is logged`() =
        runTest(testDispatcher) {
            // Given - root controller is set but tab controller is not
            val (_, navigationManager, testLogs) = createTestSetup()
            val mockRootController = mockk<NavHostController>(relaxed = true)

            navigationManager.setRootNavController(mockRootController)
            advanceUntilIdle()

            // When - attempt tab navigation without setting tab controller (will timeout on tab only)
            navigationManager.navigateToTab(NavRoute.TabHome)
            // Advance time past the 10 second timeout
            advanceTimeBy(11000)
            advanceUntilIdle()

            // Then - verify timeout error was logged
            assertTrue(
                testLogs.any { it.contains("Timed out waiting for tab nav controller") },
                "Should log timeout error for tab nav controller",
            )
        }

    @Test
    fun `when setTabNavController to null then current tab is null`() =
        runTest(testDispatcher) {
            // Given
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val navigationManager = NavigationManagerImpl(jobsManager)
            val mockTabController = mockk<NavHostController>(relaxed = true)

            // Setup - set a controller first
            navigationManager.setTabNavController(mockTabController)
            advanceUntilIdle()

            // When - set to null
            navigationManager.setTabNavController(null)
            advanceUntilIdle()

            // Then
            assertEquals(null, navigationManager.currentTab.value)
        }

    @Test
    fun `when navigate to tab preparation fails then error is logged`() =
        runTest(testDispatcher) {
            // Given
            val (_, navigationManager, testLogs) = createTestSetup()
            val mockRootController = mockk<NavHostController>(relaxed = true)
            val mockTabController = mockk<NavHostController>(relaxed = true)

            // Setup: currentBackStack will throw exception
            every { mockRootController.currentBackStack } throws IllegalStateException("Graph not ready")

            navigationManager.setRootNavController(mockRootController)
            navigationManager.setTabNavController(mockTabController)
            advanceUntilIdle()

            // When
            navigationManager.navigateToTab(NavRoute.TabHome)
            advanceUntilIdle()

            // Then - verify error was logged
            assertTrue(
                testLogs.any { it.contains("Failed to prepare tab container navigation") },
                "Should log error when tab preparation fails",
            )
        }

    @Test
    fun `when navigate to tab fails then error is logged`() =
        runTest(testDispatcher) {
            // Given
            val (_, navigationManager, testLogs) = createTestSetup()
            val mockRootController = mockk<NavHostController>(relaxed = true)
            val mockTabController = mockk<NavHostController>(relaxed = true)

            // Setup: tab controller throws on navigate
            every { mockTabController.navigate<TabNavRoute>(any<TabNavRoute>(), any<NavOptionsBuilder.() -> Unit>()) } throws IllegalStateException("Navigation failed")
            every { mockRootController.currentBackStack } returns MutableStateFlow(emptyList())

            navigationManager.setRootNavController(mockRootController)
            navigationManager.setTabNavController(mockTabController)
            advanceUntilIdle()

            // When
            navigationManager.navigateToTab(NavRoute.TabHome)
            advanceUntilIdle()

            // Then - verify error was logged
            assertTrue(
                testLogs.any { it.contains("Failed to navigate to tab") },
                "Should log error when tab navigation fails",
            )
        }

    @Test
    fun `when navigateBackTo fails then error is logged`() =
        runTest(testDispatcher) {
            // Given
            val (_, navigationManager, testLogs) = createTestSetup()
            val mockController = mockk<NavHostController>(relaxed = true)

            // Setup: controller throws on popBackStack
            every { mockController.popBackStack(any<NavRoute>(), any(), any()) } throws IllegalStateException("Pop failed")

            navigationManager.setRootNavController(mockController)
            advanceUntilIdle()

            // When
            navigationManager.navigateBackTo(NavRoute.Splash, shouldInclusive = false, shouldSaveState = false)
            advanceUntilIdle()

            // Then - verify error was logged
            assertTrue(
                testLogs.any { it.contains("Failed to popBackStack") },
                "Should log error when popBackStack fails",
            )
        }

    @Test
    fun `when navigate from uri fails on root graph then error is logged`() =
        runTest(testDispatcher) {
            // Given
            val (_, navigationManager, testLogs) = createTestSetup()
            val mockController = mockk<NavHostController>(relaxed = true)
            val mockGraph = mockk<NavGraph>(relaxed = true)
            val mockNavUri = mockk<NavUri>(relaxed = true)

            // Mock NavUri factory function
            mockkStatic(::NavUri)
            every { NavUri(any<String>()) } returns mockNavUri

            // Setup: graph has deep link but navigate will fail
            every { mockController.graph } returns mockGraph
            every { mockGraph.hasDeepLink(mockNavUri) } returns true
            every { mockController.navigate(mockNavUri, any<NavOptions>()) } throws IllegalStateException("Navigation failed")

            navigationManager.setRootNavController(mockController)
            advanceUntilIdle()

            // When
            navigationManager.navigateFromUri("https://bisq.network/test")
            advanceUntilIdle()

            // Then - verify error was logged
            assertTrue(
                testLogs.any { it.contains("Failed to navigate from uri") && it.contains("via root graph") },
                "Should log error when root deep link navigation fails",
            )
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
    fun `when navigate to tab at main screen then skips TabContainer navigation`() =
        runTest(testDispatcher) {
            // Given
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val (testLogger, _) = createTestLogger()
            // Create spy with isAtMainScreen mocked to return true
            val navigationManager = createSpiedManagerAtMainScreen(jobsManager, testLogger)
            val mockRootController = mockk<NavHostController>(relaxed = true)
            val mockTabController = mockk<NavHostController>(relaxed = true)

            every { mockRootController.currentBackStack } returns MutableStateFlow(emptyList())

            navigationManager.setRootNavController(mockRootController)
            navigationManager.setTabNavController(mockTabController)
            advanceUntilIdle()

            // When
            navigationManager.navigateToTab(NavRoute.TabHome)
            advanceUntilIdle()

            // Then - verify TabContainer navigation was NOT called (skipped because at main screen)
            verify(exactly = 0) { mockRootController.navigate<NavRoute>(NavRoute.TabContainer, any<NavOptionsBuilder.() -> Unit>()) }
            // But tab navigation was still called
            verify(exactly = 1) { mockTabController.navigate<TabNavRoute>(NavRoute.TabHome, any<NavOptionsBuilder.() -> Unit>()) }
        }

    @Test
    fun `when setTabNavController fails to remove listener then error is logged`() =
        runTest(testDispatcher) {
            // Given
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val (testLogger, testLogs) = createTestLogger()
            // Use spy to inject test logger
            val navigationManager = createTestNavigationManager(jobsManager, testLogger)
            val mockTabController = mockk<NavHostController>(relaxed = true)

            // Setup: Setting controller first time works
            navigationManager.setTabNavController(mockTabController)
            advanceUntilIdle()

            // Setup: Second call will try to remove listener, which will fail
            every { mockTabController.removeOnDestinationChangedListener(any()) } throws IllegalStateException("Failed to remove")

            // When - set a new controller (triggers listener removal)
            val newMockController = mockk<NavHostController>(relaxed = true)
            navigationManager.setTabNavController(newMockController)
            advanceUntilIdle()

            // Then - verify error was logged
            assertTrue(
                testLogs.any { it.contains("Failed to remove previous tab destination listener") },
                "Should log error when listener removal fails",
            )
        }

    @Test
    fun `when setTabNavController fails to initialize then error is logged`() =
        runTest(testDispatcher) {
            // Given
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val (testLogger, testLogs) = createTestLogger()
            // Use spy to inject test logger
            val navigationManager = createTestNavigationManager(jobsManager, testLogger)
            val mockTabController = mockk<NavHostController>(relaxed = true)

            // Setup: addOnDestinationChangedListener will throw
            every { mockTabController.addOnDestinationChangedListener(any()) } throws IllegalStateException("Graph not ready")

            // When
            navigationManager.setTabNavController(mockTabController)
            advanceUntilIdle()

            // Then - verify error was logged and current tab is null
            assertTrue(
                testLogs.any { it.contains("Failed to initialize tab nav controller") },
                "Should log error when tab controller initialization fails",
            )
            assertEquals(null, navigationManager.currentTab.value)
        }

    @Test
    fun `when navigate from uri with tab deep link then navigates on tab controller`() =
        runTest(testDispatcher) {
            // Given
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val (testLogger, _) = createTestLogger()
            // Spy with isAtMainScreen = true so we enter the tab deep link branch
            val navigationManager = createSpiedManagerAtMainScreen(jobsManager, testLogger)
            val mockRootController = mockk<NavHostController>(relaxed = true)
            val mockTabController = mockk<NavHostController>(relaxed = true)
            val mockRootGraph = mockk<NavGraph>(relaxed = true)
            val mockTabGraph = mockk<NavGraph>(relaxed = true)
            val mockNavUri = mockk<NavUri>(relaxed = true)

            mockkStatic(::NavUri)
            every { NavUri(any<String>()) } returns mockNavUri

            // Setup: root graph does NOT have deep link, but tab graph DOES
            every { mockRootController.graph } returns mockRootGraph
            every { mockRootGraph.hasDeepLink(mockNavUri) } returns false
            every { mockTabController.graph } returns mockTabGraph
            every { mockTabGraph.hasDeepLink(mockNavUri) } returns true

            navigationManager.setRootNavController(mockRootController)
            navigationManager.setTabNavController(mockTabController)
            advanceUntilIdle()

            // When
            navigationManager.navigateFromUri("https://bisq.network/test")
            advanceUntilIdle()

            // Then - verify root controller did NOT navigate, but tab controller did
            verify(exactly = 0) { mockRootController.navigate(mockNavUri, any<NavOptions>()) }
            verify(exactly = 1) { mockTabController.navigate(mockNavUri, any<NavOptions>()) }
        }

    @Test
    fun `when navigate from uri fails on tab graph then error is logged`() =
        runTest(testDispatcher) {
            // Given
            val jobsManager = TestCoroutineJobsManager(testDispatcher)
            val (testLogger, testLogs) = createTestLogger()
            // Spy with isAtMainScreen = true to enter tab deep link branch
            val navigationManager = createSpiedManagerAtMainScreen(jobsManager, testLogger)
            val mockRootController = mockk<NavHostController>(relaxed = true)
            val mockTabController = mockk<NavHostController>(relaxed = true)
            val mockRootGraph = mockk<NavGraph>(relaxed = true)
            val mockTabGraph = mockk<NavGraph>(relaxed = true)
            val mockNavUri = mockk<NavUri>(relaxed = true)

            mockkStatic(::NavUri)
            every { NavUri(any<String>()) } returns mockNavUri

            // Setup: root graph does NOT have deep link, tab graph DOES but will fail
            every { mockRootController.graph } returns mockRootGraph
            every { mockRootGraph.hasDeepLink(mockNavUri) } returns false
            every { mockTabController.graph } returns mockTabGraph
            every { mockTabGraph.hasDeepLink(mockNavUri) } returns true
            every { mockTabController.navigate(mockNavUri, any<NavOptions>()) } throws IllegalStateException("Tab nav failed")

            navigationManager.setRootNavController(mockRootController)
            navigationManager.setTabNavController(mockTabController)
            advanceUntilIdle()

            // When
            navigationManager.navigateFromUri("https://bisq.network/test")
            advanceUntilIdle()

            // Then - verify error was logged
            assertTrue(
                testLogs.any { it.contains("Failed to navigate from uri") && it.contains("via tab graph") },
                "Should log error when tab deep link navigation fails",
            )
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

    /**
     * Creates a test logger that captures error logs to a mutable list.
     */
    private fun createTestLogger(): Pair<Logger, MutableList<String>> {
        val logs = mutableListOf<String>()
        val testWriter =
            object : LogWriter() {
                override fun log(
                    severity: Severity,
                    message: String,
                    tag: String,
                    throwable: Throwable?,
                ) {
                    if (severity == Severity.Error) {
                        logs.add(message)
                    }
                }
            }
        val logger =
            Logger(
                config = loggerConfigInit(testWriter),
                tag = "Test",
            )
        return logger to logs
    }

    /**
     * Creates a spied NavigationManagerImpl with an injected test logger.
     * This bypasses the static loggerCache completely.
     */
    private fun createTestNavigationManager(
        jobsManager: CoroutineJobsManager,
        testLogger: Logger,
    ): NavigationManagerImpl {
        val realManager = NavigationManagerImpl(jobsManager)
        val spy = spyk(realManager)
        every { spy.log } returns testLogger
        return spy
    }

    /**
     * Combined helper to create test setup with jobs manager, navigation manager, and log capture.
     * Reduces boilerplate in error-logging tests.
     */
    private fun createTestSetup(): Triple<TestCoroutineJobsManager, NavigationManagerImpl, MutableList<String>> {
        val jobsManager = TestCoroutineJobsManager(testDispatcher)
        val (testLogger, testLogs) = createTestLogger()
        val navigationManager = createTestNavigationManager(jobsManager, testLogger)
        return Triple(jobsManager, navigationManager, testLogs)
    }

    /**
     * Creates a spied NavigationManagerImpl with mocked isAtMainScreen() returning true.
     * This is used to test the "at main screen" conditional branches.
     */
    private fun createSpiedManagerAtMainScreen(
        jobsManager: CoroutineJobsManager,
        testLogger: Logger,
    ): NavigationManagerImpl {
        val realManager = NavigationManagerImpl(jobsManager)
        val spy = spyk(realManager)
        every { spy.log } returns testLogger
        every { spy.isAtMainScreen() } returns true
        return spy
    }
}
