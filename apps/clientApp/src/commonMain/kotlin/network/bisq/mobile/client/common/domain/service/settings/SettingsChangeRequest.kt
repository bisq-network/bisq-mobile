package network.bisq.mobile.client.common.domain.service.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettingsChangeRequest(
    val isTacAccepted: Boolean? = null,
    @SerialName("bisqEasyTradeRulesConfirmed")
    val tradeRulesConfirmed: Boolean? = null,
    val closeMyOfferWhenTaken: Boolean? = null,
    val languageCode: String? = null,
    val supportedLanguageCodes: Set<String>? = null,
    val maxTradePriceDeviation: Double? = null,
    val numDaysAfterRedactingTradeData: Int? = null,
    val useAnimations: Boolean? = null,
)
