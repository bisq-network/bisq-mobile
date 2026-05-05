package network.bisq.mobile.domain.service.bootstrap

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.di.testModule
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.network.KmpTorService
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationBootstrapFacadeTest : KoinTest {
    private val testDispatcher = StandardTestDispatcher()
    private var previousOut: PrintStream? = null
    private var previousErr: PrintStream? = null

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin { modules(testModule) }
        // Failure-path tests intentionally drive the facade through Tor failures; the
        // production code logs the staged failure to stdout/stderr. Capture both for the
        // lifetime of the test so that diagnostic output (in particular anything that
        // could pattern-match Gradle's test-worker-failure heuristic) is contained.
        previousOut = System.out
        previousErr = System.err
        System.setOut(PrintStream(ByteArrayOutputStream()))
        System.setErr(PrintStream(ByteArrayOutputStream()))
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
        previousOut?.let { System.setOut(it) }
        previousErr?.let { System.setErr(it) }
    }

    @Test
    fun `setters update flows`() =
        runTest(testDispatcher) {
            val kmpTorService = mockk<KmpTorService>(relaxed = true)
            val facade = TestFacade(kmpTorService)

            facade.setState("loading")
            facade.setProgress(0.42f)

            assertEquals("loading", facade.state.value)
            assertEquals(0.42f, facade.progress.value)
        }

    @Test
    fun `timeout job shows dialog after timeout`() =
        runTest(testDispatcher) {
            val kmpTorService = mockk<KmpTorService>(relaxed = true)
            val facade = TestFacade(kmpTorService)

            facade.startTimeoutForStagePublic("stage")

            // advance to timeout duration (90s)
            testScheduler.advanceTimeBy(90_000)
            advanceUntilIdle()

            assertTrue(facade.isTimeoutDialogVisible.value)
        }

    @Test
    fun `extendTimeout delays showing dialog`() =
        runTest(testDispatcher) {
            val kmpTorService = mockk<KmpTorService>(relaxed = true)
            val facade = TestFacade(kmpTorService)
            assertFalse(facade.isTimeoutDialogVisible.value)
            facade.startTimeoutForStagePublic("stage-extend")
            // extend immediately -> doubles timeout (180s)
            facade.extendTimeout()
            // advance by original timeout -> should NOT show yet
            testScheduler.advanceTimeBy(90_000)
            assertFalse(facade.isTimeoutDialogVisible.value)

            // advance the remaining time -> dialog should now be visible
            testScheduler.advanceTimeBy(100_000)
            assertTrue(facade.isTimeoutDialogVisible.value)
        }

    @Test
    fun `onInitialized cancels timeout job`() =
        runTest(testDispatcher) {
            val kmpTorService = mockk<KmpTorService>(relaxed = true)
            val facade = TestFacade(kmpTorService)

            facade.startTimeoutForStagePublic("stage-cancel")
            // mark initialized which should cancel timeout
            facade.onInitializedPublic()

            testScheduler.advanceTimeBy(90_000)
            advanceUntilIdle()

            assertFalse(facade.isTimeoutDialogVisible.value)
        }

    @Test
    fun `activate resets transient state after failure cycle`() =
        runTest(testDispatcher) {
            val kmpTorService = mockk<KmpTorService>(relaxed = true)
            val facade = TestFacade(kmpTorService)

            // Simulate a failure cycle: set all transient state to non-default values
            facade.setTorBootstrapFailed(true)
            facade.setBootstrapFailed(true)
            facade.setTimeoutDialogVisible(true)
            facade.setShouldShowProgressToast(true)
            facade.setCurrentBootstrapStage("some-stage")
            facade.onInitializedPublic() // sets bootstrapSuccessful = true
            facade.setState("failed state")
            facade.setProgress(0.75f)

            // Verify the failure state is set
            assertTrue(facade.torBootstrapFailed.value)
            assertTrue(facade.isBootstrapFailed.value)

            // Deactivate then activate (simulating retry)
            facade.deactivate()
            facade.activate()
            advanceUntilIdle()

            // All transient state should be reset
            assertFalse(facade.torBootstrapFailed.value, "torBootstrapFailed should be reset")
            assertFalse(facade.isBootstrapFailed.value, "isBootstrapFailed should be reset")
            assertFalse(facade.isTimeoutDialogVisible.value, "isTimeoutDialogVisible should be reset")
            assertFalse(facade.shouldShowProgressToast.value, "shouldShowProgressToast should be reset")
            assertEquals("", facade.currentBootstrapStage.value, "currentBootstrapStage should be reset")
        }

    @Test
    fun `activate resets bootstrapSuccessful so timeouts can fire again`() =
        runTest(testDispatcher) {
            val kmpTorService = mockk<KmpTorService>(relaxed = true)
            val facade = TestFacade(kmpTorService)

            // Complete a successful bootstrap
            facade.onInitializedPublic() // sets bootstrapSuccessful = true

            // startTimeoutForStage should return early when bootstrapSuccessful is true
            facade.startTimeoutForStagePublic("stage-after-success")
            testScheduler.advanceTimeBy(90_001)
            advanceUntilIdle()
            assertFalse(facade.isTimeoutDialogVisible.value, "Timeout should not fire after successful bootstrap")

            // Deactivate and reactivate
            facade.deactivate()
            facade.activate()
            advanceUntilIdle()

            // Now timeouts should work again (bootstrapSuccessful was reset)
            facade.startTimeoutForStagePublic("stage-after-reactivate")
            testScheduler.advanceTimeBy(90_001)
            advanceUntilIdle()
            assertTrue(facade.isTimeoutDialogVisible.value, "Timeout should fire after reactivation")
        }

    @Test
    fun `tor bootstrap progress updates applied to state`() =
        runTest(testDispatcher) {
            val kmpTorService = mockk<KmpTorService>(relaxed = true)
            val stateFlow = MutableStateFlow<KmpTorService.TorState>(KmpTorService.TorState.Stopped())
            val progressFlow = MutableStateFlow(0)
            every { kmpTorService.state } returns stateFlow
            every { kmpTorService.bootstrapProgress } returns progressFlow
            val facade = TestFacade(kmpTorService)

            // Start observing
            facade.observeTorStatePublic()

            // Emit Starting state via flow
            stateFlow.emit(KmpTorService.TorState.Starting)
            advanceUntilIdle()

            // Starting should set a small progress value
            assertEquals(0.1f, facade.progress.value)

            val before = facade.state.value

            // Emit bootstrap progress update and verify facade state changed
            progressFlow.emit(42)
            advanceUntilIdle()
            assertTrue(facade.state.value != before && facade.state.value.contains("42"))

            // Emit Started state -> progress should update to 0.25f and further progress updates ignored
            stateFlow.emit(KmpTorService.TorState.Started)
            advanceUntilIdle()
            assertEquals(0.25f, facade.progress.value)

            val afterStarted = facade.state.value
            progressFlow.emit(99)
            advanceUntilIdle()
            assertEquals(afterStarted, facade.state.value)
        }

    @Test
    fun `tor failure within grace period does not show failure dialog`() =
        runTest(testDispatcher) {
            val kmpTorService = mockk<KmpTorService>(relaxed = true)
            val stateFlow = MutableStateFlow<KmpTorService.TorState>(KmpTorService.TorState.Stopped())
            val progressFlow = MutableStateFlow(0)
            every { kmpTorService.state } returns stateFlow
            every { kmpTorService.bootstrapProgress } returns progressFlow
            val facade = TestFacade(kmpTorService)

            facade.fakeTimeMillis = 1_000_000L
            facade.observeTorStatePublic()

            // Tor starts
            stateFlow.emit(KmpTorService.TorState.Starting)
            advanceUntilIdle()

            // Tor fails after 10 seconds (within 60s grace period)
            facade.fakeTimeMillis = 1_010_000L
            stateFlow.emit(KmpTorService.TorState.Stopped(RuntimeException("Circuit build timeout")))
            advanceUntilIdle()

            assertFalse(facade.torBootstrapFailed.value, "Tor failure within grace period should not show dialog")
        }

    @Test
    fun `tor failure after grace period shows failure dialog`() =
        runTest(testDispatcher) {
            val kmpTorService = mockk<KmpTorService>(relaxed = true)
            val stateFlow = MutableStateFlow<KmpTorService.TorState>(KmpTorService.TorState.Stopped())
            val progressFlow = MutableStateFlow(0)
            every { kmpTorService.state } returns stateFlow
            every { kmpTorService.bootstrapProgress } returns progressFlow
            val facade = TestFacade(kmpTorService)

            facade.fakeTimeMillis = 1_000_000L
            facade.observeTorStatePublic()

            // Tor starts
            stateFlow.emit(KmpTorService.TorState.Starting)
            advanceUntilIdle()

            // Tor fails after 65 seconds (past 60s grace period)
            facade.fakeTimeMillis = 1_065_000L
            stateFlow.emit(KmpTorService.TorState.Stopped(RuntimeException("Tor failed permanently")))
            advanceUntilIdle()

            assertTrue(facade.torBootstrapFailed.value, "Tor failure after grace period should show dialog")
        }
}

// Test subclass to expose protected functionality for unit testing only
private class TestFacade(
    kmpTorService: KmpTorService,
) : ApplicationBootstrapFacade(kmpTorService) {
    var fakeTimeMillis: Long = 0L

    override fun currentTimeMillis(): Long = fakeTimeMillis

    fun startTimeoutForStagePublic(
        stageName: String = state.value,
        extendedTimeout: Boolean = false,
    ) {
        startTimeoutForStage(stageName, extendedTimeout)
    }

    fun onInitializedPublic() {
        onInitialized()
    }

    fun observeTorStatePublic() {
        observeTorState()
    }
}
