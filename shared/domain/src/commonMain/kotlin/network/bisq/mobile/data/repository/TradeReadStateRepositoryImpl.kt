package network.bisq.mobile.data.repository

import androidx.datastore.core.DataStore
import network.bisq.mobile.data.model.TradeReadStateMap
import network.bisq.mobile.domain.repository.TradeReadStateRepository

class TradeReadStateRepositoryImpl(
    tradeReadStateMapStore: DataStore<TradeReadStateMap>,
) : DataStoreRepository<TradeReadStateMap>(tradeReadStateMapStore),
    TradeReadStateRepository {
    override fun createDefault() = TradeReadStateMap(emptyMap())

    override suspend fun setCount(
        tradeId: String,
        count: Int,
    ) {
        require(tradeId.isNotBlank()) { "tradeId cannot be blank" }
        require(count >= 0) { "count must be >= 0" }

        set { it.copy(it.map + (tradeId to count)) }
    }

    override suspend fun clearId(tradeId: String) {
        require(tradeId.isNotBlank()) { "tradeId cannot be blank" }
        set { it.copy(it.map - tradeId) }
    }
}
