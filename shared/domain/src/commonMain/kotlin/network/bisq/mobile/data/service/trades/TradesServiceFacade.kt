package network.bisq.mobile.data.service.trades

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import network.bisq.mobile.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.service.LifeCycleAware
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.core.pagination.PaginatedResponse
import network.bisq.mobile.domain.core.pagination.PaginationParams
import network.bisq.mobile.domain.model.trade.ClosedTradeListItem
import network.bisq.mobile.domain.model.trade.TradeOutcomeFilter
import network.bisq.mobile.domain.model.trade.TradeRoleFilter
import network.bisq.mobile.domain.model.trade.TradeSort

interface TradesServiceFacade : LifeCycleAware {
    val selectedTrade: StateFlow<TradeItemPresentationModel?>
    val openTradeItems: StateFlow<List<TradeItemPresentationModel>>

    /**
     * True once the open trades have been delivered at least once, so an [openTradeItems] that does not
     * hold a trade means the trade is gone rather than still on its way. Until then an empty list says
     * nothing, which is what [selectOpenTradeWhenSynced] waits on.
     */
    val openTradesSynced: StateFlow<Boolean>

    /**
     * Change signal for closed trades. Increments whenever the server pushes a closed-trades update
     * or the local closed-trades collection mutates. Consumers should use this to trigger re-fetching
     * paginated closed-trade history.
     */
    val closedTradesChangeTick: StateFlow<Int>

    suspend fun takeOffer(
        bisqEasyOffer: BisqEasyOfferVO,
        takersBaseSideAmount: MonetaryVO,
        takersQuoteSideAmount: MonetaryVO,
        bitcoinPaymentMethod: String,
        fiatPaymentMethod: String,
        takeOfferStatus: MutableStateFlow<TakeOfferStatus?>,
        takeOfferErrorMessage: MutableStateFlow<String?>,
    ): Result<String>

    fun selectOpenTrade(tradeId: String)

    /** [reason] comes from the optional chips on the interrupt dialog — analytics only (#1711). */
    suspend fun rejectTrade(
        reason: AnalyticsEvent.Trade.InterruptReason = AnalyticsEvent.Trade.InterruptReason.UNSPECIFIED,
    ): Result<Unit>

    /** [reason] comes from the optional chips on the interrupt dialog — analytics only (#1711). */
    suspend fun cancelTrade(
        reason: AnalyticsEvent.Trade.InterruptReason = AnalyticsEvent.Trade.InterruptReason.UNSPECIFIED,
    ): Result<Unit>

    suspend fun closeTrade(): Result<Unit>

    suspend fun sellerSendsPaymentAccount(paymentAccountData: String): Result<Unit>

    suspend fun buyerSendBitcoinPaymentData(bitcoinPaymentData: String): Result<Unit>

    suspend fun sellerConfirmFiatReceipt(): Result<Unit>

    suspend fun buyerConfirmFiatSent(): Result<Unit>

    suspend fun sellerConfirmBtcSent(paymentProof: String?): Result<Unit>

    suspend fun btcConfirmed(): Result<Unit>

    suspend fun exportTradeDate(): Result<Unit>

    fun resetSelectedTradeToNull()

    suspend fun getClosedTradesPaginated(
        params: PaginationParams,
        search: String? = null,
        sortBy: TradeSort? = null,
        outcomeFilter: TradeOutcomeFilter = TradeOutcomeFilter.ALL,
        roleFilter: TradeRoleFilter = TradeRoleFilter.ALL,
    ): Result<PaginatedResponse<ClosedTradeListItem>>
}

/**
 * Selects [tradeId] and returns it, retrying while the open trades sync in. A deep link or a
 * notification tap opens a trade right after the app connects, when the list can still be arriving,
 * so a plain snapshot read reports a trade that does exist as missing. Returns null once the trades
 * have synced without it, which means genuinely absent and is what the callers' not-found dialog is
 * for.
 */
suspend fun TradesServiceFacade.selectOpenTradeWhenSynced(tradeId: String): TradeItemPresentationModel? {
    var trade: TradeItemPresentationModel? = null
    // Both flows replay their current value, so a trade already in the list resolves without waiting.
    // The lookup goes through the facade rather than a local find because the facade owns the id
    // matching (the client also accepts a short id), and it runs in the collector, not the transform,
    // since combine may call the transform for values that never reach the predicate.
    openTradeItems
        .combine(openTradesSynced) { _, synced -> synced }
        .first { synced ->
            selectOpenTrade(tradeId)
            trade = selectedTrade.value
            trade != null || synced
        }
    return trade
}
