package network.bisq.mobile.client.common.domain.service.trades

import io.ktor.http.encodeURLParameter
import network.bisq.mobile.client.common.domain.service.trades.TradeEventTypeEnum.BTC_CONFIRMED
import network.bisq.mobile.client.common.domain.service.trades.TradeEventTypeEnum.BUYER_CONFIRM_FIAT_SENT
import network.bisq.mobile.client.common.domain.service.trades.TradeEventTypeEnum.BUYER_SEND_BITCOIN_PAYMENT_DATA
import network.bisq.mobile.client.common.domain.service.trades.TradeEventTypeEnum.CANCEL_TRADE
import network.bisq.mobile.client.common.domain.service.trades.TradeEventTypeEnum.CLOSE_TRADE
import network.bisq.mobile.client.common.domain.service.trades.TradeEventTypeEnum.REJECT_TRADE
import network.bisq.mobile.client.common.domain.service.trades.TradeEventTypeEnum.SELLER_CONFIRM_BTC_SENT
import network.bisq.mobile.client.common.domain.service.trades.TradeEventTypeEnum.SELLER_CONFIRM_FIAT_RECEIPT
import network.bisq.mobile.client.common.domain.service.trades.TradeEventTypeEnum.SELLER_SENDS_PAYMENT_ACCOUNT
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.data.model.trade.ClosedTradeListItemDto
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.core.pagination.PaginatedResponse
import network.bisq.mobile.domain.model.trade.TradeOutcomeFilter
import network.bisq.mobile.domain.model.trade.TradeRoleFilter
import network.bisq.mobile.domain.model.trade.TradeSort
import network.bisq.mobile.domain.utils.Logging

private enum class ClosedTradeSortBy { DATE, MARKET, QUOTE_AMOUNT, BASE_AMOUNT, ROLE }

private enum class ClosedTradeSortDirection { ASC, DESC }

private fun TradeSort?.toSortQueryValues(): Pair<String?, String?> =
    when (this) {
        TradeSort.NEWEST_FIRST -> ClosedTradeSortBy.DATE.name to ClosedTradeSortDirection.DESC.name
        TradeSort.OLDEST_FIRST -> ClosedTradeSortBy.DATE.name to ClosedTradeSortDirection.ASC.name
        TradeSort.AMOUNT_HIGH_LOW -> ClosedTradeSortBy.QUOTE_AMOUNT.name to ClosedTradeSortDirection.DESC.name
        TradeSort.AMOUNT_LOW_HIGH -> ClosedTradeSortBy.QUOTE_AMOUNT.name to ClosedTradeSortDirection.ASC.name
        null -> null to null
    }

private fun TradeRoleFilter.toRoleQueryValue(): String? =
    when (this) {
        TradeRoleFilter.ALL -> null
        TradeRoleFilter.BUYER -> "BUYER"
        TradeRoleFilter.SELLER -> "SELLER"
    }

private fun TradeOutcomeFilter.toStateQueryNames(): List<String> =
    when (this) {
        TradeOutcomeFilter.ALL -> emptyList()
        TradeOutcomeFilter.COMPLETED -> listOf(BisqEasyTradeStateEnum.BTC_CONFIRMED.name)
        TradeOutcomeFilter.CANCELLED ->
            listOf(
                BisqEasyTradeStateEnum.CANCELLED.name,
                BisqEasyTradeStateEnum.PEER_CANCELLED.name,
                BisqEasyTradeStateEnum.REJECTED.name,
                BisqEasyTradeStateEnum.PEER_REJECTED.name,
            )

        TradeOutcomeFilter.FAILED ->
            listOf(
                BisqEasyTradeStateEnum.FAILED.name,
                BisqEasyTradeStateEnum.FAILED_AT_PEER.name,
            )
    }

class TradesApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
    private val webSocketClientService: WebSocketClientService,
) : Logging {
    private val basePath = "trades"

    suspend fun takeOffer(
        offerId: String,
        baseSideAmount: Long,
        quoteSideAmount: Long,
        bitcoinPaymentMethod: String,
        fiatPaymentMethod: String,
    ): Result<TakeOfferResponse> {
        val takeOfferRequest =
            TakeOfferRequest(
                offerId,
                baseSideAmount,
                quoteSideAmount,
                bitcoinPaymentMethod,
                fiatPaymentMethod,
            )
        return webSocketApiClient.post(basePath, takeOfferRequest)
    }

    suspend fun rejectTrade(tradeId: String): Result<Unit> = webSocketApiClient.patch("$basePath/$tradeId/event", TradeEventVO(REJECT_TRADE))

    suspend fun cancelTrade(tradeId: String): Result<Unit> = webSocketApiClient.patch("$basePath/$tradeId/event", TradeEventVO(CANCEL_TRADE))

    suspend fun closeTrade(tradeId: String): Result<Unit> = webSocketApiClient.patch("$basePath/$tradeId/event", TradeEventVO(CLOSE_TRADE))

    suspend fun sellerSendsPaymentAccount(
        tradeId: String,
        paymentAccountData: String,
    ): Result<Unit> =
        webSocketApiClient.patch(
            "$basePath/$tradeId/event",
            TradeEventVO(SELLER_SENDS_PAYMENT_ACCOUNT, paymentAccountData),
        )

    suspend fun buyerSendBitcoinPaymentData(
        tradeId: String,
        bitcoinPaymentData: String,
    ): Result<Unit> =
        webSocketApiClient.patch(
            "$basePath/$tradeId/event",
            TradeEventVO(BUYER_SEND_BITCOIN_PAYMENT_DATA, bitcoinPaymentData),
        )

    suspend fun sellerConfirmFiatReceipt(tradeId: String): Result<Unit> = webSocketApiClient.patch("$basePath/$tradeId/event", TradeEventVO(SELLER_CONFIRM_FIAT_RECEIPT))

    suspend fun buyerConfirmFiatSent(tradeId: String): Result<Unit> = webSocketApiClient.patch("$basePath/$tradeId/event", TradeEventVO(BUYER_CONFIRM_FIAT_SENT))

    suspend fun sellerConfirmBtcSent(
        tradeId: String,
        paymentProof: String?,
    ): Result<Unit> = webSocketApiClient.patch("$basePath/$tradeId/event", TradeEventVO(SELLER_CONFIRM_BTC_SENT, paymentProof))

    suspend fun btcConfirmed(tradeId: String): Result<Unit> = webSocketApiClient.patch("$basePath/$tradeId/event", TradeEventVO(BTC_CONFIRMED))

    suspend fun getClosedTradesPaginated(
        page: Int,
        pageSize: Int,
        search: String? = null,
        sortBy: TradeSort? = null,
        role: TradeRoleFilter = TradeRoleFilter.ALL,
        outcome: TradeOutcomeFilter = TradeOutcomeFilter.ALL,
    ): Result<PaginatedResponse<ClosedTradeListItemDto>> {
        val (sortByQuery, directionQuery) = sortBy.toSortQueryValues()
        val query =
            buildList {
                add("page=$page")
                add("pageSize=$pageSize")
                search?.trim()?.takeIf { it.isNotEmpty() }?.let { add("search=${it.encodeURLParameter()}") }
                sortByQuery?.let { add("sortBy=$it") }
                directionQuery?.let { add("direction=$it") }
                role.toRoleQueryValue()?.let { add("role=$it") }
                outcome.toStateQueryNames().forEach { add("state=$it") }
            }.joinToString("&")
        return webSocketApiClient.get("$basePath/closed?$query")
    }
}
