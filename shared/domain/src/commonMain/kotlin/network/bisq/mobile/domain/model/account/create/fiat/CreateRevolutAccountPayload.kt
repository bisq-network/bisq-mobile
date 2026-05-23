package network.bisq.mobile.domain.model.account.create.fiat

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccountPayload
import network.bisq.mobile.domain.model.account.fiat.FiatCurrency

data class CreateRevolutAccountPayload(
    val userName: String,
    val selectedCurrencies: List<FiatCurrency>,
) : CreatePaymentAccountPayload
