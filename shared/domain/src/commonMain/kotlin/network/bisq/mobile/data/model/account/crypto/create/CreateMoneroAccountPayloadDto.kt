package network.bisq.mobile.data.model.account.crypto.create

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.account.create.CreatePaymentAccountPayloadDto

@Serializable
data class CreateMoneroAccountPayloadDto(
    val address: String,
    val isInstant: Boolean,
    val isAutoConf: Boolean? = null,
    val autoConfNumConfirmations: Int? = null,
    val autoConfMaxTradeAmount: Long? = null,
    val autoConfExplorerUrls: String? = null,
    val useSubAddresses: Boolean,
    val mainAddress: String? = null,
    val privateViewKey: String? = null,
    val subAddress: String? = null,
    val accountIndex: Int? = null,
    val initialSubAddressIndex: Int? = null,
) : CreatePaymentAccountPayloadDto
