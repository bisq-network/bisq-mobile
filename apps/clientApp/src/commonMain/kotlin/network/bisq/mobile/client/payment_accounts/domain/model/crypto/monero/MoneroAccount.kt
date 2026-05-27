package network.bisq.mobile.client.payment_accounts.domain.model.crypto.monero

import network.bisq.mobile.client.payment_accounts.domain.model.crypto.CryptoPaymentAccount

data class MoneroAccount(
    override val accountName: String,
    override val accountPayload: MoneroAccountPayload,
    override val creationDate: String?,
    override val tradeLimitInfo: String?,
    override val tradeDuration: String?,
) : CryptoPaymentAccount
