package network.bisq.mobile.client.payment_accounts.domain.model.fiat.wise

import network.bisq.mobile.domain.model.account.fiat.FiatPaymentAccount

data class WiseAccount(
    override val accountName: String,
    override val accountPayload: WiseAccountPayload,
    override val creationDate: String? = null,
    override val tradeLimitInfo: String,
    override val tradeDuration: String,
) : FiatPaymentAccount
