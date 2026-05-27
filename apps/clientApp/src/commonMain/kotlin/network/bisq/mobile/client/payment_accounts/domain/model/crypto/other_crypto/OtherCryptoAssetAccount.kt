package network.bisq.mobile.client.payment_accounts.domain.model.crypto.other_crypto

import network.bisq.mobile.client.payment_accounts.domain.model.crypto.CryptoPaymentAccount

data class OtherCryptoAssetAccount(
    override val accountName: String,
    override val accountPayload: OtherCryptoAssetAccountPayload,
    override val creationDate: String?,
    override val tradeLimitInfo: String?,
    override val tradeDuration: String?,
) : CryptoPaymentAccount
