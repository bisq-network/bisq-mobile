package network.bisq.mobile.data.service.config

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.data.replicated.config.TradeAmountLimitsVO
import network.bisq.mobile.data.service.LifeCycleAware

/**
 * Single source of static bisq2 configuration for the app, so clients don't hardcode/duplicate
 * values that bisq2 core owns.
 *
 * The value always resolves to something usable: the bundled default, then a cached value, then the
 * live value from the node. The client fetches it over the `/config` endpoint; the node reads it
 * straight from bisq2 core.
 */
interface ConfigServiceFacade : LifeCycleAware {
    val tradeAmountLimits: StateFlow<TradeAmountLimitsVO>

    /**
     * Keys of the recent API features the paired node supports, from its `/config/capabilities`
     * manifest. Empty when the node predates the manifest (fail closed). Consumed by
     * [network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService].
     */
    val supportedFeatures: StateFlow<Set<String>>
}
