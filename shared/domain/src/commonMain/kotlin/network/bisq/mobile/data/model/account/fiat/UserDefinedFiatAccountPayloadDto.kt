package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class UserDefinedFiatAccountPayloadDto(
    val accountData: String,
) : FiatAccountPayloadDto
