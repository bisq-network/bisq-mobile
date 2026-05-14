package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class WiseAccountPayloadDto(
    val selectedCurrencyCodes: List<String>,
    val holderName: String,
    val email: String,
    override val chargebackRisk: FiatPaymentMethodChargebackRiskDto? = null,
    override val paymentMethodName: String,
) : FiatPaymentAccountPayloadDto
