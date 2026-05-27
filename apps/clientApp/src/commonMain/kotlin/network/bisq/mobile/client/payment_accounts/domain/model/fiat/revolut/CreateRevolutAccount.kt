package network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount

data class CreateRevolutAccount(
    override val accountName: String,
    override val accountPayload: CreateRevolutAccountPayload,
) : CreatePaymentAccount
