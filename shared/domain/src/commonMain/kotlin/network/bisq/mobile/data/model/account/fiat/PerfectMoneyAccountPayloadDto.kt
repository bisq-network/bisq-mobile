package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class PerfectMoneyAccountPayloadDto(
    val selectedCurrencyCode: String,
    val accountNr: String,
) : FiatAccountPayloadDto
