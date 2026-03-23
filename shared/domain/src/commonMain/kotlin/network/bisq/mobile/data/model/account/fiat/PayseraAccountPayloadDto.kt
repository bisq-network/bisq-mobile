package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class PayseraAccountPayloadDto(
    val selectedCurrencyCodes: List<String>,
    val email: String,
) : FiatAccountPayloadDto
