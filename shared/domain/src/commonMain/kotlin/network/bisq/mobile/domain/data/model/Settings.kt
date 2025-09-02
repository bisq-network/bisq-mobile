package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Settings (
    val bisqApiUrl: String = "",
    val firstLaunch: Boolean = true,
    val showChatRulesWarnBox: Boolean = true,
    val selectedMarketCode: String = "BTC/USD"
)
