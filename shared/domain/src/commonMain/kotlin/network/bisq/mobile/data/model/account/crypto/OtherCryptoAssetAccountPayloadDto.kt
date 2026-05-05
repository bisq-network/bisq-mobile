package network.bisq.mobile.data.model.account.crypto

import kotlinx.serialization.Serializable

@Serializable
data class OtherCryptoAssetAccountPayloadDto(
    override val currencyCode: String,
    override val currencyName: String,
    override val address: String,
    override val isInstant: Boolean,
    override val isAutoConf: Boolean? = null,
    override val autoConfNumConfirmations: Int? = null,
    override val autoConfMaxTradeAmount: Long? = null,
    override val autoConfExplorerUrls: String? = null,
    override val supportAutoConf: Boolean,
) : CryptoPaymentAccountPayloadDto
