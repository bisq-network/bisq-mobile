package network.bisq.mobile.presentation.common.test_utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.model.BatteryOptimizationState
import network.bisq.mobile.domain.data.model.MarketFilter
import network.bisq.mobile.domain.data.model.MarketSortBy
import network.bisq.mobile.domain.data.model.PermissionState
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.repository.SettingsRepository

internal class FakeSettingsRepository(
    initial: Settings = Settings(),
) : SettingsRepository {
    private val mutableData = MutableStateFlow(initial)
    override val data: StateFlow<Settings> = mutableData

    override suspend fun setFirstLaunch(value: Boolean) {}

    override suspend fun setShowChatRulesWarnBox(value: Boolean) {}

    override suspend fun setSelectedMarketCode(value: String) {}

    override suspend fun setNotificationPermissionState(value: PermissionState) {}

    override suspend fun setBatteryOptimizationPermissionState(value: BatteryOptimizationState) {}

    override suspend fun update(transform: suspend (t: Settings) -> Settings) {
        mutableData.value = transform(mutableData.value)
    }

    override suspend fun clear() {
        mutableData.value = Settings()
    }

    override suspend fun setMarketSortBy(value: MarketSortBy) {
        mutableData.value = mutableData.value.copy(marketSortBy = value)
    }

    override suspend fun setMarketFilter(value: MarketFilter) {
        mutableData.value = mutableData.value.copy(marketFilter = value)
    }
}
