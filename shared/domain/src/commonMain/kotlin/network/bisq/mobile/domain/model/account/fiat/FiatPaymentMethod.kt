package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.domain.model.account.PaymentMethod

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
