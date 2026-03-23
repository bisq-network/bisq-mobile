package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class NeftAccountPayloadDto(
    val holderName: String,
    val accountNr: String,
    val ifsc: String,
) : FiatAccountPayloadDto
