package network.bisq.mobile.client.payment_accounts.data.model.crypto.other_crypto

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.crypto.CryptoPaymentAccountPayloadDto

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
