package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class USPostalMoneyOrderAccountPayloadDto(
    val holderName: String,
    val postalAddress: String,
) : FiatAccountPayloadDto
