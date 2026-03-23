package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class SwishAccountPayloadDto(
    val countryCode: String,
    val holderName: String,
    val mobileNr: String,
) : FiatAccountPayloadDto
