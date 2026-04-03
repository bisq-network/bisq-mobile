package network.bisq.mobile.data.model.account.crypto

import kotlinx.serialization.Serializable

@Serializable
data class OtherCryptoAssetAccountPayloadDto(
    val currencyCode: String,
    override val currencyName: String,
    override val address: String,
    override val isInstant: Boolean,
    override val isAutoConf: Boolean?,
    override val autoConfNumConfirmations: Int?,
    override val autoConfMaxTradeAmount: Long?,
    override val autoConfExplorerUrls: String?,
) : CryptoPaymentAccountPayloadDto
