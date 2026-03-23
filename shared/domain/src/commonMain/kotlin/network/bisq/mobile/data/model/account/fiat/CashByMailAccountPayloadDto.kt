package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class CashByMailAccountPayloadDto(
    val selectedCurrencyCode: String,
    val postalAddress: String,
    val contact: String,
    val extraInfo: String,
) : FiatAccountPayloadDto
