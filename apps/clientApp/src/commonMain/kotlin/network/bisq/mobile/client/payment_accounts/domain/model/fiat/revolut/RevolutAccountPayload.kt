package network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentAccountPayload
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk

data class RevolutAccountPayload(
    val selectedCurrencies: List<FiatCurrency>,
    val userName: String,
    override val chargebackRisk: FiatPaymentMethodChargebackRisk? = null,
    override val paymentMethodName: String,
) : FiatPaymentAccountPayload
