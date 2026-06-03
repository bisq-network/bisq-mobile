package network.bisq.mobile.client.payment_accounts.domain.model.fiat.sepa

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount

data class CreateSepaAccount(
    override val accountName: String,
    override val accountPayload: CreateSepaAccountPayload,
) : CreatePaymentAccount
