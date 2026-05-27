package network.bisq.mobile.client.payment_accounts.data.model.fiat.user_defined

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.payment_method.FiatPaymentMethodChargebackRiskDto
import network.bisq.mobile.domain.utils.EMPTY_STRING

@Serializable
data class UserDefinedFiatAccountPayloadDto(
    val accountData: String,
    override val chargebackRisk: FiatPaymentMethodChargebackRiskDto? = null,
    override val paymentMethodName: String = EMPTY_STRING,
    val currency: String = EMPTY_STRING,
    val country: String? = null,
) : FiatPaymentAccountPayloadDto
