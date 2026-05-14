package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class ZelleAccountPayloadDto(
    val holderName: String,
    val emailOrMobileNr: String,
    override val chargebackRisk: FiatPaymentMethodChargebackRiskDto? = null,
    override val paymentMethodName: String,
    val currency: String,
    val country: String,
) : FiatPaymentAccountPayloadDto
