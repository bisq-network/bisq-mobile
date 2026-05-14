package network.bisq.mobile.domain.model.account.create.fiat

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccountPayload

data class CreateWiseAccountPayload(
    val selectedCurrencyCodes: List<String>,
    val holderName: String,
    val email: String,
) : CreatePaymentAccountPayload
