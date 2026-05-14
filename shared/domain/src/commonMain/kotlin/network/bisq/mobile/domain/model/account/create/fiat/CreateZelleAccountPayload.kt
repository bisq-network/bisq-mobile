package network.bisq.mobile.domain.model.account.create.fiat

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccountPayload

data class CreateZelleAccountPayload(
    val holderName: String,
    val emailOrMobileNr: String,
) : CreatePaymentAccountPayload
