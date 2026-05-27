package network.bisq.mobile.client.payment_accounts.data.model.fiat.payment_method

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.CountryDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatCurrencyDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentRailDto

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
