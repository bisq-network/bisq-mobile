package network.bisq.mobile.node.common.domain.service.config

import bisq.bisq_easy.BisqEasyTradeAmountLimits
import network.bisq.mobile.node.common.domain.mapping.Mappings
import kotlin.test.Test
import kotlin.test.assertEquals

class NodeConfigServiceFacadeTest {
    @Test
    fun `tradeAmountLimits mirror the bisq2 core constants`() {
        val facade = NodeConfigServiceFacade()

        // The node is the single source of truth: the emitted VO must equal exactly what core exposes,
        // read straight from BisqEasyTradeAmountLimits with no duplicated literals.
        val limits = facade.tradeAmountLimits.value

        assertEquals(
            Mappings.FiatMapping.fromBisq2Model(BisqEasyTradeAmountLimits.DEFAULT_MIN_USD_TRADE_AMOUNT),
            limits.defaultMinUsdTradeAmount,
        )
        assertEquals(
            Mappings.FiatMapping.fromBisq2Model(BisqEasyTradeAmountLimits.MAX_USD_TRADE_AMOUNT),
            limits.maxUsdTradeAmount,
        )
        assertEquals(BisqEasyTradeAmountLimits.TOLERANCE, limits.tolerance)
        assertEquals(BisqEasyTradeAmountLimits.getRequiredReputationScorePerUsd(), limits.requiredReputationScorePerUsd)
    }
}
