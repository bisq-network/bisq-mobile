package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
data class FiatPaymentMethodDto(
    val paymentRail: FiatPaymentRailDto,
    val name: String,
    val supportedCurrencies: List<FiatCurrencyDto>,
    val supportedCountries: List<CountryDto>,
    val matchesAllCountries: Boolean,
    val chargebackRisk: FiatPaymentMethodChargebackRiskDto,
    val tradeLimitInfo: String,
    val tradeDuration: String,
)
