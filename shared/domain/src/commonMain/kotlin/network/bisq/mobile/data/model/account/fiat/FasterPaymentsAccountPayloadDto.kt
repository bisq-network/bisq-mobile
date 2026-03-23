package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class FasterPaymentsAccountPayloadDto(
    val holderName: String,
    val sortCode: String,
    val accountNr: String,
) : FiatAccountPayloadDto
