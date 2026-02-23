package network.bisq.mobile.presentation.common.test_utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.domain.data.model.TradeReadStateMap
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository

internal class FakeTradeReadStateRepository(
    initial: TradeReadStateMap = TradeReadStateMap(),
) : TradeReadStateRepository {
    private val mutableData = MutableStateFlow(initial)
    override val data: Flow<TradeReadStateMap> = mutableData

    override suspend fun setCount(
        tradeId: String,
        count: Int,
    ) {
        val current = mutableData.value
        val updatedMap = current.map + (tradeId to count)
        mutableData.value = current.copy(map = updatedMap)
    }

    override suspend fun clearId(tradeId: String) {
        val current = mutableData.value
        val updatedMap = current.map - tradeId
        mutableData.value = current.copy(map = updatedMap)
    }
}
