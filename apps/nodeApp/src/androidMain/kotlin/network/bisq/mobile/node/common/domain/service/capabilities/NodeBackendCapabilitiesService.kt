package network.bisq.mobile.node.common.domain.service.capabilities

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.service.capabilities.BackendCapabilities
import network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService

/**
 * Node-app implementation of [BackendCapabilitiesService].
 *
 * The bisq2 backend is embedded in-process, so every endpoint the mobile app
 * could probe is by definition reachable. All capabilities are unconditionally
 * enabled and [refresh] is a no-op — no probing needed.
 */
class NodeBackendCapabilitiesService : BackendCapabilitiesService {
    override val capabilities: StateFlow<BackendCapabilities> =
        MutableStateFlow(BackendCapabilities(hasClosedTradesApi = true)).asStateFlow()

    override suspend fun refresh() = Unit
}
