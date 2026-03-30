package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.domain.model.account.PaymentAccount

data class UserDefinedFiatAccount(
    override val accountName: String,
    override val accountPayload: UserDefinedFiatAccountPayload,
    override val creationDate: Long? = null,
) : PaymentAccount
