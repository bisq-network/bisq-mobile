package network.bisq.mobile.domain.model.account.create.fiat

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccountPayload
import network.bisq.mobile.domain.model.account.fiat.FiatCurrency

data class CreateWiseAccountPayload(
    val selectedCurrencies: List<FiatCurrency>,
    val holderName: String,
    val email: String,
) : CreatePaymentAccountPayload
