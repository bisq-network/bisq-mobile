package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class SbpAccountPayloadDto(
    val holderName: String,
    val mobileNumber: String,
    val bankName: String,
) : FiatAccountPayloadDto
