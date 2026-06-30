package network.bisq.mobile.node.common.domain.service.bootstrap

import androidx.core.util.Supplier
import bisq.application.State
import bisq.common.observable.Observable
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import network.bisq.mobile.node.common.domain.service.bootstrap.NodeApplicationBootstrapFacade.BootstrapPhase
import network.bisq.mobile.test.coroutines.TestCoroutineJobsManager
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NodeApplicationBootstrapFacadeTest {
    private val testDispatcher = StandardTestDispatcher()
    private var previousOut: PrintStream? = null
    private var previousErr: PrintStream? = null

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        I18nSupport.setLanguage()
        startKoin {
            modules(
                module {
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                },
            )
        }
        // The FAILED path logs the startup error to stderr; capture stdout/stderr for the
        // lifetime of the test so diagnostic output stays contained (matching the base
        // ApplicationBootstrapFacadeTest).
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
    fun `maps INITIALIZE_APP state to INITIALIZE_APP phase`() =
        runTest(testDispatcher) {
            // Start from a different value so the assertion proves the INITIALIZE_APP branch ran.
            val (facade, stateObservable) = observe(initial = State.INITIALIZE_SERVICES)

            stateObservable.set(State.INITIALIZE_APP)

            assertEquals(BootstrapPhase.INITIALIZE_APP, facade.bootstrapPhase.value)
        }

    @Test
    fun `maps INITIALIZE_NETWORK state to phase and progress`() =
        runTest(testDispatcher) {
            val (facade, stateObservable) = observe()

            stateObservable.set(State.INITIALIZE_NETWORK)

            assertEquals(BootstrapPhase.INITIALIZE_NETWORK, facade.bootstrapPhase.value)
            assertEquals(0.5f, facade.progress.value)
        }

    @Test
    fun `maps INITIALIZE_SERVICES state to phase and progress`() =
        runTest(testDispatcher) {
            val (facade, stateObservable) = observe()

            stateObservable.set(State.INITIALIZE_SERVICES)

            assertEquals(BootstrapPhase.INITIALIZE_SERVICES, facade.bootstrapPhase.value)
            assertEquals(0.75f, facade.progress.value)
        }

    @Test
    fun `maps APP_INITIALIZED state to phase full progress and state`() =
        runTest(testDispatcher) {
            val (facade, stateObservable) = observe()

            stateObservable.set(State.APP_INITIALIZED)

            assertEquals(BootstrapPhase.APP_INITIALIZED, facade.bootstrapPhase.value)
            assertEquals(1f, facade.progress.value)
            assertEquals("splash.applicationServiceState.APP_INITIALIZED".i18n(), facade.state.value)
        }

    @Test
    fun `INITIALIZE_WALLET state does not change phase or progress`() =
        runTest(testDispatcher) {
            val (facade, stateObservable) = observe()
            stateObservable.set(State.INITIALIZE_NETWORK)

            stateObservable.set(State.INITIALIZE_WALLET)

            // INITIALIZE_WALLET is a no-op on the node; phase/progress stay where INITIALIZE_NETWORK left them.
            assertEquals(BootstrapPhase.INITIALIZE_NETWORK, facade.bootstrapPhase.value)
            assertEquals(0.5f, facade.progress.value)
        }

    @Test
    fun `FAILED state keeps last phase and flags bootstrap failed`() =
        runTest(testDispatcher) {
            val (facade, stateObservable) = observe()
            stateObservable.set(State.INITIALIZE_SERVICES)

            stateObservable.set(State.FAILED)

            // Failure must not regress the phase: it stays at the last reached stage so the splash
            // steps keep their progress. Failure is signalled separately via isBootstrapFailed.
            assertEquals(BootstrapPhase.INITIALIZE_SERVICES, facade.bootstrapPhase.value)
            assertTrue(facade.isBootstrapFailed.value)
            assertEquals("splash.applicationServiceState.FAILED".i18n(), facade.state.value)
        }

    @Test
    fun `progression through all states ends initialized with full progress`() =
        runTest(testDispatcher) {
            val (facade, stateObservable) = observe()

            stateObservable.set(State.INITIALIZE_NETWORK)
            stateObservable.set(State.INITIALIZE_SERVICES)
            stateObservable.set(State.APP_INITIALIZED)

            assertEquals(BootstrapPhase.APP_INITIALIZED, facade.bootstrapPhase.value)
            assertEquals(1f, facade.progress.value)
        }

    @Test
    fun `deactivate stops observing further state changes`() =
        runTest(testDispatcher) {
            val (facade, stateObservable) = observe()
            stateObservable.set(State.INITIALIZE_NETWORK)
            assertEquals(BootstrapPhase.INITIALIZE_NETWORK, facade.bootstrapPhase.value)

            facade.deactivate()

            // After unbinding the observer, later state changes must not update the phase.
            stateObservable.set(State.INITIALIZE_SERVICES)
            assertEquals(BootstrapPhase.INITIALIZE_NETWORK, facade.bootstrapPhase.value)
        }

    /**
     * Builds a facade wired to a real Bisq2 [Observable] of [State], starts observing, and returns
     * both so the test can drive transitions via [Observable.set] (observers fire synchronously).
     * [initial] is the value present when observation starts (the observer fires immediately with
     * it); keep it a real [State] to avoid a null going through the production `when`.
     */
    private fun observe(initial: State = State.INITIALIZE_APP): Pair<NodeApplicationBootstrapFacade, Observable<State>> {
        val stateObservable = Observable(initial)
        val provider = mockk<AndroidApplicationService.Provider>(relaxed = true)
        every { provider.state } returns Supplier { stateObservable }
        val applicationService = mockk<AndroidApplicationService>(relaxed = true)
        every { provider.applicationService } returns applicationService
        every { applicationService.startupErrorMessage } returns Observable("test startup error")

        val facade = NodeApplicationBootstrapFacade(provider, mockk<KmpTorService>(relaxed = true))
        facade.observeApplicationState()
        return facade to stateObservable
    }
}
