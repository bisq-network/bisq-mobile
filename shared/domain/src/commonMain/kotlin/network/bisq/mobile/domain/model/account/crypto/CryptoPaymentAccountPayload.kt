package network.bisq.mobile.domain.model.account.crypto

import network.bisq.mobile.domain.model.account.PaymentAccountPayload

interface CryptoPaymentAccountPayload : PaymentAccountPayload {
    val currencyName: String?
    val address: String
    val isInstant: Boolean
    val isAutoConf: Boolean?
    val autoConfNumConfirmations: Int?
    val autoConfMaxTradeAmount: Long?
    val autoConfExplorerUrls: String?
}
