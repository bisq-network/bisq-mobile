package network.bisq.mobile.client.service.network_stats

import kotlinx.coroutines.Job
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.client.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.domain.data.replicated.network.NetworkStatsDto
import network.bisq.mobile.domain.service.network_stats.NetworkStatsServiceFacade
import network.bisq.mobile.domain.utils.Logging

class ClientNetworkStatsServiceFacade(
    private val apiGateway: NetworkStatsApiGateway,
    private val json: kotlinx.serialization.json.Json,
) : NetworkStatsServiceFacade(), Logging {

    private var networkStatsObserver: WebSocketEventObserver? = null
    private var job: Job? = null

    override fun activate() {
        super.activate()

        job?.cancel()
        job = launchIO {
            val result = apiGateway.getNetworkStats()
            result.onSuccess { stats ->
                _publishedProfilesCount.value = stats.publishedProfiles
                log.d { "Network stats loaded: ${stats.publishedProfiles} published profiles" }
            }.onFailure { error ->
                log.e(error) { "Failed to load network stats" }
            }
            
            try {
                networkStatsObserver = apiGateway.subscribeNetworkStats()
                networkStatsObserver?.webSocketEvent?.collect { webSocketEvent ->
                    if (webSocketEvent?.deferredPayload == null) {
                        return@collect
                    }
                    val webSocketEventPayload: WebSocketEventPayload<Map<String, NetworkStatsDto>> =
                        WebSocketEventPayload.from(json, webSocketEvent)
                    val payload = webSocketEventPayload.payload
                    _publishedProfilesCount.value = payload.values.sumOf { it.publishedProfiles }
                }
            } catch (e: Exception) {
                log.e(e) { "Failed to subscribe to network stats" }
            }
        }

        log.d { "ClientNetworkStatsServiceFacade activated" }
    }

    override fun deactivate() {
        networkStatsObserver = null
        job?.cancel()
        job = null
        super.deactivate()
        log.d { "ClientNetworkStatsServiceFacade deactivated" }
    }
}