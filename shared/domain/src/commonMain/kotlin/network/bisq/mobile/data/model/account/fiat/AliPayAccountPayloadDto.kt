package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class AliPayAccountPayloadDto(
    val accountNr: String,
) : FiatAccountPayloadDto
