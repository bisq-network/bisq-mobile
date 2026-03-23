package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class UpiAccountPayloadDto(
    val countryCode: String,
    val virtualPaymentAddress: String,
) : FiatAccountPayloadDto
