package network.bisq.mobile.domain.model.account.create.fiat

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount

data class CreateZelleAccount(
    override val accountName: String,
    override val accountPayload: CreateZelleAccountPayload,
) : CreatePaymentAccount
