package network.bisq.mobile.client.payment_accounts.domain.model.fiat.sepa

import network.bisq.mobile.domain.model.account.fiat.FiatPaymentAccount

data class SepaAccount(
    override val accountName: String,
    override val accountPayload: SepaAccountPayload,
    override val creationDate: String? = null,
    override val tradeLimitInfo: String? = null,
    override val tradeDuration: String? = null,
) : FiatPaymentAccount
