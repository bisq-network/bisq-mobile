package network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.FiatPaymentCountryBasedAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatPaymentSingleCurrencyAccountPayload
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentAccountPayload
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk

data class ZelleAccountPayload(
    val holderName: String,
    val emailOrMobileNr: String,
    override val chargebackRisk: FiatPaymentMethodChargebackRisk? = null,
    override val paymentMethodName: String,
    override val currency: FiatCurrency,
    override val country: Country,
) : FiatPaymentAccountPayload,
    FiatPaymentCountryBasedAccountPayload,
    FiatPaymentSingleCurrencyAccountPayload
