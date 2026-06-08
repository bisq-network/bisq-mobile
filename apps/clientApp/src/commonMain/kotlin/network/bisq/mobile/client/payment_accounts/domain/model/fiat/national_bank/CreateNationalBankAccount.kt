package network.bisq.mobile.client.payment_accounts.domain.model.fiat.national_bank

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount

data class CreateNationalBankAccount(
    override val accountName: String,
    override val accountPayload: CreateNationalBankAccountPayload,
) : CreatePaymentAccount
