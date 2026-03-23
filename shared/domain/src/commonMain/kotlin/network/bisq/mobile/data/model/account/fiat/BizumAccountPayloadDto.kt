package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class BizumAccountPayloadDto(
    val countryCode: String,
    val mobileNr: String,
) : FiatAccountPayloadDto
