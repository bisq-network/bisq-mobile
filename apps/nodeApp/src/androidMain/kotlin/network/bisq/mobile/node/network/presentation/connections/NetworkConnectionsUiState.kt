package network.bisq.mobile.node.network.presentation.connections

import network.bisq.mobile.node.common.domain.service.network.NodePeerInfo

data class NetworkConnectionsUiState(
    val peerCount: Int = 0,
    val peers: List<NodePeerInfo> = emptyList(),
)
