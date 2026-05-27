package network.bisq.mobile.client.payment_accounts.domain.model.fiat.wise

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount

data class CreateWiseAccount(
    override val accountName: String,
    override val accountPayload: CreateWiseAccountPayload,
) : CreatePaymentAccount
