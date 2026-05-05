package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.utils.EMPTY_STRING

@Serializable
data class UserDefinedFiatAccountPayloadDto(
    val accountData: String,
    override val chargebackRisk: FiatPaymentMethodChargebackRiskDto? = null,
    override val paymentMethodName: String = EMPTY_STRING,
    override val currency: String = EMPTY_STRING,
    override val country: String? = null,
) : FiatPaymentAccountPayloadDto
