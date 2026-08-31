package network.bisq.mobile.domain.service.trades

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.domain.utils.TimeUtils
import network.bisq.mobile.domain.utils.TradeOutOfSyncDetector

/**
 * Shared trade-funnel analytics used by both the client and node [TradesServiceFacade] implementations
 * (each passes its own scope and trade flows, so the two app types are tracked independently). The
 * scope is passed per call rather than held, so it is always the facade's LIVE serviceScope — which a
 * deactivate/reactivate cycle cancels and REPLACES.
 *
 * It emits three signals — all privacy-clean sealed [AnalyticsEvent.Trade] slugs, no trade id / amount
 * / peer:
 *  - **step outcomes** via [trackAction]: `FAILED` (request errored), `CONFIRMED` (the user's own next
 *    state advanced), `STALLED` (accepted but the local state never advanced). We only time the user's
 *    OWN state transition — a fast local dispatch — so a trade legitimately waiting hours/days on the
 *    counterparty never trips the stall watch.
 *  - **lifecycle** via [track]: `Taken` / `Cancelled` / `Rejected` on the corresponding actions.
 *  - **completion & errors** via [observeTrades]: `Completed` when a trade reaches `BTC_CONFIRMED`,
 *    `Errored` (+ captured exception) when a trade surfaces a protocol/peer error, and
 *    `OutOfSyncDetected` when a trade sits in INIT past the [TradeOutOfSyncDetector] threshold —
 *    the field measurement of the stuck-FSM desync the out-of-sync recovery pane exists for.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TradeAnalyticsTracker(
    private val analyticsService: AnalyticsService,
    private val stallTimeoutMs: Long = DEFAULT_STALL_TIMEOUT_MS,
    private val clock: () -> Long = { DateUtils.now() },
) : Logging {
    companion object {
        // Generous for Tor round-trips. Only the user's OWN local state transition is timed (fast), so
        // this never trips on a legitimate counterparty wait, which can span hours/days.
        const val DEFAULT_STALL_TIMEOUT_MS = 45_000L

        // The out-of-sync signal is time-based (a stuck trade never transitions), so it needs a
        // recheck ticker. One minute against a 10-minute detection threshold keeps detection
        // near-immediate without meaningful wakeup cost.
        const val OUT_OF_SYNC_RECHECK_MS = 60_000L
    }

    // In-memory only, by design: the trade DTO carries no per-state timestamps, so a
    // transition the app never saw this session has an unknowable age — stallBucketFor reports it
    // as UNKNOWN rather than fabricating a short stall. Entries are evicted as trades leave the
    // open list. If UNKNOWN dominates cancel events in the monthly report, the follow-up is
    // persisting this map, not widening the buckets.
    private val lastKnownStateByTradeId = mutableMapOf<String, BisqEasyTradeStateEnum>()
    private val lastTransitionAtMsByTradeId = mutableMapOf<String, Long>()

    /**
     * Bucketed time since [tradeId]'s last witnessed state transition — for stamping onto
     * [AnalyticsEvent.Trade.Cancelled]. Callers MUST capture this BEFORE issuing the cancel
     * request: the cancellation itself transitions the trade state, which resets the clock to
     * ~zero.
     */
    fun stallBucketFor(tradeId: String?): AnalyticsEvent.Trade.StallBucket {
        val lastTransitionAtMs =
            tradeId?.let { lastTransitionAtMsByTradeId[it] }
                ?: return AnalyticsEvent.Trade.StallBucket.UNKNOWN
        return AnalyticsEvent.Trade.StallBucket.fromMillis(clock() - lastTransitionAtMs)
    }

    /**
     * A first sighting (no previous state) is NOT a transition — it is either a freshly taken trade
     * or a pre-existing one loaded on startup, and only the former's age would be ~zero. StateFlows
     * also replay their current value on every re-subscription (the observers re-subscribe whenever
     * the open-trades list changes), which the same-state check discards.
     */
    private fun recordTransition(
        id: String,
        state: BisqEasyTradeStateEnum,
    ) {
        val previous = lastKnownStateByTradeId.put(id, state)
        if (previous != null && previous != state) {
            lastTransitionAtMsByTradeId[id] = clock()
        }
    }

    /**
     * Runs a user-driven confirm action and records its outcome. On failure emits `FAILED` and captures
     * the exception; on success watches [tradeState] for the user's own transition away from its current
     * value, emitting `CONFIRMED` if it advances within [stallTimeoutMs] or `STALLED` otherwise. The
     * action's [Result] is returned unchanged — the stall watch runs in the background.
     *
     * A [CancellationException] in [action]'s failure is gated with [ensureActive]: if this coroutine
     * is already cancelled (navigate-away), the exception is rethrown and not reported. If the caller
     * is still active (request timeout), it is reported as `FAILED`.
     */
    suspend fun trackAction(
        step: AnalyticsEvent.Trade.Step,
        tradeState: StateFlow<BisqEasyTradeStateEnum>,
        scope: CoroutineScope,
        action: suspend () -> Result<Unit>,
    ): Result<Unit> {
        val before = tradeState.value
        val result = action()
        result
            .onFailure { e ->
                if (e is CancellationException) {
                    currentCoroutineContext().ensureActive()
                }
                analyticsService.track(AnalyticsEvent.Trade.Action(step, AnalyticsEvent.Trade.Outcome.FAILED))
                analyticsService.captureException(TradeActionException(step, e))
            }.onSuccess {
                scope.launch {
                    // Any transition away from `before` counts as CONFIRMED — including a jump to an
                    // interrupt/error state (rare; those also surface as a separate `Errored` event, so
                    // the two are cross-referenceable). We deliberately don't assert forward progress:
                    // the goal is "did the user's local state react at all", not protocol correctness.
                    val advanced = withTimeoutOrNull(stallTimeoutMs) { tradeState.first { it != before } } != null
                    val outcome = if (advanced) AnalyticsEvent.Trade.Outcome.CONFIRMED else AnalyticsEvent.Trade.Outcome.STALLED
                    if (!advanced) log.w { "Trade step ${step.slug} stalled — accepted but state stayed $before for ${stallTimeoutMs}ms" }
                    analyticsService.track(AnalyticsEvent.Trade.Action(step, outcome))
                }
            }
        return result
    }

    fun track(event: AnalyticsEvent.Trade) = analyticsService.track(event)

    /**
     * Observes all open trades for terminal completion and protocol/peer errors, emitting once per
     * trade id. Launch once on facade activation. [tradeId] extracts a stable id from a trade item.
     */
    fun observeTrades(
        scope: CoroutineScope,
        openTradeItems: StateFlow<List<TradeItemPresentationModel>>,
        tradeId: (TradeItemPresentationModel) -> String,
    ) {
        val completed = mutableSetOf<String>()
        val errored = mutableSetOf<String>()
        val reachedPhases = mutableSetOf<String>()
        val outOfSync = mutableSetOf<String>()

        // Phase reached (state-based): emit once per (trade, phase) as a trade enters each phase,
        // whether or not the user is viewing it — the funnel denominator to compare against the
        // view-based PhaseOpened.
        scope.launch {
            openTradeItems
                .flatMapLatest { trades ->
                    merge(
                        *trades
                            .map { item ->
                                item.bisqEasyTradeModel.tradeState.map { state ->
                                    tradeId(item) to state.toTradePhase(item.bisqEasyTradeModel.isSeller)
                                }
                            }.toTypedArray(),
                    )
                }.collect { (id, phase) ->
                    if (phase != null && reachedPhases.add("$id:${phase.slug}")) {
                        analyticsService.track(AnalyticsEvent.Trade.PhaseReached(phase))
                    }
                }
        }

        // Completion: BTC_CONFIRMED. flatMapLatest re-subscribes to the current trades' state flows
        // whenever the open-trade list changes. The same stream feeds the stall clock — every state
        // change is a witnessed transition.
        scope.launch {
            openTradeItems
                .flatMapLatest { trades ->
                    // Evict stall entries for trades no longer open, so the maps stay bounded.
                    val openIds = trades.mapTo(mutableSetOf()) { tradeId(it) }
                    lastKnownStateByTradeId.keys.retainAll(openIds)
                    lastTransitionAtMsByTradeId.keys.retainAll(openIds)
                    merge(*trades.map { item -> item.bisqEasyTradeModel.tradeState.map { state -> tradeId(item) to state } }.toTypedArray())
                }.collect { (id, state) ->
                    recordTransition(id, state)
                    if (state == BisqEasyTradeStateEnum.BTC_CONFIRMED && completed.add(id)) {
                        analyticsService.track(AnalyticsEvent.Trade.Completed)
                    }
                }
        }

        // Out-of-sync: a trade parked in INIT past the detector threshold is desynced from the peer
        // (the stuck-FSM bug the recovery pane exists for — see TradeOutOfSyncDetector). This signal
        // is time-based, not state-based — a stuck trade never transitions — so it rechecks on a
        // ticker instead of observing state flows. Once per trade per session.
        scope.launch {
            openTradeItems
                .flatMapLatest { trades ->
                    TimeUtils.tickerFlow(OUT_OF_SYNC_RECHECK_MS).map { trades }
                }.collect { trades ->
                    trades
                        .filter { TradeOutOfSyncDetector.isOutOfSync(it.bisqEasyTradeModel, clock()) }
                        .forEach { item ->
                            if (outOfSync.add(tradeId(item))) {
                                log.w { "Trade detected out of sync — stuck in INIT past the detection threshold" }
                                analyticsService.track(AnalyticsEvent.Trade.OutOfSyncDetected)
                            }
                        }
                }
        }

        // Errors: the trade's own error OR the peer's error appearing. These are independent
        // observables — a peer-side failure sets peersErrorMessage without ever touching errorMessage —
        // so we observe both per trade and let the per-trade `errored` guard collapse them to a single
        // Errored (whichever surfaces first wins).
        scope.launch {
            openTradeItems
                .flatMapLatest { trades ->
                    merge(
                        *trades
                            .flatMap { item ->
                                val id = tradeId(item)
                                val model = item.bisqEasyTradeModel
                                listOf(
                                    model.errorMessage.map { Triple(id, it, model.errorStackTrace.value) },
                                    model.peersErrorMessage.map { Triple(id, it, model.peersErrorStackTrace.value) },
                                )
                            }.toTypedArray(),
                    )
                }.collect { (id, message, stackTrace) ->
                    if (message != null && errored.add(id)) {
                        analyticsService.track(AnalyticsEvent.Trade.Errored)
                        analyticsService.captureException(TradeProtocolException(message, stackTrace))
                    }
                }
        }
    }
}

/** Wraps a failed trade-step request so GlitchTip groups it by step rather than by generic HTTP error. */
class TradeActionException(
    step: AnalyticsEvent.Trade.Step,
    cause: Throwable,
) : Exception("Trade step '${step.slug}' request failed: ${cause.message}", cause)

/** A protocol/peer error surfaced on a trade, carrying the node-reported message + stack for grouping. */
class TradeProtocolException(
    message: String,
    stackTrace: String?,
) : Exception("Trade protocol error: $message${stackTrace?.let { "\n$it" } ?: ""}")
