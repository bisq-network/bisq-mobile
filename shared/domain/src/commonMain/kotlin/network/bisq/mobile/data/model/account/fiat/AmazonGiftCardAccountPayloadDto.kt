package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class AmazonGiftCardAccountPayloadDto(
    val countryCode: String,
    val selectedCurrencyCode: String,
    val emailOrMobileNr: String,
) : FiatAccountPayloadDto
