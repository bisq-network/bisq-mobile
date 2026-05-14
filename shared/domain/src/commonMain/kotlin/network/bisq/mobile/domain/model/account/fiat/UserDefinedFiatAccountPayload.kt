package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.domain.utils.EMPTY_STRING

data class UserDefinedFiatAccountPayload(
    val accountData: String,
    override val chargebackRisk: FiatPaymentMethodChargebackRisk? = null,
    override val paymentMethodName: String = EMPTY_STRING,
) : FiatPaymentAccountPayload
