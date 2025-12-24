package network.bisq.mobile.client.common.domain.sensitive_settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface SensitiveSettingsRepository {
    val data: Flow<SensitiveSettings>

    suspend fun fetch() = data.first()

    suspend fun update(transform: suspend (t: SensitiveSettings) -> SensitiveSettings)

    suspend fun clear()
}
