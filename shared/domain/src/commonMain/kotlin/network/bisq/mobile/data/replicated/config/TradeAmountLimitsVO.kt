package network.bisq.mobile.data.replicated.config

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.data.replicated.common.monetary.FiatVOFactory.fromFaceValue

/**
 * Static Bisq Easy trade-amount configuration. These values are fixed for a given bisq2 core/api
 * version and are the same across the P2P network. The client fetches them from the node's `/config`
 * endpoint (see ConfigServiceFacade); [DEFAULT] is the bundled fallback for older/offline nodes.
 */
@Serializable
data class TradeAmountLimitsVO(
    val defaultMinUsdTradeAmount: FiatVO,
    val maxUsdTradeAmount: FiatVO,
    val tolerance: Double,
    val requiredReputationScorePerUsd: Double,
    // Minimum extra reputation points the tolerance gives a positive score; covers whole-USD rounding
    // at low scores. Nodes before 2.1.14 do not send it, so it defaults to the bisq2 value.
    val minimumReputationScoreTolerance: Long = DEFAULT_MINIMUM_REPUTATION_SCORE_TOLERANCE,
) {
    companion object {
        const val DEFAULT_MINIMUM_REPUTATION_SCORE_TOLERANCE = 200L

        /**
         * Bundled fallback, mirrored from bisq2 core's `BisqEasyTradeAmountLimits`. Used until a
         * successful fetch, and as the permanent fallback for nodes that predate the endpoint.
         */
        val DEFAULT =
            TradeAmountLimitsVO(
                defaultMinUsdTradeAmount = FiatVOFactory.fromFaceValue(6.0, "USD"),
                maxUsdTradeAmount = FiatVOFactory.fromFaceValue(600.0, "USD"),
                tolerance = 0.05,
                requiredReputationScorePerUsd = 200.0,
                minimumReputationScoreTolerance = DEFAULT_MINIMUM_REPUTATION_SCORE_TOLERANCE,
            )
    }
}
