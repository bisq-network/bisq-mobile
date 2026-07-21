package network.bisq.mobile.client.common.domain.service.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.data.replicated.config.TradeAmountLimitsVO
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.data.service.config.ConfigServiceFacade

/**
 * Client implementation of [ConfigServiceFacade].
 *
 * On [activate] it fetches the static config from the trusted node's `/config` endpoint and emits it.
 * The value always resolves to something usable via a layered fallback: the last fetched value stays
 * in [tradeAmountLimits] (in-session cache), and until a successful fetch it is the bundled
 * [TradeAmountLimitsVO.DEFAULT]. Older nodes that predate the endpoint simply fail the request and we
 * keep DEFAULT — no capability gate needed because DEFAULT is always safe.
 *
 * TODO: persist the last fetched value so it survives restarts and covers the offline-against-a-newer-
 *  node edge case (currently we re-fetch each launch and show DEFAULT until it returns).
 */
class ClientConfigServiceFacade(
    private val configApiGateway: ConfigApiGateway,
) : ServiceFacade(),
    ConfigServiceFacade {
    private val _tradeAmountLimits = MutableStateFlow(TradeAmountLimitsVO.DEFAULT)
    override val tradeAmountLimits: StateFlow<TradeAmountLimitsVO> = _tradeAmountLimits.asStateFlow()

    override suspend fun activate() {
        super<ServiceFacade>.activate()
        serviceScope.launch { fetchTradeAmountLimits() }
    }

    private suspend fun fetchTradeAmountLimits() {
        configApiGateway
            .getTradeAmountLimits()
            .onSuccess { limits ->
                log.d { "Fetched trade amount limits from node config" }
                _tradeAmountLimits.value = limits
            }.onFailure { e ->
                // Expected against older nodes without the endpoint, or while offline. Keep the last
                // good value (DEFAULT on first launch); consumers always have usable limits.
                log.d(e) { "Config fetch failed; keeping ${if (_tradeAmountLimits.value == TradeAmountLimitsVO.DEFAULT) "bundled default" else "cached value"}" }
            }
    }
}
