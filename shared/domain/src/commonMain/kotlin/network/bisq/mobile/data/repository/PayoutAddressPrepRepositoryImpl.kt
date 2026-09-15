package network.bisq.mobile.data.repository

import androidx.datastore.core.DataStore
import network.bisq.mobile.data.model.PayoutAddressPrep
import network.bisq.mobile.domain.repository.PayoutAddressPrepRepository

class PayoutAddressPrepRepositoryImpl(
    payoutAddressPrepStore: DataStore<PayoutAddressPrep>,
) : DataStoreRepository<PayoutAddressPrep>(payoutAddressPrepStore),
    PayoutAddressPrepRepository {
    override fun createDefault() = PayoutAddressPrep()

    override suspend fun setPrefill(
        tradeId: String,
        address: String,
    ) {
        require(tradeId.isNotBlank()) { "tradeId cannot be blank" }
        require(address.isNotBlank()) { "address cannot be blank" }

        set { it.copy(prefillByTradeId = it.prefillByTradeId + (tradeId to address)) }
    }

    override suspend fun clearPrefill(tradeId: String) {
        require(tradeId.isNotBlank()) { "tradeId cannot be blank" }
        set { it.copy(prefillByTradeId = it.prefillByTradeId - tradeId) }
    }

    override suspend fun markTradeCompleted(profileId: String) {
        require(profileId.isNotBlank()) { "profileId cannot be blank" }
        set { it.copy(profilesWithCompletedTrade = it.profilesWithCompletedTrade + profileId) }
    }
}
