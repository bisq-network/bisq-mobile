package network.bisq.mobile.client.payment_accounts.domain.model.fiat.wise

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccountPayload

data class CreateWiseAccountPayload(
    val selectedCurrencies: List<FiatCurrency>,
    val holderName: String,
    val email: String,
) : CreatePaymentAccountPayload
