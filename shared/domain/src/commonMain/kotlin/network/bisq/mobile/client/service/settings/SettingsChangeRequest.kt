package network.bisq.mobile.client.service.settings

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO

@Serializable
data class SettingsChangeRequest(
    val isTacAccepted: Boolean? = null,
    val tradeRulesConfirmed: Boolean? = null,
    val closeMyOfferWhenTaken: Boolean? = null,
    val languageCode: String? = null,
    val supportedLanguageCodes: Set<String>? = null,
    val maxTradePriceDeviation: Double? = null,
    val selectedMarket: MarketVO? = null
)