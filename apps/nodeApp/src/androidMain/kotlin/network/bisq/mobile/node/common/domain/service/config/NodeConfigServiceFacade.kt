package network.bisq.mobile.node.common.domain.service.config

import bisq.bisq_easy.BisqEasyTradeAmountLimits
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.data.replicated.config.TradeAmountLimitsVO
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.data.service.config.ConfigServiceFacade
import network.bisq.mobile.domain.service.capabilities.Feature
import network.bisq.mobile.node.common.domain.mapping.Mappings

/**
 * Node implementation of [ConfigServiceFacade].
 *
 * Reads the values straight from bisq2 core's [BisqEasyTradeAmountLimits] (the same code the node
 * runs), so the node is the single source of truth — no HTTP, no duplicated literals. The client
 * fetches the equivalent values over the `/config` endpoint backed by the same core class.
 *
 * The node runs the current core version, so it supports every known [Feature] — [supportedFeatures]
 * is the full key set, and the shared capabilities service gates nothing on the node app.
 */
class NodeConfigServiceFacade :
    ServiceFacade(),
    ConfigServiceFacade {
    private val _tradeAmountLimits =
        MutableStateFlow(
            TradeAmountLimitsVO(
                defaultMinUsdTradeAmount = Mappings.FiatMapping.fromBisq2Model(BisqEasyTradeAmountLimits.DEFAULT_MIN_USD_TRADE_AMOUNT),
                maxUsdTradeAmount = Mappings.FiatMapping.fromBisq2Model(BisqEasyTradeAmountLimits.MAX_USD_TRADE_AMOUNT),
                tolerance = BisqEasyTradeAmountLimits.TOLERANCE,
                requiredReputationScorePerUsd = BisqEasyTradeAmountLimits.getRequiredReputationScorePerUsd(),
            ),
        )
    override val tradeAmountLimits: StateFlow<TradeAmountLimitsVO> = _tradeAmountLimits.asStateFlow()

    override val supportedFeatures: StateFlow<Set<String>> =
        MutableStateFlow(Feature.entries.map { it.key }.toSet()).asStateFlow()
}
