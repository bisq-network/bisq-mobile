package network.bisq.mobile.client.payment_accounts.domain.model.crypto.other_crypto

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccountPayload

data class CreateOtherCryptoAssetAccountPayload(
    val currencyCode: String,
    val address: String,
    val isInstant: Boolean,
    val isAutoConf: Boolean? = null,
    val autoConfNumConfirmations: Int? = null,
    val autoConfMaxTradeAmount: Long? = null,
    val autoConfExplorerUrls: String? = null,
) : CreatePaymentAccountPayload
