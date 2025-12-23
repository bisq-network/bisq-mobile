package network.bisq.mobile.presentation.common.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Simple fake ForegroundServiceController for tests
class FakeForegroundServiceController : network.bisq.mobile.presentation.common.notification.ForegroundServiceController {
    private val _registered = mutableSetOf<Flow<*>>()
    private val mutex = Mutex()
    val registered: Set<Flow<*>> get() = runBlocking { mutex.withLock { _registered.toSet() } }

    override fun startService() {}
    override fun stopService() {}
    override fun <T> registerObserver(flow: Flow<T>, onStateChange: suspend (T) -> Unit) {
        runBlocking { mutex.withLock { _registered.add(flow) } }
    }

    override fun unregisterObserver(flow: Flow<*>) {
        runBlocking { mutex.withLock { _registered.remove(flow) } }
    }

    override fun unregisterObservers() { runBlocking { mutex.withLock { _registered.clear() } } }
    override fun isServiceRunning(): Boolean = false
    override fun dispose() {}
}

// Test helper that mirrors the registration logic from OpenTradesNotificationService
class TestRegistrationHelper(private val foregroundServiceController: FakeForegroundServiceController) {
    private val observedTradeIds = mutableSetOf<String>()
    private val perTradeFlows = mutableMapOf<String, MutableList<Flow<*>>>()
    private val stateMutex = Mutex()

    suspend fun addObserved(tradeId: String) = stateMutex.withLock { observedTradeIds.add(tradeId) }
    suspend fun removeObserved(tradeId: String) = stateMutex.withLock { observedTradeIds.remove(tradeId) }

    suspend fun <T> registerTradeFlowObserver(tradeId: String, flow: Flow<T>, onStateChange: suspend (T) -> Unit) {
        val changeFlow = flow.distinctUntilChanged().drop(1)

        // Register first
        foregroundServiceController.registerObserver(changeFlow, onStateChange)

        // Then add under mutex only if still observed
        val stillObserved = stateMutex.withLock {
            if (tradeId in observedTradeIds) {
                perTradeFlows.getOrPut(tradeId) { mutableListOf() }.add(changeFlow)
                true
            } else {
                false
            }
        }

        if (!stillObserved) {
            foregroundServiceController.unregisterObserver(changeFlow)
        }
    }

    suspend fun hasFlowForTrade(tradeId: String): Boolean = stateMutex.withLock { perTradeFlows[tradeId]?.isNotEmpty() ?: false }
}

@OptIn(ExperimentalCoroutinesApi::class)
class OpenTradesNotificationServiceRaceTest {

    @Test
    fun register_when_not_observed_unregisters_immediately() = runTest {
        val fake = FakeForegroundServiceController()
        val helper = TestRegistrationHelper(fake)

        val flow = MutableStateFlow(0)

        helper.registerTradeFlowObserver("trade1", flow) { /* no-op */ }

        // After registration, since trade1 was not in observedTradeIds, the flow should have been unregistered
        assertTrue(fake.registered.isEmpty(), "Expected no registered observers when trade is not observed")
    }

    @Test
    fun register_when_observed_keeps_registration() = runTest {
        val fake = FakeForegroundServiceController()
        val helper = TestRegistrationHelper(fake)

        val flow = MutableStateFlow(0)

        helper.addObserved("trade2")

        helper.registerTradeFlowObserver("trade2", flow) { /* no-op */ }

        // Since trade2 was observed, the registered set should contain the flow
        assertEquals(1, fake.registered.size, "Expected one registered observer when trade is observed")
        assertTrue(helper.hasFlowForTrade("trade2"), "perTradeFlows should contain the flow for trade2")
    }
}
