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
import network.bisq.mobile.domain.di.testModule
import network.bisq.mobile.domain.service.network.KmpTorService
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationBootstrapFacadeTest : KoinTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin { modules(testModule) }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
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
}

// Test subclass to expose protected functionality for unit testing only
private class TestFacade(
    kmpTorService: KmpTorService,
) : ApplicationBootstrapFacade(kmpTorService) {
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
