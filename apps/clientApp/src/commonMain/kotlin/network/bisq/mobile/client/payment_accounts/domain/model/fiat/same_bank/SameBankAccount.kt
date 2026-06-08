package network.bisq.mobile.client.payment_accounts.domain.model.fiat.same_bank

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccount

data class SameBankAccount(
    override val accountName: String,
    override val accountPayload: SameBankAccountPayload,
    override val creationDate: String? = null,
    override val tradeLimitInfo: String? = null,
    override val tradeDuration: String? = null,
) : BankAccount
