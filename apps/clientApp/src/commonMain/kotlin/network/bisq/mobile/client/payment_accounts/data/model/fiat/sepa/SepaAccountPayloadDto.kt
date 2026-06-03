package network.bisq.mobile.client.payment_accounts.data.model.fiat.sepa

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.CountryDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatCurrencyDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.payment_method.FiatPaymentMethodChargebackRiskDto

@Serializable
data class SepaAccountPayloadDto(
    override val chargebackRisk: FiatPaymentMethodChargebackRiskDto? = null,
    override val paymentMethodName: String,
    val currency: FiatCurrencyDto,
    val country: CountryDto,
    val acceptedCountries: List<CountryDto>,
    val holderName: String,
    val iban: String,
    val bic: String,
) : FiatPaymentAccountPayloadDto
