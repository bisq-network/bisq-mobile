package network.bisq.mobile.presentation.settings.payment_accounts_musig.model

import network.bisq.mobile.domain.model.account.fiat.FiatPaymentAccount
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentAccountPayload
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentCountryBasedAccountPayload
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentSingleCurrencyAccountPayload
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccount
import network.bisq.mobile.domain.model.account.fiat.WiseAccount
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
            is WiseAccount -> PaymentTypeVO.WISE
            is UserDefinedFiatAccount -> PaymentTypeVO.CUSTOM
            else -> return null
        }

    return toFiatAccountVO(paymentMethod)
}

private fun FiatPaymentAccount.toFiatAccountVO(paymentMethod: PaymentTypeVO): FiatAccountVO? {
    val fiatAccountPayload = accountPayload as? FiatPaymentAccountPayload ?: return null
    val payloadValues = fiatAccountPayload.toFiatPayloadValues()

    return FiatAccountVO(
        accountName = accountName,
        chargebackRisk = payloadValues.chargebackRisk.toVO(),
        paymentType = paymentMethod,
        paymentMethodName = payloadValues.paymentMethodName,
        country = payloadValues.country,
        currency = payloadValues.currency,
    )
}

private fun FiatPaymentAccountPayload.toFiatPayloadValues(): FiatPayloadValues =
    when {
        this is FiatPaymentCountryBasedAccountPayload && this is FiatPaymentSingleCurrencyAccountPayload ->
            FiatPayloadValues(
                chargebackRisk = chargebackRisk,
                paymentMethodName = paymentMethodName,
                country = country,
                currency = currency,
            )

        this is FiatPaymentCountryBasedAccountPayload ->
            FiatPayloadValues(
                chargebackRisk = chargebackRisk,
                paymentMethodName = paymentMethodName,
                country = country,
                currency = EMPTY_STRING,
            )

        this is FiatPaymentSingleCurrencyAccountPayload ->
            FiatPayloadValues(
                chargebackRisk = chargebackRisk,
                paymentMethodName = paymentMethodName,
                country = EMPTY_STRING,
                currency = currency,
            )

        else ->
            FiatPayloadValues(
                chargebackRisk = chargebackRisk,
                paymentMethodName = paymentMethodName,
                country = EMPTY_STRING,
                currency = EMPTY_STRING,
            )
    }

private data class FiatPayloadValues(
    val chargebackRisk: FiatPaymentMethodChargebackRisk?,
    val paymentMethodName: String,
    val country: String,
    val currency: String,
)
