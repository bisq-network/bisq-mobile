package network.bisq.mobile.client.payment_accounts.data.model.fiat.common

import kotlinx.serialization.Serializable

@Serializable
data class FiatCurrencyDto(
    val code: String,
    val name: String,
)
