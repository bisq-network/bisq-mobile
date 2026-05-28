package network.bisq.mobile.client.payment_accounts.domain.model.fiat.ach_transfer

import network.bisq.mobile.domain.model.account.fiat.FiatPaymentAccount

data class AchTransferAccount(
    override val accountName: String,
    override val accountPayload: AchTransferAccountPayload,
    override val creationDate: String? = null,
    override val tradeLimitInfo: String? = null,
    override val tradeDuration: String? = null,
) : FiatPaymentAccount
