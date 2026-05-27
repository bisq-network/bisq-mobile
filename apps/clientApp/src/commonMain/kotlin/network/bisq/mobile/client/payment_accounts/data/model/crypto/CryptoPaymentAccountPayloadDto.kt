package network.bisq.mobile.client.payment_accounts.data.model.crypto

import network.bisq.mobile.client.payment_accounts.data.model.PaymentAccountPayloadDto

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
