package network.bisq.mobile.client.common.domain.service.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.httpclient.HttpClientService
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.websocket.ConnectionState
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.collectPayloads
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.data.service.network.NetworkServiceFacade
import network.bisq.mobile.domain.coroutines.DispatcherProvider

class ClientNetworkServiceFacade(
    private val sensitiveSettingsRepository: SensitiveSettingsRepository,
    private val httpClientService: HttpClientService,
    private val webSocketClientService: WebSocketClientService,
    private val networkApiGateway: NetworkApiGateway,
    private val json: Json,
    kmpTorService: KmpTorService,
    applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val dispatcherProvider: DispatcherProvider,
) : NetworkServiceFacade(kmpTorService, applicationBootstrapFacade) {
    override val numConnections: StateFlow<Int> =
        webSocketClientService.connectionState
            .map { if (it is ConnectionState.Connected) 1 else -1 }
            .stateIn(
                serviceScope,
                SharingStarted.Lazily,
                -1,
            )

    private val _networkInfo = MutableStateFlow<NetworkInfoDto?>(null)

    /** Latest network snapshot pushed by the trusted node over the NETWORK_INFO subscription, or null until the first event. */
    val networkInfo: StateFlow<NetworkInfoDto?> = _networkInfo.asStateFlow()

    // Outlives deactivate(): a StateFlow shared in serviceScope stops updating once that scope is cancelled.
    private val sharingScope = CoroutineScope(SupervisorJob() + dispatcherProvider.default)

    /**
     * The first data of the banner subscriptions, plus the node's own sync flag: until it has synced,
     * the node answers with data it has not finished loading. A node that refuses the NETWORK_INFO
     * subscription, or does not answer it before the subscribe timeout, counts on the first data alone.
     */
    override val allDataReceived: StateFlow<Boolean> =
        combine(
            webSocketClientService.initialSubscriptionsReceivedData,
            _networkInfo,
            webSocketClientService.failedSubscriptionTopics,
        ) { firstDataReceived, info, failedTopics ->
            firstDataReceived && (info?.allDataReceived ?: (Topic.NETWORK_INFO in failedTopics))
        }.stateIn(sharingScope, SharingStarted.Lazily, false)

    override suspend fun isTorEnabled(): Boolean = sensitiveSettingsRepository.fetch().selectedProxyOption == BisqProxyOption.INTERNAL_TOR

    override suspend fun activate() {
        super.activate()
        httpClientService.activate()
        webSocketClientService.activate()

        // A restart or a reconnect must not pair the new first data with the node's previous answer.
        _networkInfo.value = null
        serviceScope.launch(dispatcherProvider.default) {
            webSocketClientService.connectionState
                .filter { it is ConnectionState.Disconnected }
                .collect { _networkInfo.value = null }
        }
        subscribeNetworkInfo()
    }

    private fun subscribeNetworkInfo() {
        serviceScope.launch(dispatcherProvider.default) {
            val observer = networkApiGateway.subscribeNetworkInfo()
            observer.collectPayloads<NetworkInfoDto>(json, dispatcherProvider.default) { info, _ ->
                _networkInfo.value = info
                log.i {
                    "NETWORK_INFO received: torRunning=${info.torRunning}, " +
                        "allDataReceived=${info.allDataReceived}, connections=${info.connections.size}"
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
