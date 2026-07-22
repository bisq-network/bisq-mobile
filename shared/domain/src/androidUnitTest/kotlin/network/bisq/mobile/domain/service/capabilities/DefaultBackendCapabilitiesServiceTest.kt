package network.bisq.mobile.domain.service.capabilities

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.replicated.config.TradeAmountLimitsVO
import network.bisq.mobile.data.service.config.ConfigServiceFacade
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultBackendCapabilitiesServiceTest {
    private val featuresFlow = MutableStateFlow<Set<String>>(emptySet())
    private val configServiceFacade =
        object : ConfigServiceFacade {
            override val tradeAmountLimits: StateFlow<TradeAmountLimitsVO> = MutableStateFlow(TradeAmountLimitsVO.DEFAULT)
            override val supportedFeatures: StateFlow<Set<String>> = featuresFlow

            override suspend fun activate() = Unit

            override suspend fun deactivate() = Unit
        }

    private fun service() = DefaultBackendCapabilitiesService(configServiceFacade, UnconfinedTestDispatcher())

    @Test
    fun `maps supported feature keys into isSupported`() =
        runTest {
            featuresFlow.value = setOf(Feature.CLOSED_TRADES.key)

            val capabilities = service().capabilities.value

            assertTrue(capabilities.isSupported(Feature.CLOSED_TRADES))
            assertEquals(setOf(Feature.CLOSED_TRADES.key), capabilities.supportedFeatures)
        }

    @Test
    fun `empty manifest reports nothing supported`() =
        runTest {
            featuresFlow.value = emptySet()

            assertFalse(service().capabilities.value.isSupported(Feature.CLOSED_TRADES))
        }

    @Test
    fun `reacts to manifest changes`() =
        runTest {
            val service = service()
            assertFalse(service.capabilities.value.isSupported(Feature.CLOSED_TRADES))

            featuresFlow.value = setOf(Feature.CLOSED_TRADES.key)

            assertTrue(service.capabilities.value.isSupported(Feature.CLOSED_TRADES))
        }
}
