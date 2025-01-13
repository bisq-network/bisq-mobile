package network.bisq.mobile.client.service.settings

import kotlinx.serialization.Serializable

@Serializable
enum class SettingsKey {
    IS_TAC_ACCEPTED,
    TRADE_RULES_CONFIRMED,
    CLOSE_MY_OFFER_WHEN_TAKEN,
    LANGUAGE_CODE,
    SUPPORTED_LANGUAGE_CODES,
    MAX_TRADE_PRICE_DEVIATION,
    SELECTED_MARKET,
}
