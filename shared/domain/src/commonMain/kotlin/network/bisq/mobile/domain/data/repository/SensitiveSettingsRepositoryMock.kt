package network.bisq.mobile.domain.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import network.bisq.mobile.domain.data.model.SensitiveSettings
import network.bisq.mobile.domain.utils.Logging

class SensitiveSettingsRepositoryMock : SensitiveSettingsRepository, Logging {

    private val _data = MutableStateFlow(SensitiveSettings())
    override val data: StateFlow<SensitiveSettings> get() = _data.asStateFlow()

    override suspend fun update(transform: suspend (SensitiveSettings) -> SensitiveSettings) {
        _data.value = transform(_data.value)
    }


    override suspend fun clear() {
        _data.update {
            SensitiveSettings()
        }
    }
}