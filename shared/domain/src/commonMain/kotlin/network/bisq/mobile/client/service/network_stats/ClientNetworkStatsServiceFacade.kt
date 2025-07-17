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
                    log.d { "Processing network stats event: ${webSocketEvent.deferredPayload}" }
                    val webSocketEventPayload: WebSocketEventPayload<NetworkStatsDto> =
                        WebSocketEventPayload.from(json, webSocketEvent)
                    val payload = webSocketEventPayload.payload
                    log.d { "Updating published profiles count from ${_publishedProfilesCount.value} to ${payload.publishedProfiles}" }
                    _publishedProfilesCount.value = payload.publishedProfiles
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