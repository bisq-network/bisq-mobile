package network.bisq.mobile.client.service.trades

import kotlinx.serialization.Serializable

@Serializable
enum class TradeEventTypeEnum {
    REJECT_TRADE,
    CANCEL_TRADE,
    CLOSE_TRADE,
    SELLER_SENDS_PAYMENT_ACCOUNT,
    BUYER_SEND_BITCOIN_PAYMENT_DATA,
    SELLER_CONFIRM_FIAT_RECEIPT,
    BUYER_CONFIRM_FIAT_SENT,
    SELLER_CONFIRM_BTC_SENT,
    BTC_CONFIRMED,
}