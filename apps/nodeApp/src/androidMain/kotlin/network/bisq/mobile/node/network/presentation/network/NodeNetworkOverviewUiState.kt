package network.bisq.mobile.node.network.presentation.network

import network.bisq.mobile.presentation.common.ui.components.network.NetworkHealthState

data class NodeNetworkOverviewUiState(
    val peerCount: Int = 0,
    val isTorRunning: Boolean = false,
    val onionAddress: String? = null,
    val healthState: NetworkHealthState = NetworkHealthState.OFFLINE,
)
