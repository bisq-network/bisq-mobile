package network.bisq.mobile.presentation.common.test_utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import network.bisq.mobile.data.model.PayoutAddressPrep
import network.bisq.mobile.domain.repository.PayoutAddressPrepRepository

internal class FakePayoutAddressPrepRepository(
    initial: PayoutAddressPrep = PayoutAddressPrep(),
) : PayoutAddressPrepRepository {
    val mutableData = MutableStateFlow(initial)
    override val data: Flow<PayoutAddressPrep> = mutableData

    override suspend fun setPrefill(
        tradeId: String,
        address: String,
    ) {
        mutableData.update { it.copy(prefillByTradeId = it.prefillByTradeId + (tradeId to address)) }
    }

    override suspend fun clearPrefill(tradeId: String) {
        mutableData.update { it.copy(prefillByTradeId = it.prefillByTradeId - tradeId) }
    }

    override suspend fun markTradeCompleted(profileId: String) {
        mutableData.update { it.copy(profilesWithCompletedTrade = it.profilesWithCompletedTrade + profileId) }
    }
}
