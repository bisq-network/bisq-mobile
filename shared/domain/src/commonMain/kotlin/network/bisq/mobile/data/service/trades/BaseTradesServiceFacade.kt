package network.bisq.mobile.data.service.trades

import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.domain.service.trades.TradeAnalyticsTracker

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
) : ServiceFacade(),
    TradesServiceFacade {
    // Scope-free by design: the tracker reads `serviceScope` fresh on every call (see the helpers
    // below), so it always launches on the facade's LIVE scope. `deactivate()` cancels and REPLACES
    // serviceScope, so a tracker that captured it once would silently go dead after the first
    // deactivate/reactivate cycle.
    private val tradeAnalyticsTracker by lazy { TradeAnalyticsTracker(analyticsService) }

    /** Emit a lifecycle/funnel [AnalyticsEvent.Trade] (Taken/Completed/Cancelled/Rejected/Errored/…). */
    protected fun trackTrade(event: AnalyticsEvent.Trade) = tradeAnalyticsTracker.track(event)

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

    /** Launch the open-trades observers (phase-reached / completed / errored). Call once from activate(). */
    protected fun observeTradesForAnalytics() {
        tradeAnalyticsTracker.observeTrades(serviceScope, openTradeItems) { it.tradeId }
    }
}
