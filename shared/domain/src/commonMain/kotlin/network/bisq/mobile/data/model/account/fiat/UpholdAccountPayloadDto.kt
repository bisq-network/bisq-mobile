package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class UpholdAccountPayloadDto(
    val selectedCurrencyCodes: List<String>,
    val holderName: String,
    val accountId: String,
) : FiatAccountPayloadDto
