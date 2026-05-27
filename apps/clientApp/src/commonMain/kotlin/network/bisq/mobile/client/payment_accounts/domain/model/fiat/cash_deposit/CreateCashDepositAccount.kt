package network.bisq.mobile.client.payment_accounts.domain.model.fiat.cash_deposit

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount

data class CreateCashDepositAccount(
    override val accountName: String,
    override val accountPayload: CreateCashDepositAccountPayload,
) : CreatePaymentAccount
