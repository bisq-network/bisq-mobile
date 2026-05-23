package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class RevolutAccountPayloadDto(
    override val chargebackRisk: FiatPaymentMethodChargebackRiskDto? = null,
    override val paymentMethodName: String,
    val selectedCurrencies: List<FiatCurrencyDto>,
    val userName: String,
) : FiatPaymentAccountPayloadDto
