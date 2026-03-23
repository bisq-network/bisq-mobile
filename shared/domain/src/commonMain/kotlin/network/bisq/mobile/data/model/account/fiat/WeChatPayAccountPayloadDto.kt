package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class WeChatPayAccountPayloadDto(
    val accountNr: String,
) : FiatAccountPayloadDto
