package network.bisq.mobile.domain.model.account.crypto

data class OtherCryptoAssetAccount(
    override val accountName: String,
    override val accountPayload: OtherCryptoAssetAccountPayload,
    override val creationDate: String?,
    override val tradeLimitInfo: String?,
    override val tradeDuration: String?,
) : CryptoPaymentAccount
