package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Settings (
    val bisqApiUrl: String = "",
    val firstLaunch: Boolean = true,
    val showChatRulesWarnBox: Boolean = true,
    val selectedMarketCode: String = "BTC/USD",
    val notificationPermissionState: NotificationPermissionState = NotificationPermissionState.NOT_GRANTED,
    // client node specific:
    val useExternalProxy: Boolean = false,
    val proxyUrl: String = "",
    val isTorProxy: Boolean = true,
)

@Serializable
enum class NotificationPermissionState {
    NOT_GRANTED,
    GRANTED,
    DENIED,
    DONT_ASK_AGAIN,
}