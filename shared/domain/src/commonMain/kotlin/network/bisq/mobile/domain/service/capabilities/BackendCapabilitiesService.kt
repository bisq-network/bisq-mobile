package network.bisq.mobile.domain.service.capabilities

import kotlinx.coroutines.flow.StateFlow

/**
 * Typed feature-gating surface for presenters. Backed by the features the trusted node advertises
 * (in the connect handshake, or via the older `/config/capabilities` manifest), which
 * [network.bisq.mobile.data.service.config.ConfigServiceFacade] publishes at bootstrap and caches per
 * node version. A node that does not advertise a feature — or is too old to advertise any — reports
 * it unsupported (fail closed).
 *
 * ## Gating a feature
 * 1. Add a key to [Feature] matching bisq2's `ApiFeature`.
 * 2. Gate UI on `capabilities.value.isSupported(Feature.X)` (or map the flow) — no version checks.
 */
interface BackendCapabilitiesService {
    val capabilities: StateFlow<BackendCapabilities>
}
