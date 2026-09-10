package network.bisq.mobile.data.service.trades

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import network.bisq.mobile.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
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
     * True while the open trades cannot be delivered at all: on the client, a TRADES subscribe that
     * failed, which is only retried on the next reconnect. [openTradesSynced] will not turn true until
     * this clears, so a wait on it gives up instead and the trade reads as absent. Never true on the
     * node, whose trades come from its own store.
     */
    val openTradesSyncFailed: StateFlow<Boolean>

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
 * Selects [tradeId] and returns it, waiting while the open trades sync in. A deep link or a
 * notification tap opens a trade right after the app connects, when the list can still be arriving,
 * so a plain snapshot read reports a trade that does exist as missing. Returns null once the trades
 * have synced without it, which means genuinely absent, or once the sync has failed and is not coming;
 * both are what the callers' not-found dialog is for. The wait has no bound: the data layer knows
 * when the trades have arrived and when they are not going to, and a duration would only be wrong in
 * one of the two directions.
 *
 * The lookup is local and the selection happens once, at the end: selecting on every emission would
 * write the shared [TradesServiceFacade.selectedTrade] for every waiter, and two screens waiting on
 * different trades (a deep link and a chat notification) would overwrite each other's selection while
 * the trade actions all read that one global.
 */
suspend fun TradesServiceFacade.selectOpenTradeWhenSynced(tradeId: String): TradeItemPresentationModel? {
    // Every flow replays its current value, so a trade already in the list resolves without waiting.
    val trade =
        combine(openTradeItems, openTradesSynced, openTradesSyncFailed) { items, synced, failed ->
            items.find { it.tradeId == tradeId } to (synced || failed)
        }.first { (found, settled) -> found != null || settled }
            .first ?: return null
    selectOpenTrade(trade.tradeId)
    return trade
}

/**
 * Whether this user has ever traded with [profileId] — open trades first (in memory, free), then
 * the closed-trade history through [TradesServiceFacade.getClosedTradesPaginated], newest page
 * first, stopping at the first match.
 *
 * Degrades to false rather than failing: on Bisq Connect the closed-trades API is capability-gated,
 * so an old trusted node answers the paginated call with a failure — the caller's gate (the peer
 * profile also admits contacts) is designed to tolerate that under-report. The page cap
 * ([HAS_TRADED_WITH_MAX_PAGES] × the max page size, i.e. 5000 closed trades — far beyond any
 * realistic mobile history) bounds the scan; a peer beyond it reads as not-traded, which errs the
 * same harmless direction.
 */
suspend fun TradesServiceFacade.hasTradedWith(profileId: String): Boolean {
    if (openTradeItems.value.any { it.peersUserProfile.id == profileId }) return true
    var page = PaginationParams.DEFAULT_PAGE
    while (page <= HAS_TRADED_WITH_MAX_PAGES) {
        val response =
            getClosedTradesPaginated(PaginationParams(page, PaginationParams.MAX_PAGE_SIZE))
                .getOrNull() ?: return false
        if (response.items.any { it.peersUserProfile.id == profileId }) return true
        if (response.items.isEmpty() || page >= response.totalPages) return false
        page++
    }
    return false
}

private const val HAS_TRADED_WITH_MAX_PAGES = 50
