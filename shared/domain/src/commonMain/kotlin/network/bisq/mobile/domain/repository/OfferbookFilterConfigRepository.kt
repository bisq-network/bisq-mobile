package network.bisq.mobile.domain.repository

import kotlinx.coroutines.flow.Flow
import network.bisq.mobile.data.model.offerbook.OfferbookFilterConfig
import network.bisq.mobile.data.model.offerbook.OfferbookFilterConfigs

interface OfferbookFilterConfigRepository {
    val data: Flow<OfferbookFilterConfigs>

    suspend fun getConfig(marketKey: String): OfferbookFilterConfig

    suspend fun setConfig(
        marketKey: String,
        config: OfferbookFilterConfig,
    )
}
