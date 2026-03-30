package network.bisq.mobile.domain.model.account.crypto

import network.bisq.mobile.domain.model.account.PaymentAccountPayload

data class MoneroAccountPayload(
    val currencyCode: String,
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
) : PaymentAccountPayload
