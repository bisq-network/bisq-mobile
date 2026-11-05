package network.bisq.mobile.data.repository

import androidx.datastore.core.DataStore
import network.bisq.mobile.data.model.TradeCancelReason
import network.bisq.mobile.data.model.TradeCancelReasonMap
import network.bisq.mobile.domain.repository.TradeCancelReasonRepository

class TradeCancelReasonRepositoryImpl(
    tradeCancelReasonMapStore: DataStore<TradeCancelReasonMap>,
) : DataStoreRepository<TradeCancelReasonMap>(tradeCancelReasonMapStore),
    TradeCancelReasonRepository {
    override fun createDefault() = TradeCancelReasonMap()

    override suspend fun setReason(
        tradeId: String,
        reason: TradeCancelReason,
    ) {
        require(tradeId.isNotBlank()) { "tradeId cannot be blank" }
        set { it.copy(it.map + (tradeId to reason)) }
    }

    override suspend fun clearId(tradeId: String) {
        require(tradeId.isNotBlank()) { "tradeId cannot be blank" }
        set { it.copy(it.map - tradeId) }
    }
}
