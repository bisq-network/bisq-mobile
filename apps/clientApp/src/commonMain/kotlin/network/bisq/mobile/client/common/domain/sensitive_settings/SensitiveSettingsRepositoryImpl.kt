package network.bisq.mobile.client.common.domain.sensitive_settings

import androidx.datastore.core.DataStore
import network.bisq.mobile.data.repository.DataStoreRepository

class SensitiveSettingsRepositoryImpl(
    sensitiveSettingsStore: DataStore<SensitiveSettings>,
) : DataStoreRepository<SensitiveSettings>(sensitiveSettingsStore),
    SensitiveSettingsRepository {
    override fun createDefault() = SensitiveSettings()

    override suspend fun update(transform: suspend (t: SensitiveSettings) -> SensitiveSettings) = set(transform)
}
