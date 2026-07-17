package network.bisq.mobile.client.common.domain.service.config

import network.bisq.mobile.data.replicated.config.TradeAmountLimitsVO
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientConfigServiceFacadeTest {
    @Test
    fun `tradeAmountLimits emits the bundled default`() {
        val facade = ClientConfigServiceFacade()

        // Until the config endpoint exists, the client must expose the bundled fallback so
        // trade-amount math is unchanged from when these values were hardcoded.
        assertEquals(TradeAmountLimitsVO.DEFAULT, facade.tradeAmountLimits.value)
    }
}
