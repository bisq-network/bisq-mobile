package network.bisq.mobile.client.payment_accounts.domain.model.crypto.other_crypto

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount

data class CreateOtherCryptoAssetAccount(
    override val accountName: String,
    override val accountPayload: CreateOtherCryptoAssetAccountPayload,
) : CreatePaymentAccount
