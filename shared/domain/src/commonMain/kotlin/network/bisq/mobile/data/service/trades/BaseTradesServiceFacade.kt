package network.bisq.mobile.data.service.trades

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.data.model.TradeCancelReason
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.domain.repository.TradeCancelReasonRepository
import network.bisq.mobile.domain.repository.TradeStallClockRepository
import network.bisq.mobile.domain.service.trades.TradeAnalyticsTracker
import network.bisq.mobile.domain.utils.redactedSummary
import network.bisq.mobile.domain.utils.resultCatching

/**
 * Shared base for the client and node [TradesServiceFacade] implementations, owning the trade-funnel
 * analytics wiring so it lives in exactly one place instead of being copy-pasted into both facades.
 *
 * TODO The two implementations still duplicate more than this — notably the `selectedTrade` /
 *  `openTradeItems` / `closedTradesChangeTick` backing fields. Consolidating those is a larger,
 *  riskier cleanup (the node facade's state is entangled with bisq2-thread locking), so it is
 *  intentionally deferred to a follow-up rather than folded into this analytics change.
 */
abstract class BaseTradesServiceFacade(
    protected val analyticsService: AnalyticsService,
    tradeStallClockRepository: TradeStallClockRepository,
    private val tradeCancelReasonRepository: TradeCancelReasonRepository,
) : ServiceFacade(),
    TradesServiceFacade {
    private val _openTradesSynced = MutableStateFlow(false)
    override val openTradesSynced: StateFlow<Boolean> = _openTradesSynced.asStateFlow()

    /** Call once the node has delivered the open trades, and again with false when they are dropped. */
    protected fun setOpenTradesSynced(synced: Boolean) {
        _openTradesSynced.value = synced
    }

    private val _openTradesSyncFailed = MutableStateFlow(false)
    override val openTradesSyncFailed: StateFlow<Boolean> = _openTradesSyncFailed.asStateFlow()

    /** Call while the node cannot deliver the open trades at all, and again with false once it can. */
    protected fun setOpenTradesSyncFailed(failed: Boolean) {
        _openTradesSyncFailed.value = failed
    }

    // Scope-free by design: the tracker reads `serviceScope` fresh on every call (see the helpers
    // below), so it always launches on the facade's LIVE scope. `deactivate()` cancels and REPLACES
    // serviceScope, so a tracker that captured it once would silently go dead after the first
    // deactivate/reactivate cycle.
    private val tradeAnalyticsTracker by lazy {
        TradeAnalyticsTracker(analyticsService, stallClockRepository = tradeStallClockRepository)
    }

    /** Emit a lifecycle/funnel [AnalyticsEvent.Trade] (Taken/Completed/Cancelled/Rejected/Errored/…). */
    protected fun trackTrade(event: AnalyticsEvent.Trade) = tradeAnalyticsTracker.track(event)

    /**
     * Stall bucket for the currently selected trade. Capture BEFORE issuing the cancel request —
     * the cancellation itself transitions the trade state and would reset the clock to ~zero.
     */
    protected fun selectedTradeStallBucket(): AnalyticsEvent.Trade.StallBucket = tradeAnalyticsTracker.stallBucketFor(selectedTrade.value?.tradeId)

    /**
     * Wraps a user-driven confirm [action] with step-outcome analytics (CONFIRMED/FAILED/STALLED),
     * timing the currently selected trade's own state transition. With no selected trade it just runs
     * the action untracked. `serviceScope` is read fresh so the stall watch launches on the live scope.
     */
    protected suspend fun trackedAction(
        step: AnalyticsEvent.Trade.Step,
        action: suspend () -> Result<Unit>,
    ): Result<Unit> {
        val tradeState = selectedTrade.value?.bisqEasyTradeModel?.tradeState
        return if (tradeState != null) {
            tradeAnalyticsTracker.trackAction(step, tradeState, serviceScope, action)
        } else {
            action()
        }
    }

    /**
     * Saves the reason on this device so the cancelled trade screen can explain it, and removes it
     * again if the cancel fails. Saving or removing it can fail without failing the cancel; that is logged.
     */
    final override suspend fun cancelTradeForBannedAccountData(): Result<Unit> {
        // Captured before the request, while the trade being cancelled is still the selected one.
        val tradeId = selectedTrade.value?.tradeId
        // Saved before the request: on Connect the cancelled screen can show, and the trade be closed,
        // before the cancel response arrives.
        if (tradeId != null) saveCancelReason(tradeId, TradeCancelReason.BANNED_ACCOUNT_DATA)
        return cancelSelectedTradeForBannedAccountData().onFailure {
            if (tradeId != null) clearCancelReason(tradeId)
        }
    }

    /** Cancels the selected trade because the seller's account data is banned; see [cancelTradeForBannedAccountData]. */
    protected abstract suspend fun cancelSelectedTradeForBannedAccountData(): Result<Unit>

    private suspend fun saveCancelReason(
        tradeId: String,
        reason: TradeCancelReason,
    ) {
        resultCatching { tradeCancelReasonRepository.setReason(tradeId, reason) }
            .onFailure { log.e { "Failed to save the cancel reason: ${it.redactedSummary()}" } }
    }

    private suspend fun clearCancelReason(tradeId: String) {
        resultCatching { tradeCancelReasonRepository.clearId(tradeId) }
            .onFailure { log.e { "Failed to remove the cancel reason: ${it.redactedSummary()}" } }
    }

    /** Launch the open-trades observers (phase-reached / completed / errored). Call once from activate(). */
    protected fun observeTradesForAnalytics() {
        tradeAnalyticsTracker.observeTrades(serviceScope, openTradeItems) { it.tradeId }
    }
}
