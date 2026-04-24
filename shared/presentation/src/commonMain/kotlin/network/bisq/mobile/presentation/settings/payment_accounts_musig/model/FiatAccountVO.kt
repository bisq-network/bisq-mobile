package network.bisq.mobile.presentation.settings.payment_accounts_musig.model

import network.bisq.mobile.domain.model.account.fiat.FiatPaymentAccount
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentAccountPayload
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccount
import network.bisq.mobile.domain.model.account.fiat.ZelleAccount
import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.model.account.toVO
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING

data class FiatAccountVO(
    val accountName: String,
    val paymentType: PaymentTypeVO,
    val chargebackRisk: FiatPaymentMethodChargebackRiskVO?,
    val paymentMethodName: String,
    val country: String,
    val currency: String,
)

fun FiatPaymentAccount.toVO(): FiatAccountVO? {
    val paymentMethod =
        when (this) {
            is ZelleAccount -> PaymentTypeVO.ZELLE
            is UserDefinedFiatAccount -> PaymentTypeVO.CUSTOM
            else -> return null
        }

    return toFiatAccountVO(paymentMethod)
}

private fun FiatPaymentAccount.toFiatAccountVO(paymentMethod: PaymentTypeVO): FiatAccountVO? {
    val payload = accountPayload as? FiatPaymentAccountPayload ?: return null

    return FiatAccountVO(
        accountName = accountName,
        chargebackRisk = payload.chargebackRisk.toVO(),
        paymentType = paymentMethod,
        paymentMethodName = payload.paymentMethodName ?: EMPTY_STRING,
        country = payload.country ?: EMPTY_STRING,
        currency = payload.currency ?: EMPTY_STRING,
    )
}
