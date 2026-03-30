package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.account.PaymentAccountPayloadDto

@Serializable
data class ZelleAccountPayloadDto(
    val holderName: String,
    val emailOrMobileNr: String,
) : PaymentAccountPayloadDto
