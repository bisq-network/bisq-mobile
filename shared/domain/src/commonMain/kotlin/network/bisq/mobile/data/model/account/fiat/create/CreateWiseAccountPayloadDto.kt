package network.bisq.mobile.data.model.account.fiat.create

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.account.create.CreatePaymentAccountPayloadDto

@Serializable
data class CreateWiseAccountPayloadDto(
    val selectedCurrencyCodes: List<String>,
    val holderName: String,
    val email: String,
) : CreatePaymentAccountPayloadDto
