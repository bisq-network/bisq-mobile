package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class WiseUsdAccountPayloadDto(
    val countryCode: String,
    val holderName: String,
    val email: String,
    val beneficiaryAddress: String,
) : FiatAccountPayloadDto
