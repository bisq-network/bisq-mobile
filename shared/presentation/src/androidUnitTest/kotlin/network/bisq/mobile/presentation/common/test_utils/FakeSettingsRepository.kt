package network.bisq.mobile.presentation.common.test_utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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

    override suspend fun setFirstLaunch(value: Boolean) {
        mutableData.update { it.copy(firstLaunch = value) }
    }

    override suspend fun setShowChatRulesWarnBox(value: Boolean) {
        mutableData.update { it.copy(showChatRulesWarnBox = value) }
    }

    override suspend fun setSelectedMarketCode(value: String) {
        mutableData.update { it.copy(selectedMarketCode = value) }
    }

    override suspend fun setNotificationPermissionState(value: PermissionState) {
        mutableData.update { it.copy(notificationPermissionState = value) }
    }

    override suspend fun setBatteryOptimizationPermissionState(value: BatteryOptimizationState) {
        mutableData.update { it.copy(batteryOptimizationState = value) }
    }

    override suspend fun update(transform: suspend (t: Settings) -> Settings) {
        mutableData.value = transform(mutableData.value)
    }

    override suspend fun clear() {
        mutableData.update { Settings() }
    }

    override suspend fun setMarketSortBy(value: MarketSortBy) {
        mutableData.update { it.copy(marketSortBy = value) }
    }

    override suspend fun setMarketFilter(value: MarketFilter) {
        mutableData.update { it.copy(marketFilter = value) }
    }
}
