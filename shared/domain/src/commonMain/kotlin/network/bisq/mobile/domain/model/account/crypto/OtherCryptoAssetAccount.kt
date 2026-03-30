package network.bisq.mobile.domain.model.account.crypto

import network.bisq.mobile.domain.model.account.PaymentAccount

data class OtherCryptoAssetAccount(
    override val accountName: String,
    override val accountPayload: OtherCryptoAssetAccountPayload,
    override val creationDate: Long?,
) : PaymentAccount
