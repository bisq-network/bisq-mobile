package network.bisq.mobile.presentation.common.test_utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
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
        mutableData.update { it.copy(map = it.map + (tradeId to count)) }
    }

    override suspend fun clearId(tradeId: String) {
        mutableData.update { it.copy(map = it.map - tradeId) }
    }
}
