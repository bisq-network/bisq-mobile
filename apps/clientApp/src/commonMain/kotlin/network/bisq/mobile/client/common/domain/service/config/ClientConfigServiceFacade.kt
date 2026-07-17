package network.bisq.mobile.client.common.domain.service.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.data.replicated.config.TradeAmountLimitsVO
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.data.service.config.ConfigServiceFacade

/**
 * Client implementation of [ConfigServiceFacade].
 *
 * TODO: fetch the config from the trusted node's config endpoint, cache it (survives restarts), and
 *  refresh it in the background; fall back to the cached value, then to [TradeAmountLimitsVO.DEFAULT]
 *  when the node is unreachable or predates the endpoint. For now it emits the bundled default.
 */
class ClientConfigServiceFacade :
    ServiceFacade(),
    ConfigServiceFacade {
    private val _tradeAmountLimits = MutableStateFlow(TradeAmountLimitsVO.DEFAULT)
    override val tradeAmountLimits: StateFlow<TradeAmountLimitsVO> = _tradeAmountLimits.asStateFlow()
}
