package network.bisq.mobile.client.payment_accounts.data.model.crypto.monero

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.crypto.CryptoPaymentAccountPayloadDto

@Serializable
data class MoneroAccountPayloadDto(
    val useSubAddresses: Boolean,
    val mainAddress: String? = null,
    val privateViewKey: String? = null,
    val subAddress: String? = null,
    val accountIndex: Int? = null,
    val initialSubAddressIndex: Int? = null,
    override val currencyName: String,
    override val currencyCode: String,
    override val address: String,
    override val isInstant: Boolean,
    override val isAutoConf: Boolean? = null,
    override val autoConfNumConfirmations: Int? = null,
    override val autoConfMaxTradeAmount: Long? = null,
    override val autoConfExplorerUrls: String? = null,
    override val supportAutoConf: Boolean,
) : CryptoPaymentAccountPayloadDto
