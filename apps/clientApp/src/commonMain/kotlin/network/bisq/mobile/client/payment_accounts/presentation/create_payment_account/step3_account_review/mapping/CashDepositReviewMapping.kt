package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step3_account_review.mapping

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.FiatPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.cash_deposit.CashDepositAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.cash_deposit.CashDepositAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.cash_deposit.CreateCashDepositAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency

internal fun CreateCashDepositAccount.toReviewPaymentAccount(paymentMethod: FiatPaymentMethod): CashDepositAccount {
    val country = paymentMethod.supportedCountries.firstOrNull { it.code == accountPayload.selectedCountryCode }
    val currency = paymentMethod.supportedCurrencies.firstOrNull { it.code == accountPayload.selectedCurrencyCode }

    return CashDepositAccount(
        accountName = accountName,
        accountPayload =
            CashDepositAccountPayload(
                chargebackRisk = paymentMethod.chargebackRisk,
                paymentMethodName = paymentMethod.name,
                currency =
                    currency ?: FiatCurrency(
                        code = accountPayload.selectedCurrencyCode,
                        name = accountPayload.selectedCurrencyCode,
                    ),
                country =
                    country ?: Country(
                        code = accountPayload.selectedCountryCode,
                        name = accountPayload.selectedCountryCode,
                    ),
                holderName = accountPayload.holderName,
                holderId = accountPayload.holderId,
                bankName = accountPayload.bankName,
                bankId = accountPayload.bankId,
                branchId = accountPayload.branchId,
                accountNr = accountPayload.accountNr,
                bankAccountType = accountPayload.bankAccountType,
                nationalAccountId = accountPayload.nationalAccountId,
                requirements = accountPayload.requirements,
            ),
        creationDate = null,
        tradeLimitInfo = paymentMethod.tradeLimitInfo,
        tradeDuration = paymentMethod.tradeDuration,
    )
}
