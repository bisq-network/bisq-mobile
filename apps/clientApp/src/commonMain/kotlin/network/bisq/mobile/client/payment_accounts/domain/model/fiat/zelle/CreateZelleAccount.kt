package network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount

data class CreateZelleAccount(
    override val accountName: String,
    override val accountPayload: CreateZelleAccountPayload,
) : CreatePaymentAccount
