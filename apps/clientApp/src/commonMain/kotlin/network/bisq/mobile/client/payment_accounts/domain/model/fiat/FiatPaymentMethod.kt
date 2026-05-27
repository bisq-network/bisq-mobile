package network.bisq.mobile.client.payment_accounts.domain.model.fiat

import network.bisq.mobile.client.payment_accounts.domain.model.PaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk

data class FiatPaymentMethod(
    val paymentRail: FiatPaymentRail,
    override val name: String,
    val supportedCurrencies: List<FiatCurrency>,
    val supportedCountries: List<Country>,
    val matchesAllCountries: Boolean,
    val chargebackRisk: FiatPaymentMethodChargebackRisk,
    override val tradeLimitInfo: String,
    override val tradeDuration: String,
) : PaymentMethod
