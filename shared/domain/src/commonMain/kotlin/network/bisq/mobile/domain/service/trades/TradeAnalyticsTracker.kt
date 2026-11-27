package network.bisq.mobile.domain.service.trades

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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.domain.repository.TradeStallClockRepository
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
 *  - **state-derived events** via [observeTrades]: `StartedAsMaker` when a maker-side trade first
 *    appears, `PhaseEntered` as a trade enters each phase, `Completed` (with the user's role) when
 *    it reaches `BTC_CONFIRMED`, `Errored` (with the role) when it surfaces a protocol/peer error
 *    (expected validation rejections stay volume-only; unexpected ones also capture an exception),
 *    `OutOfSyncFirstDetected` when it sits in INIT past the [TradeOutOfSyncDetector] threshold —
 *    the field measurement of the stuck-FSM desync the out-of-sync recovery pane exists for — and
 *    `OutOfSyncRecovered` when such a trade leaves INIT. Open trades replay their state on every
 *    launch and every facade activation, so each of these is reported once per trade: the
 *    reported markers live in this instance (re-activations) and in the persisted stall clock
 *    (restarts), and are evicted with the trade.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TradeAnalyticsTracker(
    private val analyticsService: AnalyticsService,
    private val stallTimeoutMs: Long = DEFAULT_STALL_TIMEOUT_MS,
    private val clock: () -> Long = { DateUtils.now() },
    // Optional so tests exercising unrelated signals need no repository; production wiring
    // (BaseTradesServiceFacade) always passes one.
    private val stallClockRepository: TradeStallClockRepository? = null,
) : Logging {
    companion object {
        // Generous for Tor round-trips. Only the user's OWN local state transition is timed (fast), so
        // this never trips on a legitimate counterparty wait, which can span hours/days.
        const val DEFAULT_STALL_TIMEOUT_MS = 45_000L

        // The out-of-sync signal is time-based (a stuck trade never transitions), so it needs a
        // recheck ticker. One minute against a 10-minute detection threshold keeps detection
        // near-immediate without meaningful wakeup cost.
        const val OUT_OF_SYNC_RECHECK_MS = 60_000L

        // Reported-marker keys, persisted in TradeStallClockEntry.reported. Changing one makes
        // trades already reported under the old key report again once.
        private const val MARKER_STARTED_AS_MAKER = "started_as_maker"
        private const val MARKER_COMPLETED = "completed"
        private const val MARKER_ERRORED = "errored"
        private const val MARKER_OUT_OF_SYNC = "out_of_sync"
        private const val MARKER_OUT_OF_SYNC_RECOVERED = "out_of_sync_recovered"

        private fun phaseMarker(phase: AnalyticsEvent.Trade.Phase) = "phase:${phase.slug}"

        private fun roleOf(item: TradeItemPresentationModel) = if (item.bisqEasyTradeModel.isMaker) AnalyticsEvent.Trade.Role.MAKER else AnalyticsEvent.Trade.Role.TAKER
    }

    // Session cache of the persisted stall clock ([TradeStallClockRepository]): the trade DTO
    // carries no per-state timestamps, so the app can only time transitions it witnesses itself —
    // but witnessed transitions are persisted, so the clock survives restarts. A genuinely stuck
    // trade (state frozen across sessions) now ages into the 1h+/1d+ buckets instead of resetting
    // to UNKNOWN on every restart (which the first monthly report showed dominating cancel
    // events). A state that CHANGED while the app was closed is re-stamped at reload — the moment
    // the change became visible. Entries are evicted as trades leave the open list.
    private val lastKnownStateByTradeId = mutableMapOf<String, BisqEasyTradeStateEnum>()
    private val lastTransitionAtMsByTradeId = mutableMapOf<String, Long>()

    // Session cache of the persisted reported markers, per trade. Held by the instance rather than
    // by one [observeTrades] call, so a facade re-activation does not report replayed state again.
    // Four collectors read and write it, hence the mutex.
    private val reportedByTradeId = mutableMapOf<String, MutableSet<String>>()
    private val reportedMutex = Mutex()

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
     *
     * Returns whether anything new was witnessed (first sighting or transition) — i.e. whether the
     * persisted clock is stale and worth rewriting. Replays return false, so the datastore is not
     * rewritten on every open-list change.
     */
    private fun recordTransition(
        id: String,
        state: BisqEasyTradeStateEnum,
    ): Boolean {
        val previous = lastKnownStateByTradeId.put(id, state)
        if (previous != null && previous != state) {
            lastTransitionAtMsByTradeId[id] = clock()
        }
        return previous != state
    }

    /**
     * Restores persisted stall clocks into the session cache before observing. Only ids not already
     * tracked this session are seeded, so a deactivate/reactivate cycle never overwrites fresher
     * in-memory data with stale persisted state. A persisted state name the current enum no longer
     * knows (app update) is skipped — its age is unknowable, which [stallBucketFor] already reports
     * as UNKNOWN.
     */
    private suspend fun seedFromPersistedClocks() {
        val persisted = stallClockRepository?.fetch()?.map ?: return
        reportedMutex.withLock {
            persisted.forEach { (id, entry) ->
                if (entry.reported.isNotEmpty()) reportedByTradeId.getOrPut(id) { mutableSetOf() } += entry.reported
            }
        }
        persisted.forEach { (id, entry) ->
            if (id !in lastKnownStateByTradeId) {
                val state =
                    BisqEasyTradeStateEnum.entries.find { it.name == entry.stateName }
                        ?: return@forEach
                lastKnownStateByTradeId[id] = state
                entry.transitionAtMs?.let { lastTransitionAtMsByTradeId[id] = it }
            }
        }
    }

    private suspend fun isReported(
        id: String,
        marker: String,
    ): Boolean = reportedMutex.withLock { reportedByTradeId[id]?.contains(marker) == true }

    /**
     * Runs [report] only the first time [marker] is seen for trade [id], then persists the marker.
     * Reporting before persisting means a cancellation in between can at worst repeat the event
     * after a restart, never lose it.
     */
    private suspend fun reportOnce(
        id: String,
        stateName: String,
        marker: String,
        report: () -> Unit,
    ) {
        val first = reportedMutex.withLock { reportedByTradeId.getOrPut(id) { mutableSetOf() }.add(marker) }
        if (!first) return
        report()
        persistSafely { stallClockRepository?.markReported(id, stateName, marker) }
    }

    /**
     * Stall-clock persistence is best-effort: the [observeTrades] collector that writes it also
     * emits `Completed`, so a datastore failure must degrade to in-memory clocks rather than end
     * completion tracking for the session. Cancellation still propagates.
     */
    private suspend fun persistSafely(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            log.e(e) { "Stall-clock persistence failed — continuing with in-memory clocks only" }
        }
    }

    /**
     * Runs a user-driven confirm action and records its outcome. On failure emits `FAILED` and captures
     * the exception; on success watches [tradeState] for the user's own transition away from its current
     * value, emitting `CONFIRMED` if it advances within [stallTimeoutMs] or `STALLED` otherwise. The
     * action's [Result] is returned unchanged — the stall watch runs in the background.
     *
     * A failure is gated with [ensureActive]: if this coroutine is already cancelled (navigate-away),
     * the exception is rethrown and not reported — including non-[CancellationException] teardown
     * failures. If the caller is still active (request timeout or a genuine error), it is reported
     * as `FAILED`.
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
                currentCoroutineContext().ensureActive()
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
     * Observes all open trades for phases, completion, protocol/peer errors and out-of-sync, each
     * reported once per trade (see the class doc). Launch on facade activation. [tradeId] extracts
     * a stable id from a trade item.
     */
    fun observeTrades(
        scope: CoroutineScope,
        openTradeItems: StateFlow<List<TradeItemPresentationModel>>,
        tradeId: (TradeItemPresentationModel) -> String,
    ) {
        scope.launch {
            // The persisted markers must be in place before any collector checks them, or replayed
            // state would be reported again on every launch.
            persistSafely { seedFromPersistedClocks() }

            // Phase entered (state-based): once per (trade, phase) as a trade enters each phase,
            // whether or not the user is viewing it — the funnel denominator to compare against
            // the view-based PhaseOpened.
            launch {
                openTradeItems
                    .flatMapLatest { trades ->
                        merge(
                            *trades
                                .map { item ->
                                    item.bisqEasyTradeModel.tradeState.map { state ->
                                        Triple(tradeId(item), state, state.toTradePhase(item.bisqEasyTradeModel.isSeller))
                                    }
                                }.toTypedArray(),
                        )
                    }.collect { (id, state, phase) ->
                        if (phase != null) {
                            reportOnce(id, state.name, phaseMarker(phase)) {
                                analyticsService.track(AnalyticsEvent.Trade.PhaseEntered(phase))
                            }
                        }
                    }
            }

            // Maker start and completion: BTC_CONFIRMED. flatMapLatest re-subscribes to the current
            // trades' state flows whenever the open-trade list changes. The same stream feeds the
            // stall clock — every state change is a witnessed transition, cached in memory and
            // persisted so it survives restarts — and closes out-of-sync reports when a stuck trade
            // leaves INIT.
            launch {
                // Eviction is gated on having seen a non-empty list: the first emission at startup
                // is an empty list while trades load, and evicting on it would wipe the just-seeded
                // persisted clocks and markers. A session that never loads a trade keeps stale
                // entries — bounded (only ever past open trades) and evicted on the next session
                // that does.
                var sawOpenTrades = false
                openTradeItems
                    .flatMapLatest { trades ->
                        // Evict entries for trades no longer open, so the maps and the persisted
                        // clocks stay bounded.
                        if (trades.isNotEmpty()) sawOpenTrades = true
                        if (sawOpenTrades) {
                            val openIds = trades.mapTo(mutableSetOf()) { tradeId(it) }
                            lastKnownStateByTradeId.keys.retainAll(openIds)
                            lastTransitionAtMsByTradeId.keys.retainAll(openIds)
                            reportedMutex.withLock { reportedByTradeId.keys.retainAll(openIds) }
                            persistSafely { stallClockRepository?.retainAll(openIds) }
                        }
                        merge(
                            *trades
                                .map { item ->
                                    val role = roleOf(item)
                                    item.bisqEasyTradeModel.tradeState.map { state -> Triple(tradeId(item), state, role) }
                                }.toTypedArray(),
                        )
                    }.collect { (id, state, role) ->
                        if (recordTransition(id, state)) {
                            persistSafely { stallClockRepository?.record(id, state.name, lastTransitionAtMsByTradeId[id]) }
                        }
                        if (role == AnalyticsEvent.Trade.Role.MAKER) {
                            reportOnce(id, state.name, MARKER_STARTED_AS_MAKER) { analyticsService.track(AnalyticsEvent.Trade.StartedAsMaker) }
                        }
                        if (state == BisqEasyTradeStateEnum.BTC_CONFIRMED) {
                            reportOnce(id, state.name, MARKER_COMPLETED) { analyticsService.track(AnalyticsEvent.Trade.Completed(role)) }
                        }
                        if (state != BisqEasyTradeStateEnum.INIT && isReported(id, MARKER_OUT_OF_SYNC)) {
                            reportOnce(id, state.name, MARKER_OUT_OF_SYNC_RECOVERED) {
                                analyticsService.track(AnalyticsEvent.Trade.OutOfSyncRecovered)
                            }
                        }
                    }
            }

            // Out-of-sync: a trade parked in INIT past the detector threshold is desynced from the
            // peer (the stuck-FSM bug the recovery pane exists for — see TradeOutOfSyncDetector).
            // This signal is time-based, not state-based — a stuck trade never transitions — so it
            // rechecks on a ticker instead of observing state flows.
            launch {
                openTradeItems
                    .flatMapLatest { trades ->
                        TimeUtils.tickerFlow(OUT_OF_SYNC_RECHECK_MS).map { trades }
                    }.collect { trades ->
                        trades
                            .filter { TradeOutOfSyncDetector.isOutOfSync(it.bisqEasyTradeModel, clock()) }
                            .forEach { item ->
                                reportOnce(tradeId(item), item.bisqEasyTradeModel.tradeState.value.name, MARKER_OUT_OF_SYNC) {
                                    log.w { "Trade detected out of sync — stuck in INIT past the detection threshold" }
                                    analyticsService.track(AnalyticsEvent.Trade.OutOfSyncFirstDetected)
                                }
                            }
                    }
            }

            // Errors: the trade's own error OR the peer's error appearing. These are independent
            // observables — a peer-side failure sets peersErrorMessage without ever touching
            // errorMessage — so we observe both per trade and let the per-trade marker collapse
            // them to a single Errored (whichever surfaces first wins).
            launch {
                openTradeItems
                    .flatMapLatest { trades ->
                        merge(
                            *trades
                                .flatMap { item ->
                                    val id = tradeId(item)
                                    val role = roleOf(item)
                                    val model = item.bisqEasyTradeModel
                                    listOf(
                                        model.errorMessage.map { ErrorSighting(id, role, model.tradeState.value.name, it, model.errorStackTrace.value) },
                                        model.peersErrorMessage.map {
                                            ErrorSighting(id, role, model.tradeState.value.name, it, model.peersErrorStackTrace.value)
                                        },
                                    )
                                }.toTypedArray(),
                        )
                    }.collect { sighting ->
                        val message = sighting.message ?: return@collect
                        reportOnce(sighting.tradeId, sighting.stateName, MARKER_ERRORED) {
                            analyticsService.track(AnalyticsEvent.Trade.Errored(sighting.role))
                            if (!ExpectedTradeProtocolRejection.isExpected(message)) {
                                analyticsService.captureException(TradeProtocolException(message, sighting.stackTrace))
                            }
                        }
                    }
            }
        }
    }

    private data class ErrorSighting(
        val tradeId: String,
        val role: AnalyticsEvent.Trade.Role,
        val stateName: String,
        val message: String?,
        val stackTrace: String?,
    )
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
