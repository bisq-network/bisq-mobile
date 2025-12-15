package network.bisq.mobile.domain.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import network.bisq.mobile.domain.data.model.BatteryOptimizationState
import network.bisq.mobile.domain.data.model.PermissionState
import network.bisq.mobile.domain.data.model.Settings

interface SettingsRepository {

    val data: Flow<Settings>

    suspend fun fetch() = data.first()

    suspend fun setFirstLaunch(value: Boolean)

    suspend fun setShowChatRulesWarnBox(value: Boolean)

    suspend fun setSelectedMarketCode(value: String)

    suspend fun setNotificationPermissionState(value: PermissionState)

    suspend fun setBatteryOptimizationPermissionState(value: BatteryOptimizationState)

    suspend fun update(transform: suspend (t: Settings) -> Settings)

    suspend fun clear()
}