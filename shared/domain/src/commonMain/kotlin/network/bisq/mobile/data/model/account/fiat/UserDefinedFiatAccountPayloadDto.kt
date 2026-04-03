package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class UserDefinedFiatAccountPayloadDto(
    val accountData: String,
    override val chargebackRisk: FiatPaymentMethodChargebackRiskDto? = null,
    override val paymentMethodName: String? = null,
    override val currency: String? = null,
    override val country: String? = null,
) : FiatPaymentAccountPayloadDto
