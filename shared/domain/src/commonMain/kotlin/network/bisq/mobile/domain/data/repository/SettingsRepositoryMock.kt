package network.bisq.mobile.domain.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.utils.Logging

class SettingsRepositoryMock : SettingsRepository, Logging {

    private val _data = MutableStateFlow(Settings())
    override val data: StateFlow<Settings> get() = _data.asStateFlow()

    override suspend fun setBisqApiUrl(value: String) {
        _data.update {
            it.copy(bisqApiUrl = value)
        }
    }

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

    override suspend fun clear() {
        _data.update {
            Settings()
        }
    }
}