package network.bisq.mobile.client.common.domain.service.config

import network.bisq.mobile.client.common.di.clientJson
import network.bisq.mobile.data.replicated.config.TradeAmountLimitsVO
import kotlin.test.Test
import kotlin.test.assertEquals

/** Decodes the trusted node's `GET /config/trade-amount-limits` body with the client's own `Json`. */
class TradeAmountLimitsDecodingTest {
    private fun body(minimumToleranceField: String) =
        """
        {
          "defaultMinUsdTradeAmount": {"id": "USD", "value": 60000, "code": "USD", "precision": 4, "lowPrecision": 2},
          "maxUsdTradeAmount": {"id": "USD", "value": 6000000, "code": "USD", "precision": 4, "lowPrecision": 2},
          "tolerance": 0.05,
          "requiredReputationScorePerUsd": 200.0
          $minimumToleranceField
        }
        """.trimIndent()

    @Test
    fun `decodes the minimum reputation score tolerance a 2_1_14 or newer node sends`() {
        val limits = clientJson.decodeFromString<TradeAmountLimitsVO>(body(""", "minimumReputationScoreTolerance": 300"""))

        assertEquals(300L, limits.minimumReputationScoreTolerance)
    }

    @Test
    fun `falls back to the bisq2 minimum tolerance for an older node without the field`() {
        val limits = clientJson.decodeFromString<TradeAmountLimitsVO>(body(""))

        assertEquals(TradeAmountLimitsVO.DEFAULT, limits)
    }
}
