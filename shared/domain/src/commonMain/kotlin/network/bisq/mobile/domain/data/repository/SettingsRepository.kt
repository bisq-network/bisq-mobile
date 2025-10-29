package network.bisq.mobile.domain.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import network.bisq.mobile.client.httpclient.BisqProxyOption
import network.bisq.mobile.domain.data.model.NotificationPermissionState
import network.bisq.mobile.domain.data.model.Settings

interface SettingsRepository {

    val data: Flow<Settings>

    suspend fun fetch() = data.first()

    suspend fun setBisqApiUrl(value: String)

    suspend fun setFirstLaunch(value: Boolean)

    suspend fun setShowChatRulesWarnBox(value: Boolean)

    suspend fun setSelectedMarketCode(value: String)

    suspend fun setNotificationPermissionState(value: NotificationPermissionState)

    suspend fun setExternalProxyUrl(value: String)

    suspend fun setSelectedProxyOption(value: BisqProxyOption)

    suspend fun setBisqApiPassword(value: String)

    suspend fun update(transform: suspend (t: Settings) -> Settings)

    suspend fun clear()
}