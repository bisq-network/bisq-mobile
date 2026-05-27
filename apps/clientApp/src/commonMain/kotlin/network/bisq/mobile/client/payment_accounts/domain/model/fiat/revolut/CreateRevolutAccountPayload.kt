package network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccountPayload

data class CreateRevolutAccountPayload(
    val userName: String,
    val selectedCurrencies: List<FiatCurrency>,
) : CreatePaymentAccountPayload
