package network.bisq.mobile.node.network.presentation.connections

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.node.common.domain.service.network.NodeNetworkServiceFacade
import network.bisq.mobile.node.common.domain.service.network.NodePeerInfo
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

class NetworkConnectionsPresenter(
    private val networkServiceFacade: NodeNetworkServiceFacade,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(NetworkConnectionsUiState())
    val uiState: StateFlow<NetworkConnectionsUiState> = _uiState.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()
        presenterScope.launch {
            networkServiceFacade.connectedPeers.collect { peers ->
                // Newest peers first; stable tie-break so the LazyColumn keys don't reshuffle across rebuilds.
                val sorted =
                    peers.sortedWith(
                        compareByDescending<NodePeerInfo> { it.establishedAtMillis }.thenBy { it.connectionId },
                    )
                _uiState.value =
                    NetworkConnectionsUiState(
                        peerCount = sorted.size,
                        peers = sorted,
                    )
            }
        }
    }
}
