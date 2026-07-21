package network.bisq.mobile.client.common.domain.service.config

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.data.replicated.config.TradeAmountLimitsVO
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ClientConfigServiceFacadeTest : ClientKoinIntegrationTestBase() {
    private val apiGateway: ConfigApiGateway = mockk(relaxed = true)
    private lateinit var facade: ClientConfigServiceFacade

    override fun onSetup() {
        facade = ClientConfigServiceFacade(apiGateway)
    }

    @Test
    fun `exposes the bundled default before any fetch`() {
        // Consumers must always have usable limits; before activate() the value is the bundled default
        // so trade-amount math matches the previously-hardcoded values.
        assertEquals(TradeAmountLimitsVO.DEFAULT, facade.tradeAmountLimits.value)
    }

    @Test
    fun `activate fetches the limits from the node and emits them`() =
        runTest {
            // A value distinct from DEFAULT so the assertion proves the fetched value was applied.
            val fetched = TradeAmountLimitsVO.DEFAULT.copy(tolerance = 0.1, requiredReputationScorePerUsd = 150.0)
            coEvery { apiGateway.getTradeAmountLimits() } returns Result.success(fetched)

            facade.activate()
            advanceUntilIdle()

            assertEquals(fetched, facade.tradeAmountLimits.value)
        }

    @Test
    fun `activate keeps the bundled default when the fetch fails`() =
        runTest {
            // Older nodes without the endpoint (or an offline node) fail the request; the facade must
            // fall back rather than surface an unusable value.
            coEvery { apiGateway.getTradeAmountLimits() } returns Result.failure(RuntimeException("404"))

            facade.activate()
            advanceUntilIdle()

            assertEquals(TradeAmountLimitsVO.DEFAULT, facade.tradeAmountLimits.value)
        }
}
