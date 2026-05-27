package network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccountPayload

data class CreateZelleAccountPayload(
    val holderName: String,
    val emailOrMobileNr: String,
) : CreatePaymentAccountPayload
