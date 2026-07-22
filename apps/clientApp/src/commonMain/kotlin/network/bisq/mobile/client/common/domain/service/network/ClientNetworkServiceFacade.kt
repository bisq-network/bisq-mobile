package network.bisq.mobile.client.common.domain.service.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.httpclient.HttpClientService
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.websocket.ConnectionState
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.data.service.network.NetworkServiceFacade

class ClientNetworkServiceFacade(
    private val sensitiveSettingsRepository: SensitiveSettingsRepository,
    private val httpClientService: HttpClientService,
    private val webSocketClientService: WebSocketClientService,
    private val networkApiGateway: NetworkApiGateway,
    private val json: Json,
    kmpTorService: KmpTorService,
    applicationBootstrapFacade: ApplicationBootstrapFacade,
) : NetworkServiceFacade(kmpTorService, applicationBootstrapFacade) {
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

    private val _networkInfo = MutableStateFlow<NetworkInfoDto?>(null)

    /** Latest network snapshot pushed by the trusted node over the NETWORK_INFO subscription, or null until the first event. */
    val networkInfo: StateFlow<NetworkInfoDto?> = _networkInfo.asStateFlow()

    override suspend fun isTorEnabled(): Boolean = sensitiveSettingsRepository.fetch().selectedProxyOption == BisqProxyOption.INTERNAL_TOR

    override suspend fun activate() {
        super.activate()
        httpClientService.activate()
        webSocketClientService.activate()

        subscribeNetworkInfo()
    }

    private fun subscribeNetworkInfo() {
        serviceScope.launch(Dispatchers.Default) {
            val observer = networkApiGateway.subscribeNetworkInfo()
            observer.webSocketEvent.collect { event ->
                try {
                    if (event?.deferredPayload == null) {
                        return@collect
                    }
                    val payload: WebSocketEventPayload<NetworkInfoDto> = WebSocketEventPayload.from(json, event)
                    val info = payload.payload
                    _networkInfo.value = info
                    log.i {
                        "NETWORK_INFO received: torRunning=${info.torRunning}, " +
                            "allDataReceived=${info.allDataReceived}, myAddress=${info.myAddress}, keyId=${info.keyId}, " +
                            "connections=${info.connections.size}"
                    }
                } catch (e: Exception) {
                    log.e(e) { "Failed to parse NETWORK_INFO payload" }
                }
            }
        }
    }

    override suspend fun deactivate() {
        super.deactivate()
        webSocketClientService.deactivate()
        httpClientService.deactivate()
    }
}
