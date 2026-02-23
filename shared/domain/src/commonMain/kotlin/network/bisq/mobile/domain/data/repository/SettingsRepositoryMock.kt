package network.bisq.mobile.domain.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import network.bisq.mobile.domain.data.model.BatteryOptimizationState
import network.bisq.mobile.domain.data.model.MarketFilter
import network.bisq.mobile.domain.data.model.MarketSortBy
import network.bisq.mobile.domain.data.model.PermissionState
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.utils.Logging

class SettingsRepositoryMock :
    SettingsRepository,
    Logging {
    private val _data = MutableStateFlow(Settings())
    override val data: StateFlow<Settings> get() = _data.asStateFlow()

    override suspend fun setFirstLaunch(value: Boolean) {
        _data.update {
            it.copy(firstLaunch = value)
        }
    }

    override suspend fun setShowChatRulesWarnBox(value: Boolean) {
        _data.update {
            it.copy(showChatRulesWarnBox = value)
        }
    }

    override suspend fun setSelectedMarketCode(value: String) {
        _data.update {
            it.copy(selectedMarketCode = value)
        }
    }

    override suspend fun setNotificationPermissionState(value: PermissionState) {
        _data.update {
            it.copy(notificationPermissionState = value)
        }
    }

    override suspend fun setBatteryOptimizationPermissionState(value: BatteryOptimizationState) {
        _data.update {
            it.copy(batteryOptimizationState = value)
        }
    }

    override suspend fun update(transform: suspend (Settings) -> Settings) {
        _data.value = transform(_data.value)
    }

    override suspend fun clear() {
        _data.update {
            Settings()
        }
    }

    override suspend fun setMarketSortBy(value: MarketSortBy) {
        _data.update {
            it.copy(marketSortBy = value)
        }
    }

    override suspend fun setMarketFilter(value: MarketFilter) {
        _data.update {
            it.copy(marketFilter = value)
        }
    }
}
