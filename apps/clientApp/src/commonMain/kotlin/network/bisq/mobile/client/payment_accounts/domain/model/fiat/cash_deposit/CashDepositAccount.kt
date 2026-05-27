package network.bisq.mobile.client.payment_accounts.domain.model.fiat.cash_deposit

import network.bisq.mobile.domain.model.account.fiat.FiatPaymentAccount

data class CashDepositAccount(
    override val accountName: String,
    override val accountPayload: CashDepositAccountPayload,
    override val creationDate: String? = null,
    override val tradeLimitInfo: String? = null,
    override val tradeDuration: String? = null,
) : FiatPaymentAccount
