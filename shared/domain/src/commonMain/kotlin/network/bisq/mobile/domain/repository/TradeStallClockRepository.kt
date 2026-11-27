package network.bisq.mobile.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import network.bisq.mobile.data.model.TradeStallClockMap

interface TradeStallClockRepository {
    val data: Flow<TradeStallClockMap>

    suspend fun fetch() = data.first()

    suspend fun record(
        tradeId: String,
        stateName: String,
        transitionAtMs: Long?,
    )

    /**
     * Adds [marker] to the trade's reported markers, keeping its clock. Creates an unstamped entry
     * in [stateName] when the trade has none yet.
     */
    suspend fun markReported(
        tradeId: String,
        stateName: String,
        marker: String,
    )

    suspend fun retainAll(tradeIds: Set<String>)
}
