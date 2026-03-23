package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class MoneyGramAccountPayloadDto(
    val countryCode: String,
    val selectedCurrencyCodes: List<String>,
    val holderName: String,
    val email: String,
    val state: String,
) : FiatAccountPayloadDto
