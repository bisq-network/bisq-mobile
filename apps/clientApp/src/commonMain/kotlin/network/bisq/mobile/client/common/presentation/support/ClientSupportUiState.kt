package network.bisq.mobile.client.common.presentation.support

/** Push-notification debugging state for the client's Support screen. */
data class ClientSupportUiState(
    val deviceToken: String? = null,
    val isDeviceRegistered: Boolean = false,
    val tokenRequestInProgress: Boolean = false,
)
