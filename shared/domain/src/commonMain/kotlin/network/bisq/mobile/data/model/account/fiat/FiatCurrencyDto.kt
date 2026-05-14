package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class FiatCurrencyDto(
    val code: String,
    val name: String,
)
