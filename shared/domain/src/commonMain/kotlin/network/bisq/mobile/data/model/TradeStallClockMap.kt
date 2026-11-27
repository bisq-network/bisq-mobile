package network.bisq.mobile.data.model

import kotlinx.serialization.Serializable

/**
 * Persisted stall clocks for open trades: per trade, the last state the app witnessed, when the
 * last state transition was witnessed, and which once-per-trade analytics events were already
 * reported. Local-only — read back into `TradeAnalyticsTracker` so "time since the trade last
 * visibly moved" and "already reported" survive app restarts; only the bounded
 * `AnalyticsEvent.Trade` slugs ever reach analytics.
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
    // Markers of the once-per-trade events already reported for this trade (see
    // TradeAnalyticsTracker). Empty for entries written before markers existed.
    val reported: Set<String> = emptySet(),
)
