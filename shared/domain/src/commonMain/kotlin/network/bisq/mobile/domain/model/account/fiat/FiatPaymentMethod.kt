package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail

data class FiatPaymentMethod(
    val paymentRail: FiatPaymentRail,
    val name: String,
    val supportedCurrencyCodes: String,
    val countryNames: String,
    val chargebackRisk: FiatPaymentMethodChargebackRisk,
    val tradeLimitInfo: String,
    val tradeDuration: String,
)
