package network.bisq.mobile.domain.model.account.crypto

data class OtherCryptoAssetAccountPayload(
    override val currencyCode: String,
    override val currencyName: String,
    override val address: String,
    override val isInstant: Boolean,
    override val isAutoConf: Boolean? = null,
    override val autoConfNumConfirmations: Int? = null,
    override val autoConfMaxTradeAmount: Long? = null,
    override val autoConfExplorerUrls: String? = null,
    override val supportAutoConf: Boolean,
) : CryptoPaymentAccountPayload
