package network.bisq.mobile.node.network.presentation.network

enum class NetworkHealthState { HEALTHY, SYNCING, OFFLINE }

data class NetworkUiState(
    val peerCount: Int = 0,
    val isTorRunning: Boolean = false,
    val onionAddress: String? = null,
    val healthState: NetworkHealthState = NetworkHealthState.OFFLINE,
)
