package network.bisq.mobile.client.payment_accounts.data.model.fiat.revolut

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatCurrencyDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.payment_method.FiatPaymentMethodChargebackRiskDto

@Serializable
data class RevolutAccountPayloadDto(
    override val chargebackRisk: FiatPaymentMethodChargebackRiskDto? = null,
    override val paymentMethodName: String,
    val selectedCurrencies: List<FiatCurrencyDto>,
    val userName: String,
) : FiatPaymentAccountPayloadDto
