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

    suspend fun retainAll(tradeIds: Set<String>)
}
