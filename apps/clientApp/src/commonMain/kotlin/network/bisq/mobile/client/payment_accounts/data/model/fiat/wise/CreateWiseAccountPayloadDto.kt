package network.bisq.mobile.client.payment_accounts.data.model.fiat.wise

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.create.CreatePaymentAccountPayloadDto

@Serializable
data class CreateWiseAccountPayloadDto(
    val selectedCurrencyCodes: List<String>,
    val holderName: String,
    val email: String,
) : CreatePaymentAccountPayloadDto
