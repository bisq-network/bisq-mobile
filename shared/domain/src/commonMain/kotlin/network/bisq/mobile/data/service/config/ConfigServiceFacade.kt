package network.bisq.mobile.data.service.config

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.data.replicated.config.TradeAmountLimitsVO
import network.bisq.mobile.data.service.LifeCycleAware

/**
 * Single source of static bisq2 configuration for the app, so clients don't hardcode/duplicate
 * values that bisq2 core owns.
 *
 * The value always resolves to something usable: the bundled default, then a cached value, then the
 * live value from the node. The client fetches it over HTTP; the node reads it straight from bisq2
 * core.
 *
 * TODO: back [tradeAmountLimits] with a fetched+cached value (client) / a direct core read (node);
 *  currently both impls emit [TradeAmountLimitsVO.DEFAULT].
 */
interface ConfigServiceFacade : LifeCycleAware {
    val tradeAmountLimits: StateFlow<TradeAmountLimitsVO>
}
