package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class PixAccountPayloadDto(
    val holderName: String,
    val pixKey: String,
    val countryCode: String,
) : FiatAccountPayloadDto
