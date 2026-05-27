package network.bisq.mobile.client.payment_accounts.data.model.fiat.user_defined

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.create.CreatePaymentAccountPayloadDto

@Serializable
data class CreateUserDefinedFiatAccountPayloadDto(
    val accountData: String,
) : CreatePaymentAccountPayloadDto
