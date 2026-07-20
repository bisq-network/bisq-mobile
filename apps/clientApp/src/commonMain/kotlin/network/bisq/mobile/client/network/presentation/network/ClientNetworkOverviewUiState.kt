package network.bisq.mobile.client.network.presentation.network

import network.bisq.mobile.presentation.common.ui.components.network.NetworkHealthState

data class ClientNetworkOverviewUiState(
    val trustedNodeHost: String? = null,
    val isReachable: Boolean = false,
    val isTorRouted: Boolean = false,
    val peerCountViaNode: Int? = null,
    val latencyMs: Long? = null,
    val healthState: NetworkHealthState = NetworkHealthState.OFFLINE,
)
