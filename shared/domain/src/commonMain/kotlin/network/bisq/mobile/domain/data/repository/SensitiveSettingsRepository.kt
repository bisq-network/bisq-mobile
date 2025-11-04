package network.bisq.mobile.domain.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import network.bisq.mobile.domain.data.model.SensitiveSettings

interface SensitiveSettingsRepository {

    val data: Flow<SensitiveSettings>

    suspend fun fetch() = data.first()

    suspend fun update(transform: suspend (t: SensitiveSettings) -> SensitiveSettings)

    suspend fun clear()
}