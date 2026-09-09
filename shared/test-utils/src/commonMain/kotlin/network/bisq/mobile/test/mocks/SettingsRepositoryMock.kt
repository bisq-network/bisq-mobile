package network.bisq.mobile.test.mocks

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import network.bisq.mobile.data.model.BatteryOptimizationState
import network.bisq.mobile.data.model.CommunityNotificationLevel
import network.bisq.mobile.data.model.PermissionState
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.data.model.market.MarketFilter
import network.bisq.mobile.data.model.market.MarketSortBy
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.utils.Logging

/**
 * Stateful [SettingsRepository] double: every setter really mutates [mutableData], so a test can set
 * a value and then assert on [data].
 *
 * @param initial seed state, for tests that need a non-default [Settings] before anything observes it.
 * @param fetchException when non-null, [fetch] throws it — for exercising read-failure paths.
 */
class SettingsRepositoryMock(
    initial: Settings = Settings(),
    private val fetchException: Throwable? = null,
) : SettingsRepository,
    Logging {
    /**
     * Exposed so tests can seed or flip state from a non-suspend context (e.g. inside a Compose or
     * JUnit test body that isn't a coroutine). Prefer the suspend setters where you can.
     */
    val mutableData = MutableStateFlow(initial)
    override val data: StateFlow<Settings> = mutableData.asStateFlow()

    override suspend fun fetch(): Settings = fetchException?.let { throw it } ?: mutableData.value

    override suspend fun setFirstLaunch(value: Boolean) {
        mutableData.update {
            it.copy(firstLaunch = value)
        }
    }

    override suspend fun setShowChatRulesWarnBox(value: Boolean) {
        mutableData.update {
            it.copy(showChatRulesWarnBox = value)
        }
    }

    override suspend fun setCommunityNotificationLevel(value: CommunityNotificationLevel) {
        mutableData.update {
            it.copy(communityNotificationLevel = value)
        }
    }

    override suspend fun setSelectedMarketCode(value: String) {
        mutableData.update {
            it.copy(selectedMarketCode = value)
        }
    }

    override suspend fun setNotificationPermissionState(value: PermissionState) {
        mutableData.update {
            it.copy(notificationPermissionState = value)
        }
    }

    override suspend fun setBatteryOptimizationPermissionState(value: BatteryOptimizationState) {
        mutableData.update {
            it.copy(batteryOptimizationState = value)
        }
    }

    override suspend fun update(transform: suspend (Settings) -> Settings) {
        mutableData.value = transform(mutableData.value)
    }

    override suspend fun clear() {
        mutableData.update {
            Settings()
        }
    }

    override suspend fun setMarketSortBy(value: MarketSortBy) {
        mutableData.update {
            it.copy(marketSortBy = value)
        }
    }

    override suspend fun setMarketFilter(value: MarketFilter) {
        mutableData.update {
            it.copy(marketFilter = value)
        }
    }

    override suspend fun setDontShowAgainHyperlinksOpenInBrowser(value: Boolean) {
        mutableData.update {
            it.copy(dontShowAgainHyperlinksOpenInBrowser = value)
        }
    }

    override suspend fun setPermitOpeningBrowser(value: Boolean) {
        mutableData.update {
            it.copy(cookiePermitOpeningBrowser = value)
        }
    }

    override suspend fun setAnalyticsEnabled(value: Boolean) {
        mutableData.update {
            it.copy(analyticsEnabled = value)
        }
    }

    override suspend fun setAnalyticsPromptSeen(value: Boolean) {
        mutableData.update {
            it.copy(analyticsPromptSeen = value)
        }
    }

    override suspend fun setAnalyticsBaselineSent(value: Boolean) {
        mutableData.update {
            it.copy(analyticsBaselineSent = value)
        }
    }

    override suspend fun setRememberOfferbookFilterPreferences(value: Boolean) {
        mutableData.update {
            it.copy(rememberOfferbookFilterPreferences = value)
        }
    }
}
