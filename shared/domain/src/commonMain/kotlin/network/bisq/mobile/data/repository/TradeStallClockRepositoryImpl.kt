package network.bisq.mobile.data.repository

import androidx.datastore.core.DataStore
import network.bisq.mobile.data.model.TradeStallClockEntry
import network.bisq.mobile.data.model.TradeStallClockMap
import network.bisq.mobile.domain.repository.TradeStallClockRepository

class TradeStallClockRepositoryImpl(
    tradeStallClockMapStore: DataStore<TradeStallClockMap>,
) : DataStoreRepository<TradeStallClockMap>(tradeStallClockMapStore),
    TradeStallClockRepository {
    override fun createDefault() = TradeStallClockMap(emptyMap())

    override suspend fun record(
        tradeId: String,
        stateName: String,
        transitionAtMs: Long?,
    ) {
        require(tradeId.isNotBlank()) { "tradeId cannot be blank" }

        set {
            val reported = it.map[tradeId]?.reported.orEmpty()
            it.copy(it.map + (tradeId to TradeStallClockEntry(stateName, transitionAtMs, reported)))
        }
    }

    override suspend fun markReported(
        tradeId: String,
        stateName: String,
        marker: String,
    ) {
        require(tradeId.isNotBlank()) { "tradeId cannot be blank" }

        set {
            val entry = it.map[tradeId] ?: TradeStallClockEntry(stateName)
            it.copy(it.map + (tradeId to entry.copy(reported = entry.reported + marker)))
        }
    }

    override suspend fun retainAll(tradeIds: Set<String>) {
        set { it.copy(it.map.filterKeys(tradeIds::contains)) }
    }
}
