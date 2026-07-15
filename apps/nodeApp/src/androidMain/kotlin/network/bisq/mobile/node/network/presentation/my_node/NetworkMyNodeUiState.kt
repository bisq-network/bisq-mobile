package network.bisq.mobile.node.network.presentation.my_node

data class NetworkMyNodeUiState(
    val onionAddress: String? = null,
    val keyId: String? = null,
    val appVersion: String = "",
    val isTorRunning: Boolean = false,
)
