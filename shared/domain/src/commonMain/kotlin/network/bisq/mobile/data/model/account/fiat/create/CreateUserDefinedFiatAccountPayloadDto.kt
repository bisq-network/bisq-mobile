package network.bisq.mobile.data.model.account.fiat.create

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.account.create.CreatePaymentAccountPayloadDto

@Serializable
data class CreateUserDefinedFiatAccountPayloadDto(
    val accountData: String,
) : CreatePaymentAccountPayloadDto
