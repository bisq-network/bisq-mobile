package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class InteracETransferAccountPayloadDto(
    val holderName: String,
    val email: String,
    val question: String,
    val answer: String,
) : FiatAccountPayloadDto
