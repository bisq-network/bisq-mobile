package network.bisq.mobile.presentation.common.test_utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import network.bisq.mobile.domain.data.model.TradeReadStateMap
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository

internal class FakeTradeReadStateRepository(
    initial: TradeReadStateMap = TradeReadStateMap(),
) : TradeReadStateRepository {
    override val data: Flow<TradeReadStateMap> = flowOf(initial)

    override suspend fun setCount(
        tradeId: String,
        count: Int,
    ) {
    }

    override suspend fun clearId(tradeId: String) {}
}
