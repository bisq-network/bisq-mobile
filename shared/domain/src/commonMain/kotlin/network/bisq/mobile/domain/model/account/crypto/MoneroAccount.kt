package network.bisq.mobile.domain.model.account.crypto

import network.bisq.mobile.domain.model.account.PaymentAccount

data class MoneroAccount(
    override val accountName: String,
    override val accountPayload: MoneroAccountPayload,
    override val creationDate: Long?,
) : PaymentAccount
