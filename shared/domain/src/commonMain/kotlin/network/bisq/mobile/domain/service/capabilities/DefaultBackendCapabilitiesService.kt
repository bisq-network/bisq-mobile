package network.bisq.mobile.domain.service.capabilities

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import network.bisq.mobile.data.service.config.ConfigServiceFacade

/**
 * Projects [ConfigServiceFacade.supportedFeatures] into typed [BackendCapabilities]. Used by both
 * apps: the client's config facade supplies the fetched `/config/capabilities` manifest, the node's
 * supplies the full set of features it runs — so feature gating works the same way in both.
 */
class DefaultBackendCapabilitiesService(
    configServiceFacade: ConfigServiceFacade,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : BackendCapabilitiesService {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    override val capabilities: StateFlow<BackendCapabilities> =
        configServiceFacade.supportedFeatures
            .map { BackendCapabilities(it) }
            .stateIn(scope, SharingStarted.Eagerly, BackendCapabilities.UNAVAILABLE)
}
