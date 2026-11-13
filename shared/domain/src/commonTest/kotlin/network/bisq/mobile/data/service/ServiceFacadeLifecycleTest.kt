package network.bisq.mobile.data.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.di.testModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceFacadeLifecycleTest : KoinTest {
    private val testDispatcher = StandardTestDispatcher()

    private class TestFacade : ServiceFacade() {
        val source = MutableStateFlow(0)
        val held = MutableStateFlow<List<Int>>(emptyList())
        var clearStateCalls = 0
        var scopeJobCompletedAtClearState: Boolean? = null
        private var activeScopeJob: Job? = null

        val doubled: StateFlow<Int> =
            source.map { it * 2 }.stateIn(sharingScope, SharingStarted.WhileSubscribed(), 0)

        override suspend fun activate() {
            super.activate()
            activeScopeJob = serviceScope.coroutineContext[Job]
            serviceScope.launch {
                source.collect { value -> held.value = held.value + value }
            }
        }

        override suspend fun clearState() {
            clearStateCalls++
            scopeJobCompletedAtClearState = activeScopeJob?.isCompleted
            held.value = emptyList()
        }
    }

    private lateinit var facade: TestFacade

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin { modules(testModule) }
        facade = TestFacade()
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `deactivate clears the state once the scope's work has finished`() =
        runTest(testDispatcher) {
            facade.activate()
            advanceUntilIdle()
            facade.source.value = 1
            advanceUntilIdle()

            facade.deactivate()
            facade.source.value = 2
            advanceUntilIdle()

            assertEquals(1, facade.clearStateCalls)
            assertEquals(true, facade.scopeJobCompletedAtClearState)
            assertTrue(facade.held.value.isEmpty())
        }

    @Test
    fun `deactivating a facade that never activated still clears its state`() =
        runTest(testDispatcher) {
            facade.held.value = listOf(1)

            facade.deactivate()

            assertEquals(1, facade.clearStateCalls)
            assertTrue(facade.held.value.isEmpty())
        }

    @Test
    fun `a flow derived on the sharing scope keeps following its source after a restart`() =
        runTest(testDispatcher) {
            backgroundScope.launch(testDispatcher) { facade.doubled.collect {} }
            facade.activate()
            advanceUntilIdle()

            facade.deactivate()
            facade.activate()
            facade.source.value = 21
            advanceUntilIdle()

            assertEquals(42, facade.doubled.value)
        }
}
