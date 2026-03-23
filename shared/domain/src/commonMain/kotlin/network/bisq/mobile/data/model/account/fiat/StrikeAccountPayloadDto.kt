package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class StrikeAccountPayloadDto(
    val countryCode: String,
    val holderName: String,
) : FiatAccountPayloadDto
