package network.bisq.mobile.domain.repository

import kotlinx.coroutines.flow.Flow
import network.bisq.mobile.data.model.TradeCancelReason
import network.bisq.mobile.data.model.TradeCancelReasonMap

interface TradeCancelReasonRepository {
    val data: Flow<TradeCancelReasonMap>

    suspend fun setReason(
        tradeId: String,
        reason: TradeCancelReason,
    )

    suspend fun clearId(tradeId: String)
}
