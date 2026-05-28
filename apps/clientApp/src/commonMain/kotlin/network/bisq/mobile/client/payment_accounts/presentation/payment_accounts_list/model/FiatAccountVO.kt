package network.bisq.mobile.client.payment_accounts.presentation.payment_accounts_list.model

import network.bisq.mobile.client.common.presentation.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.client.common.presentation.model.account.PaymentTypeVO
import network.bisq.mobile.client.common.presentation.model.account.toVO
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.ach_transfer.AchTransferAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.cash_deposit.CashDepositAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.FiatPaymentCountryBasedAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatPaymentSingleCurrencyAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut.RevolutAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.wise.WiseAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle.ZelleAccount
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentAccount
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentAccountPayload
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccount
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
            is AchTransferAccount -> PaymentTypeVO.ACH_TRANSFER
            is CashDepositAccount -> PaymentTypeVO.CASH_DEPOSIT
            is ZelleAccount -> PaymentTypeVO.ZELLE
            is WiseAccount -> PaymentTypeVO.WISE
            is RevolutAccount -> PaymentTypeVO.REVOLUT
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
    when (this) {
        is FiatPaymentCountryBasedAccountPayload if this is FiatPaymentSingleCurrencyAccountPayload ->
            FiatPayloadValues(
                chargebackRisk = chargebackRisk,
                paymentMethodName = paymentMethodName,
                country = country.name,
                currency = currency.code,
            )

        is FiatPaymentCountryBasedAccountPayload ->
            FiatPayloadValues(
                chargebackRisk = chargebackRisk,
                paymentMethodName = paymentMethodName,
                country = country.name,
                currency = EMPTY_STRING,
            )

        is FiatPaymentSingleCurrencyAccountPayload ->
            FiatPayloadValues(
                chargebackRisk = chargebackRisk,
                paymentMethodName = paymentMethodName,
                country = EMPTY_STRING,
                currency = currency.code,
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
