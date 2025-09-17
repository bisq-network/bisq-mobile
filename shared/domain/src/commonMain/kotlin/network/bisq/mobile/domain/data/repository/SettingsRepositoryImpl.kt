package network.bisq.mobile.domain.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import network.bisq.mobile.domain.data.model.NotificationPermissionState
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.utils.Logging

class SettingsRepositoryImpl(
    private val settingsStore: DataStore<Settings>,
) : SettingsRepository, Logging {

    override val data: Flow<Settings>
        get() =
            settingsStore.data.catch { exception ->
                if (exception is IOException) {
                    log.e("Error reading Settings datastore", exception)
                    emit(Settings())
                } else {
                    throw exception
                }
            }

    override suspend fun setBisqApiUrl(value: String) {
        settingsStore.updateData {
            it.copy(bisqApiUrl = value)
        }
    }

    override suspend fun setFirstLaunch(value: Boolean) {
        settingsStore.updateData {
            it.copy(firstLaunch = value)
        }
    }

    override suspend fun setShowChatRulesWarnBox(value: Boolean) {
        settingsStore.updateData {
            it.copy(showChatRulesWarnBox = value)
        }
    }

    override suspend fun setSelectedMarketCode(value: String) {
        settingsStore.updateData {
            it.copy(selectedMarketCode = value)
        }
    }

    override suspend fun setNotificationPermissionState(value: NotificationPermissionState) {
        settingsStore.updateData {
            it.copy(notificationPermissionState = value)
        }
    }

    override suspend fun clear() {
        settingsStore.updateData {
            Settings()
        }
    }
}