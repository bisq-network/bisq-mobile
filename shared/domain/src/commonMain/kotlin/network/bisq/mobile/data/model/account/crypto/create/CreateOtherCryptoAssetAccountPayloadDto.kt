package network.bisq.mobile.data.model.account.crypto.create

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.account.create.CreatePaymentAccountPayloadDto

@Serializable
data class CreateOtherCryptoAssetAccountPayloadDto(
    val currencyCode: String,
    val address: String,
    val isInstant: Boolean,
    val isAutoConf: Boolean? = null,
    val autoConfNumConfirmations: Int? = null,
    val autoConfMaxTradeAmount: Long? = null,
    val autoConfExplorerUrls: String? = null,
) : CreatePaymentAccountPayloadDto
