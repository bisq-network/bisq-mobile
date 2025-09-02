package network.bisq.mobile.domain.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import network.bisq.mobile.domain.data.model.TradeReadStateMap

interface TradeReadStateRepository {

    val data: Flow<TradeReadStateMap>

    suspend fun fetch() = data.first()

    suspend fun setCount(tradeId: String, count: Int)

    suspend fun clearId(tradeId: String)
}