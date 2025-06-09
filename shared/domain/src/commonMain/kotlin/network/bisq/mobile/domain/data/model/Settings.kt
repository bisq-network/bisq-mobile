package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Settings (
    var bisqApiUrl: String = "",
    var firstLaunch: Boolean = true,
    var showChatRulesWarnBox: Boolean = true,
    var selectedMarketCode: String = "BTC/USD"
) : BaseModel()
