package network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut

import network.bisq.mobile.domain.model.account.fiat.FiatPaymentAccount

data class RevolutAccount(
    override val accountName: String,
    override val accountPayload: RevolutAccountPayload,
    override val creationDate: String? = null,
    override val tradeLimitInfo: String,
    override val tradeDuration: String,
) : FiatPaymentAccount
