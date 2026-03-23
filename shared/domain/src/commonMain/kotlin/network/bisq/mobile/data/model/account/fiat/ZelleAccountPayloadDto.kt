package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class ZelleAccountPayloadDto(
    val holderName: String,
    val emailOrMobileNr: String,
) : FiatAccountPayloadDto
