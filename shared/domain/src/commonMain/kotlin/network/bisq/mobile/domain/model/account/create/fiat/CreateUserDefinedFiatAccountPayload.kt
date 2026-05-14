package network.bisq.mobile.domain.model.account.create.fiat

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccountPayload

data class CreateUserDefinedFiatAccountPayload(
    val accountData: String,
) : CreatePaymentAccountPayload
