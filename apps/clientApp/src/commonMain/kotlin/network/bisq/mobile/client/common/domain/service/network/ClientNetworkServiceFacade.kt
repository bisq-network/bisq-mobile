package network.bisq.mobile.client.common.domain.service.network

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.httpclient.HttpClientService
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.websocket.ConnectionState
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.domain.service.network.NetworkServiceFacade

class ClientNetworkServiceFacade(
    private val sensitiveSettingsRepository: SensitiveSettingsRepository,
    private val httpClientService: HttpClientService,
    private val webSocketClientService: WebSocketClientService,
    kmpTorService: KmpTorService,
) : NetworkServiceFacade(kmpTorService) {
    override val numConnections: StateFlow<Int> =
        webSocketClientService.connectionState
            .map { if (it is ConnectionState.Connected) 1 else -1 }
            .stateIn(
                serviceScope,
                SharingStarted.Lazily,
                -1,
            )

    override val allDataReceived: StateFlow<Boolean> =
        webSocketClientService.initialSubscriptionsReceivedData.stateIn(
            serviceScope,
            SharingStarted.Lazily,
            false,
        )

    override suspend fun isTorEnabled(): Boolean = sensitiveSettingsRepository.fetch().selectedProxyOption == BisqProxyOption.INTERNAL_TOR

    override suspend fun activate() {
        super.activate()
        httpClientService.activate()
        webSocketClientService.activate()
        // TODO implement gateway and endpoints to subscribe to number of connections of backend
    }

    override suspend fun deactivate() {
        super.deactivate()
        webSocketClientService.deactivate()
        httpClientService.deactivate()
    }
}
