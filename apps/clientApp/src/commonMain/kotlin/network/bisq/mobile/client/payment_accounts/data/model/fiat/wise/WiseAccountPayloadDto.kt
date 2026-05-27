package network.bisq.mobile.client.payment_accounts.data.model.fiat.wise

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatCurrencyDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.payment_method.FiatPaymentMethodChargebackRiskDto

@Serializable
data class WiseAccountPayloadDto(
    val selectedCurrencies: List<FiatCurrencyDto>,
    val holderName: String,
    val email: String,
    override val chargebackRisk: FiatPaymentMethodChargebackRiskDto? = null,
    override val paymentMethodName: String,
) : FiatPaymentAccountPayloadDto
