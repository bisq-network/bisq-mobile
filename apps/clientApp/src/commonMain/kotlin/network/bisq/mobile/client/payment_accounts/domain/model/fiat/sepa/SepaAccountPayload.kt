package network.bisq.mobile.client.payment_accounts.domain.model.fiat.sepa

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.FiatPaymentCountryBasedAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatPaymentSingleCurrencyAccountPayload
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentAccountPayload
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk

data class SepaAccountPayload(
    override val chargebackRisk: FiatPaymentMethodChargebackRisk? = null,
    override val paymentMethodName: String,
    override val currency: FiatCurrency,
    override val country: Country,
    val acceptedCountries: List<Country>,
    val holderName: String,
    val iban: String,
    val bic: String,
) : FiatPaymentAccountPayload,
    FiatPaymentCountryBasedAccountPayload,
    FiatPaymentSingleCurrencyAccountPayload
