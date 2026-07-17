package network.bisq.mobile.node.common.domain.service.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.data.replicated.config.TradeAmountLimitsVO
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.data.service.config.ConfigServiceFacade

/**
 * Node implementation of [ConfigServiceFacade].
 *
 * TODO: read the values straight from bisq2 core's BisqEasyTradeAmountLimits (no HTTP, no
 *  hardcoding) so the node is the single source of truth. For now it emits the bundled default.
 */
class NodeConfigServiceFacade :
    ServiceFacade(),
    ConfigServiceFacade {
    private val _tradeAmountLimits = MutableStateFlow(TradeAmountLimitsVO.DEFAULT)
    override val tradeAmountLimits: StateFlow<TradeAmountLimitsVO> = _tradeAmountLimits.asStateFlow()
}
