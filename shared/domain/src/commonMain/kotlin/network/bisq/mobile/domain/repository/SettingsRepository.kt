package network.bisq.mobile.domain.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import network.bisq.mobile.data.model.BatteryOptimizationState
import network.bisq.mobile.data.model.PermissionState
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.data.model.market.MarketFilter
import network.bisq.mobile.data.model.market.MarketSortBy

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

    suspend fun setMarketSortBy(value: MarketSortBy)

    suspend fun setMarketFilter(value: MarketFilter)

    suspend fun setDontShowAgainHyperlinksOpenInBrowser(value: Boolean)

    suspend fun setPermitOpeningBrowser(value: Boolean)

    suspend fun setAnalyticsEnabled(value: Boolean)

    suspend fun setAnalyticsPromptSeen(value: Boolean)

    /**
     * Returns a hot [StateFlow] of [Settings.analyticsEnabled] sharing in the
     * given [scope]. The DI module passes its long-lived buffer scope here so
     * the analytics SDK's synchronous `runtimeOptInProvider` can read
     * `.value` without suspending. Default is `false` (matches privacy
     * contract: never emit until proven opted-in).
     */
    fun analyticsEnabledIn(scope: CoroutineScope): StateFlow<Boolean> =
        data
            .map { it.analyticsEnabled }
            .stateIn(scope, SharingStarted.Eagerly, false)
}
