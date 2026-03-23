package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class SepaAccountPayloadDto(
    val holderName: String,
    val iban: String,
    val bic: String,
    val countryCode: String,
    val acceptedCountryCodes: List<String>,
) : FiatAccountPayloadDto
