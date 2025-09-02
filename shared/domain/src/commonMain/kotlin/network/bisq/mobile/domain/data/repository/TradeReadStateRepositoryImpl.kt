package network.bisq.mobile.domain.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import network.bisq.mobile.domain.data.model.TradeReadStateMap
import network.bisq.mobile.domain.utils.Logging

class TradeReadStateRepositoryImpl(
    private val tradeReadStateMapStore: DataStore<TradeReadStateMap>,
) : TradeReadStateRepository, Logging {

    override val data: Flow<TradeReadStateMap>
        get() =
            tradeReadStateMapStore.data.catch { exception ->
                if (exception is IOException) {
                    log.e("Error reading TradeReadStateMap datastore", exception)
                    emit(TradeReadStateMap(emptyMap()))
                } else {
                    throw exception
                }
            }

    override suspend fun setCount(tradeId: String, count: Int) {
        require(tradeId.isNotBlank()) { "tradeId cannot be blank" }
        require(count >= 0) { "count must be >= 0" }

        tradeReadStateMapStore.updateData { it.copy(it.map + (tradeId to count))  }
    }

    override suspend fun clearId(tradeId: String) {
        require(tradeId.isNotBlank()) { "tradeId cannot be blank" }
        tradeReadStateMapStore.updateData {
            it.copy(it.map - tradeId)
        }
    }
}