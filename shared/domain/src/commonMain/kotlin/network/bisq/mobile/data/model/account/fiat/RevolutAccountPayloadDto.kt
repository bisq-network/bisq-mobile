package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class RevolutAccountPayloadDto(
    val userName: String,
    val selectedCurrencyCodes: List<String>,
) : FiatAccountPayloadDto
