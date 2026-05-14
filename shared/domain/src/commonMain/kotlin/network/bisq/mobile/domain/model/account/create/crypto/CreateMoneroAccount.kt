package network.bisq.mobile.domain.model.account.create.crypto

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount

data class CreateMoneroAccount(
    override val accountName: String,
    override val accountPayload: CreateMoneroAccountPayload,
) : CreatePaymentAccount
