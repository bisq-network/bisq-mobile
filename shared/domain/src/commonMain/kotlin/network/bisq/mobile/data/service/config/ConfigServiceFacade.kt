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
     * manifest. When the manifest is absent the client falls back to
     * [network.bisq.mobile.domain.service.capabilities.Feature.LEGACY_BASELINE_KEYS] so pre-manifest
     * features aren't lost; newer/unknown features fail closed. Consumed by
     * [network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService].
     */
    val supportedFeatures: StateFlow<Set<String>>
}
