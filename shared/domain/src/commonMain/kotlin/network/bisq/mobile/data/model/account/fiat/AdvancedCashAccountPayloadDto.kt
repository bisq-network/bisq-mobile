package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class AdvancedCashAccountPayloadDto(
    val selectedCurrencyCodes: List<String>,
    val accountNr: String,
) : FiatAccountPayloadDto
