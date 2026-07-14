package network.bisq.mobile.data.repository

import androidx.datastore.core.DataStore
import network.bisq.mobile.data.model.BatteryOptimizationState
import network.bisq.mobile.data.model.PermissionState
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.data.model.market.MarketFilter
import network.bisq.mobile.data.model.market.MarketSortBy
import network.bisq.mobile.domain.repository.SettingsRepository

open class SettingsRepositoryImpl(
    settingsStore: DataStore<Settings>,
) : DataStoreRepository<Settings>(settingsStore),
    SettingsRepository {
    override fun createDefault() = Settings()

    override suspend fun setFirstLaunch(value: Boolean) = set { it.copy(firstLaunch = value) }

    override suspend fun setShowChatRulesWarnBox(value: Boolean) = set { it.copy(showChatRulesWarnBox = value) }

    override suspend fun setSelectedMarketCode(value: String) = set { it.copy(selectedMarketCode = value) }

    override suspend fun setNotificationPermissionState(value: PermissionState) = set { it.copy(notificationPermissionState = value) }

    override suspend fun setBatteryOptimizationPermissionState(value: BatteryOptimizationState) = set { it.copy(batteryOptimizationState = value) }

    override suspend fun update(transform: suspend (t: Settings) -> Settings) = set(transform)

    override suspend fun setMarketSortBy(value: MarketSortBy) = set { it.copy(marketSortBy = value) }

    override suspend fun setMarketFilter(value: MarketFilter) = set { it.copy(marketFilter = value) }

    override suspend fun setDontShowAgainHyperlinksOpenInBrowser(value: Boolean) = set { it.copy(dontShowAgainHyperlinksOpenInBrowser = value) }

    override suspend fun setPermitOpeningBrowser(value: Boolean) = set { it.copy(cookiePermitOpeningBrowser = value) }

    override suspend fun setAnalyticsEnabled(value: Boolean) = set { it.copy(analyticsEnabled = value) }

    override suspend fun setAnalyticsPromptSeen(value: Boolean) = set { it.copy(analyticsPromptSeen = value) }

    override suspend fun setAnalyticsBaselineSent(value: Boolean) = set { it.copy(analyticsBaselineSent = value) }

    override suspend fun setRememberOfferbookFilterPreferences(value: Boolean) = set { it.copy(rememberOfferbookFilterPreferences = value) }
}
