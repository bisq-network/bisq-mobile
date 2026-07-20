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
import network.bisq.mobile.presentation.common.ui.components.network.NetworkHealthState
import network.bisq.mobile.presentation.main.MainPresenter

class NodeNetworkOverviewPresenter(
    private val networkServiceFacade: NodeNetworkServiceFacade,
    private val kmpTorService: KmpTorService,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(NodeNetworkOverviewUiState())
    val uiState: StateFlow<NodeNetworkOverviewUiState> = _uiState.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()
        presenterScope.launch {
            combine(
                networkServiceFacade.numConnections,
                networkServiceFacade.allDataReceived,
                kmpTorService.state,
                networkServiceFacade.myNodeInfo,
            ) { numConnections, dataReceived, torState, myNodeInfo ->
                val peerCount = numConnections.coerceAtLeast(0)
                val isTorRunning = torState is KmpTorService.TorState.Started
                NodeNetworkOverviewUiState(
                    peerCount = peerCount,
                    isTorRunning = isTorRunning,
                    onionAddress = myNodeInfo.onionAddress,
                    healthState = computeHealthState(peerCount, dataReceived, isTorRunning),
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    fun onAction(action: NodeNetworkOverviewUiAction) {
        when (action) {
            NodeNetworkOverviewUiAction.OnConnectionsClick -> navigateTo(NodeNavRoute.NetworkPeerConnections)
            NodeNetworkOverviewUiAction.OnMyNodeClick -> navigateTo(NodeNavRoute.NetworkMyNode)
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
