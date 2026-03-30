package network.bisq.mobile.domain.model.account.crypto

import network.bisq.mobile.domain.model.account.PaymentAccountPayload

data class OtherCryptoAssetAccountPayload(
    val currencyCode: String,
    val address: String,
    val isInstant: Boolean,
    val isAutoConf: Boolean? = null,
    val autoConfNumConfirmations: Int? = null,
    val autoConfMaxTradeAmount: Long? = null,
    val autoConfExplorerUrls: String? = null,
) : PaymentAccountPayload
