package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class PayIdAccountPayloadDto(
    val holderName: String,
    val payId: String,
) : FiatAccountPayloadDto
