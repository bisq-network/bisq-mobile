package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class Pin4AccountPayloadDto(
    val mobileNr: String,
) : FiatAccountPayloadDto
