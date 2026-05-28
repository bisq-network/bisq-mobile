package network.bisq.mobile.client.payment_accounts.domain.model.fiat.ach_transfer

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount

data class CreateAchTransferAccount(
    override val accountName: String,
    override val accountPayload: CreateAchTransferAccountPayload,
) : CreatePaymentAccount
