package network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle

import network.bisq.mobile.domain.model.account.fiat.FiatPaymentAccount

data class ZelleAccount(
    override val accountName: String,
    override val accountPayload: ZelleAccountPayload,
    override val creationDate: String? = null,
    override val tradeLimitInfo: String? = null,
    override val tradeDuration: String? = null,
) : FiatPaymentAccount
