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
) {
    companion object {
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
            )
    }
}
