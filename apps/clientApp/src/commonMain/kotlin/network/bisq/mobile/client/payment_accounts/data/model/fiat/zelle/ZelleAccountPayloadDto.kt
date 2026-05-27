package network.bisq.mobile.client.payment_accounts.data.model.fiat.zelle

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.payment_method.FiatPaymentMethodChargebackRiskDto

@Serializable
data class ZelleAccountPayloadDto(
    val holderName: String,
    val emailOrMobileNr: String,
    override val chargebackRisk: FiatPaymentMethodChargebackRiskDto? = null,
    override val paymentMethodName: String,
    val currency: String,
    val country: String,
) : FiatPaymentAccountPayloadDto
