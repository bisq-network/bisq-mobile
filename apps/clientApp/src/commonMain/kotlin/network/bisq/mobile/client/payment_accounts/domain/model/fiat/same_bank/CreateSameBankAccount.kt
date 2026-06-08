package network.bisq.mobile.client.payment_accounts.domain.model.fiat.same_bank

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount

data class CreateSameBankAccount(
    override val accountName: String,
    override val accountPayload: CreateSameBankAccountPayload,
) : CreatePaymentAccount
