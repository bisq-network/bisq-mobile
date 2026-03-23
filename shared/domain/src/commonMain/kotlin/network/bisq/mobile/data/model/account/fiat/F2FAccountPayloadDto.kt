package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class F2FAccountPayloadDto(
    val countryCode: String,
    val selectedCurrencyCode: String,
    val city: String,
    val contact: String,
    val extraInfo: String,
) : FiatAccountPayloadDto
