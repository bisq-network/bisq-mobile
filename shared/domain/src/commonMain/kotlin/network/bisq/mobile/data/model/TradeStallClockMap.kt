package network.bisq.mobile.data.model

import kotlinx.serialization.Serializable

/**
 * Persisted stall clocks for open trades: per trade, the last state the app witnessed and when the
 * last state transition was witnessed. Local-only — read back into `TradeAnalyticsTracker` so
 * "time since the trade last visibly moved" survives app restarts; only the bounded
 * `AnalyticsEvent.Trade.StallBucket` ever reaches analytics.
 */
@Serializable
data class TradeStallClockMap(
    // tradeId to last witnessed state + transition time
    val map: Map<String, TradeStallClockEntry> = emptyMap(),
)

@Serializable
data class TradeStallClockEntry(
    val stateName: String,
    // null until a transition is witnessed — a first sighting's age is unknowable
    val transitionAtMs: Long? = null,
)
