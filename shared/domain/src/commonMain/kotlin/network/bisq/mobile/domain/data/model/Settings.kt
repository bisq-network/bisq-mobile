package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.httpclient.NetworkType

@Serializable
data class Settings (
    val bisqApiUrl: String = "",
    val firstLaunch: Boolean = true,
    val showChatRulesWarnBox: Boolean = true,
    val selectedMarketCode: String = "BTC/USD",
    val notificationPermissionState: NotificationPermissionState = NotificationPermissionState.NOT_GRANTED,
    // client node specific:
    val isInternalTorEnabled: Boolean = false,
    val selectedNetworkType: NetworkType = NetworkType.LAN,
    val useExternalProxy: Boolean = false,
    val proxyUrl: String = "",
    val isProxyUrlTor: Boolean = true,
)

@Serializable
enum class NotificationPermissionState {
    NOT_GRANTED,
    GRANTED,
    DENIED,
    DONT_ASK_AGAIN,
}