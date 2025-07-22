package network.bisq.mobile.client.service.network_stats

import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.client.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.domain.service.network_stats.ProfileStatsServiceFacade
import network.bisq.mobile.domain.utils.Logging

class ClientProfileStatsServiceFacade(
    private val apiGateway: UserProfileStats,
    private val json: kotlinx.serialization.json.Json,
) : ProfileStatsServiceFacade(), Logging {

    private var networkStatsObserver: WebSocketEventObserver? = null
    private var job: Job? = null
    private val statsMutex = Mutex()

    override fun activate() {
        super.activate()

        job?.cancel()
        job = launchIO {
            try {
                networkStatsObserver = apiGateway.subscribeStats()
                networkStatsObserver?.webSocketEvent?.collect { webSocketEvent ->
                    if (webSocketEvent?.deferredPayload == null) {
                        log.d { "Received WebSocket event with null payload, skipping" }
                        return@collect
                    }

                    try {
                        log.d { "Processing published profiled count event: ${webSocketEvent.deferredPayload}" }
                        val webSocketEventPayload: WebSocketEventPayload<Int> =
                                WebSocketEventPayload.from(json, webSocketEvent)
                        val publishedProfiles = webSocketEventPayload.payload
                        statsMutex.withLock {
                            _publishedProfilesCount.value = publishedProfiles
                        }
                    } catch (e: Exception) {
                        log.e(e) { "Failed to process WebSocket event" }
                    }
                }
            } catch (e: Exception) {
                networkStatsObserver = null
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