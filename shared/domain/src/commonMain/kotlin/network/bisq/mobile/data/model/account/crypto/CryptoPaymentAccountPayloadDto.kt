package network.bisq.mobile.data.model.account.crypto

import network.bisq.mobile.data.model.account.PaymentAccountPayloadDto

interface CryptoPaymentAccountPayloadDto : PaymentAccountPayloadDto {
    val currencyName: String
    val currencyCode: String
    val address: String
    val isInstant: Boolean
    val isAutoConf: Boolean?
    val autoConfNumConfirmations: Int?
    val autoConfMaxTradeAmount: Long?
    val autoConfExplorerUrls: String?
    val supportAutoConf: Boolean
}
