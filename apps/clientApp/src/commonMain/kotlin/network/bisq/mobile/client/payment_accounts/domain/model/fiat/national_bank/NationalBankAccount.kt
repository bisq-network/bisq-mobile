package network.bisq.mobile.client.payment_accounts.domain.model.fiat.national_bank

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccount

data class NationalBankAccount(
    override val accountName: String,
    override val accountPayload: NationalBankAccountPayload,
    override val creationDate: String? = null,
    override val tradeLimitInfo: String? = null,
    override val tradeDuration: String? = null,
) : BankAccount
