package network.bisq.mobile.client.network.presentation.network

import io.ktor.http.parseUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.service.network.ClientConnectivityService
import network.bisq.mobile.client.common.domain.service.network.ClientNetworkServiceFacade
import network.bisq.mobile.client.common.presentation.navigation.ClientNavRoute
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.components.network.NetworkHealthState
import network.bisq.mobile.presentation.main.MainPresenter

class ClientNetworkOverviewPresenter(
    private val networkServiceFacade: ClientNetworkServiceFacade,
    private val sensitiveSettingsRepository: SensitiveSettingsRepository,
    private val connectivityService: ClientConnectivityService,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(ClientNetworkOverviewUiState())
    val uiState: StateFlow<ClientNetworkOverviewUiState> = _uiState.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()

        // Latency and the trusted-node config are point-in-time reads. Round-trip time is only
        // recorded on REST-over-WS requests (not health checks or subscriptions), and this screen
        // issues none, so latency does not change while it is open — read it once on attach.
        presenterScope.launch {
            val settings = sensitiveSettingsRepository.fetch()
            _uiState.update {
                it.copy(
                    trustedNodeHost = parseUrl(settings.bisqApiUrl)?.host,
                    isTorRouted = settings.selectedProxyOption.isTorProxyOption,
                    latencyMs = connectivityService.currentAverageRoundTripTimeMs().takeIf { ms -> ms >= 0 },
                )
            }
        }

        presenterScope.launch {
            combine(
                connectivityService.status,
                networkServiceFacade.networkInfo,
            ) { status, info ->
                // Reachability comes from the active health-check loop, not the raw WS connectionState:
                // in airplane mode the socket can stay half-open (esp. iOS Darwin), so connectionState
                // still reports Connected while health checks correctly flip status to RECONNECTING/DISCONNECTED.
                val isReachable = status.isConnected()
                // Only trust the last snapshot's peer count while the link is up; otherwise the
                // stale count would show beneath an OFFLINE badge (networkInfo is not cleared on drop).
                val peerCount = if (isReachable) info?.connections?.size else null
                val dataReceived = info?.allDataReceived == true
                Triple(isReachable, peerCount, computeHealthState(isReachable, peerCount, dataReceived))
            }.distinctUntilChanged()
                .collect { (isReachable, peerCount, health) ->
                    _uiState.update {
                        it.copy(
                            isReachable = isReachable,
                            peerCountViaNode = peerCount,
                            healthState = health,
                        )
                    }
                }
        }
    }

    fun onAction(action: ClientNetworkOverviewUiAction) {
        when (action) {
            ClientNetworkOverviewUiAction.OnConnectionsClick -> navigateTo(ClientNavRoute.NetworkConnections)
            ClientNetworkOverviewUiAction.OnMyConnectionClick -> navigateTo(ClientNavRoute.NetworkMyConnection)
            ClientNetworkOverviewUiAction.OnCheckConnectionSettings -> navigateTo(ClientNavRoute.TrustedNodeSetupSettings)
        }
    }

    private fun computeHealthState(
        isReachable: Boolean,
        peerCount: Int?,
        isDataReceived: Boolean,
    ): NetworkHealthState =
        when {
            !isReachable -> NetworkHealthState.OFFLINE
            peerCount == 0 -> NetworkHealthState.OFFLINE
            peerCount == null || !isDataReceived -> NetworkHealthState.SYNCING
            else -> NetworkHealthState.HEALTHY
        }
}
