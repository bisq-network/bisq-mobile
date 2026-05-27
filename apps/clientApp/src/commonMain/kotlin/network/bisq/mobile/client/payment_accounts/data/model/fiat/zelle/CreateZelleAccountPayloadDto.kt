package network.bisq.mobile.client.payment_accounts.data.model.fiat.zelle

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.create.CreatePaymentAccountPayloadDto

@Serializable
data class CreateZelleAccountPayloadDto(
    val holderName: String,
    val emailOrMobileNr: String,
) : CreatePaymentAccountPayloadDto
