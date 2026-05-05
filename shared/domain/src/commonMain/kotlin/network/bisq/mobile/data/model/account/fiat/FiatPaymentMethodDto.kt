package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class FiatPaymentMethodDto(
    val paymentRail: FiatPaymentRailDto,
    val name: String,
    val supportedCurrencyCodes: String,
    val countryNames: String,
    val chargebackRisk: FiatPaymentMethodChargebackRiskDto,
    val tradeLimitInfo: String,
    val tradeDuration: String,
)
