package network.bisq.mobile.data.model.account.crypto

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.account.PaymentAccountPayloadDto

@Serializable
data class OtherCryptoAssetAccountPayloadDto(
    val currencyCode: String,
    val address: String,
    val isInstant: Boolean,
    val isAutoConf: Boolean? = null,
    val autoConfNumConfirmations: Int? = null,
    val autoConfMaxTradeAmount: Long? = null,
    val autoConfExplorerUrls: String? = null,
) : PaymentAccountPayloadDto
