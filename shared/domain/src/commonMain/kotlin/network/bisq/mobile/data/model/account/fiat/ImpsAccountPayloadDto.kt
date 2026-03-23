package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class ImpsAccountPayloadDto(
    val holderName: String,
    val accountNr: String,
    val ifsc: String,
) : FiatAccountPayloadDto
