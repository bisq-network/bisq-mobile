package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Settings (
    val firstLaunch: Boolean = true,
    val showChatRulesWarnBox: Boolean = true,
    val selectedMarketCode: String = "BTC/USD",
    val notificationPermissionState: PermissionState = PermissionState.NOT_GRANTED,
    val batteryOptimizationState: BatteryOptimizationState = BatteryOptimizationState.NOT_IGNORED,
)

@Serializable
enum class PermissionState {
    NOT_GRANTED,
    GRANTED,
    DENIED,
    DONT_ASK_AGAIN,
}

@Serializable
enum class BatteryOptimizationState {
    NOT_IGNORED,
    IGNORED,
    DONT_ASK_AGAIN,
}