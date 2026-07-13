package network.bisq.mobile.node.network.presentation.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.node.common.domain.service.network.NodeNetworkServiceFacade
import network.bisq.mobile.node.common.presentation.navigation.NodeNavRoute
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

class NetworkPresenter(
    private val networkServiceFacade: NodeNetworkServiceFacade,
    private val kmpTorService: KmpTorService,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(NetworkUiState())
    val uiState: StateFlow<NetworkUiState> = _uiState.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()
        presenterScope.launch {
            combine(
                networkServiceFacade.numConnections,
                networkServiceFacade.allDataReceived,
                kmpTorService.state,
                networkServiceFacade.myNodeAddress,
            ) { numConnections, dataReceived, torState, myNodeAddress ->
                val peerCount = numConnections.coerceAtLeast(0)
                val isTorRunning = torState is KmpTorService.TorState.Started
                NetworkUiState(
                    peerCount = peerCount,
                    isTorRunning = isTorRunning,
                    onionAddress = myNodeAddress,
                    healthState = computeHealthState(peerCount, dataReceived, isTorRunning),
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    fun onAction(action: NetworkUiAction) {
        when (action) {
            NetworkUiAction.OnConnectionsClick -> navigateTo(NodeNavRoute.NetworkPeerConnections)
            NetworkUiAction.OnMyNodeClick -> navigateTo(NodeNavRoute.NetworkMyNode)
        }
    }

    private fun computeHealthState(
        peerCount: Int,
        isDataSynced: Boolean,
        isTorRunning: Boolean,
    ): NetworkHealthState =
        when {
            peerCount == 0 || !isTorRunning -> NetworkHealthState.OFFLINE
            !isDataSynced -> NetworkHealthState.SYNCING
            else -> NetworkHealthState.HEALTHY
        }
}
