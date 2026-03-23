package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class PromptPayAccountPayloadDto(
    val promptPayId: String,
    val countryCode: String,
) : FiatAccountPayloadDto
