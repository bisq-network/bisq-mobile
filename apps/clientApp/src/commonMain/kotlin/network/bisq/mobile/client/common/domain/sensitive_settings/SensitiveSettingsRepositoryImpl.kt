package network.bisq.mobile.client.common.domain.sensitive_settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import network.bisq.mobile.domain.utils.Logging

class SensitiveSettingsRepositoryImpl(
    private val sensitiveSettingsStore: DataStore<SensitiveSettings>,
) : SensitiveSettingsRepository,
    Logging {
    override val data: Flow<SensitiveSettings>
        get() =
            sensitiveSettingsStore.data.catch { exception ->
                if (exception is IOException) {
                    log.e("Error reading SensitiveSettings datastore", exception)
                    emit(SensitiveSettings())
                } else {
                    throw exception
                }
            }

    override suspend fun update(transform: suspend (t: SensitiveSettings) -> SensitiveSettings) {
        sensitiveSettingsStore.updateData(transform)
    }

    override suspend fun clear() {
        sensitiveSettingsStore.updateData {
            SensitiveSettings()
        }
    }
}
