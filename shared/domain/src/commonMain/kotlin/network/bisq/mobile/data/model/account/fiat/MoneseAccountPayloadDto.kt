package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class MoneseAccountPayloadDto(
    val selectedCurrencyCodes: List<String>,
    val holderName: String,
    val mobileNr: String,
) : FiatAccountPayloadDto
