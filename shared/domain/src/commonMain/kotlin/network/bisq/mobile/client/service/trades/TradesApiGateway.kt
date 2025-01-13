package network.bisq.mobile.client.service.trades

import network.bisq.mobile.client.service.trades.TradeEventTypeEnum.BTC_CONFIRMED
import network.bisq.mobile.client.service.trades.TradeEventTypeEnum.BUYER_CONFIRM_FIAT_SENT
import network.bisq.mobile.client.service.trades.TradeEventTypeEnum.BUYER_SEND_BITCOIN_PAYMENT_DATA
import network.bisq.mobile.client.service.trades.TradeEventTypeEnum.CANCEL_TRADE
import network.bisq.mobile.client.service.trades.TradeEventTypeEnum.CLOSE_TRADE
import network.bisq.mobile.client.service.trades.TradeEventTypeEnum.REJECT_TRADE
import network.bisq.mobile.client.service.trades.TradeEventTypeEnum.SELLER_CONFIRM_BTC_SENT
import network.bisq.mobile.client.service.trades.TradeEventTypeEnum.SELLER_CONFIRM_FIAT_RECEIPT
import network.bisq.mobile.client.service.trades.TradeEventTypeEnum.SELLER_SENDS_PAYMENT_ACCOUNT
import network.bisq.mobile.client.websocket.WebSocketClient
import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.domain.utils.Logging

class TradesApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
    private val webSocketClient: WebSocketClient,
) : Logging {
    private val basePath = "trades"

    suspend fun takeOffer(
        offerId: String,
        baseSideAmount: Long,
        quoteSideAmount: Long,
        bitcoinPaymentMethod: String,
        fiatPaymentMethod: String
    ): Result<TakeOfferResponse> {
        val takeOfferRequest = TakeOfferRequest(
            offerId,
            baseSideAmount,
            quoteSideAmount,
            bitcoinPaymentMethod,
            fiatPaymentMethod
        )
        return webSocketApiClient.post(basePath, takeOfferRequest)
    }

    suspend fun rejectTrade(tradeId: String): Result<Unit> {
        return webSocketApiClient.patch("$basePath/$tradeId/event", TradeEventVO(REJECT_TRADE))
    }

    suspend fun cancelTrade(tradeId: String): Result<Unit> {
        return webSocketApiClient.patch("$basePath/$tradeId/event", TradeEventVO(CANCEL_TRADE))
    }

    suspend fun closeTrade(tradeId: String): Result<Unit> {
        return webSocketApiClient.patch("$basePath/$tradeId/event", TradeEventVO(CLOSE_TRADE))
    }

    suspend fun sellerSendsPaymentAccount(tradeId: String, paymentAccountData: String): Result<Unit> {
        return webSocketApiClient.patch("$basePath/$tradeId/event", TradeEventVO(SELLER_SENDS_PAYMENT_ACCOUNT, paymentAccountData))
    }

    suspend fun buyerSendBitcoinPaymentData(tradeId: String, bitcoinPaymentData: String): Result<Unit> {
        return webSocketApiClient.patch("$basePath/$tradeId/event", TradeEventVO(BUYER_SEND_BITCOIN_PAYMENT_DATA, bitcoinPaymentData))
    }

    suspend fun sellerConfirmFiatReceipt(tradeId: String): Result<Unit> {
        return webSocketApiClient.patch("$basePath/$tradeId/event", TradeEventVO(SELLER_CONFIRM_FIAT_RECEIPT))
    }

    suspend fun buyerConfirmFiatSent(tradeId: String): Result<Unit> {
        return webSocketApiClient.patch("$basePath/$tradeId/event", TradeEventVO(BUYER_CONFIRM_FIAT_SENT))
    }

    suspend fun sellerConfirmBtcSent(tradeId: String, paymentProof: String?): Result<Unit> {
        return webSocketApiClient.patch("$basePath/$tradeId/event", TradeEventVO(SELLER_CONFIRM_BTC_SENT, paymentProof))
    }

    suspend fun btcConfirmed(tradeId: String): Result<Unit> {
        return webSocketApiClient.patch("$basePath/$tradeId/event", TradeEventVO(BTC_CONFIRMED))
    }
}

