package network.bisq.mobile.client.payment_accounts.domain.model.crypto.monero

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount

data class CreateMoneroAccount(
    override val accountName: String,
    override val accountPayload: CreateMoneroAccountPayload,
) : CreatePaymentAccount
