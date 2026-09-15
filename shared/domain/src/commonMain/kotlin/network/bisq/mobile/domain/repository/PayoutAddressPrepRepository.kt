package network.bisq.mobile.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import network.bisq.mobile.data.model.PayoutAddressPrep

interface PayoutAddressPrepRepository {
    val data: Flow<PayoutAddressPrep>

    suspend fun fetch() = data.first()

    suspend fun setPrefill(
        tradeId: String,
        address: String,
    )

    suspend fun clearPrefill(tradeId: String)

    suspend fun markTradeCompleted(profileId: String)
}
