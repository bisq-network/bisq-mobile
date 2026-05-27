package network.bisq.mobile.client.payment_accounts.data.model.fiat.revolut

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.create.CreatePaymentAccountPayloadDto

@Serializable
data class CreateRevolutAccountPayloadDto(
    val userName: String,
    val selectedCurrencyCodes: List<String>,
) : CreatePaymentAccountPayloadDto
