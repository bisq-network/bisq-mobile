package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step3_account_review.mapping

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.FiatPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.sepa.CreateSepaAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.sepa.SepaAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.sepa.SepaAccountPayload

internal fun CreateSepaAccount.toReviewPaymentAccount(paymentMethod: FiatPaymentMethod): SepaAccount {
    val country = paymentMethod.supportedCountries.firstOrNull { it.code == accountPayload.selectedCountryCode }
    val acceptedCountries =
        accountPayload.acceptedCountryCodes.map { acceptedCountryCode ->
            paymentMethod.supportedCountries.firstOrNull { country -> country.code == acceptedCountryCode }
                ?: Country(code = acceptedCountryCode, name = acceptedCountryCode)
        }

    return SepaAccount(
        accountName = accountName,
        accountPayload =
            SepaAccountPayload(
                chargebackRisk = paymentMethod.chargebackRisk,
                paymentMethodName = paymentMethod.name,
                currency = paymentMethod.supportedCurrencies.firstOrNull() ?: FiatCurrency(code = "EUR", name = "Euro"),
                country =
                    country ?: Country(
                        code = accountPayload.selectedCountryCode,
                        name = accountPayload.selectedCountryCode,
                    ),
                acceptedCountries = acceptedCountries,
                holderName = accountPayload.holderName,
                iban = accountPayload.iban,
                bic = accountPayload.bic,
            ),
        creationDate = null,
        tradeLimitInfo = paymentMethod.tradeLimitInfo,
        tradeDuration = paymentMethod.tradeDuration,
    )
}
