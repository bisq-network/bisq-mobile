package network.bisq.mobile.domain.analytics

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import network.bisq.mobile.data.model.BatteryOptimizationState
import network.bisq.mobile.data.model.PermissionState
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.data.model.market.MarketFilter
import network.bisq.mobile.data.model.market.MarketSortBy
import network.bisq.mobile.domain.repository.SettingsRepository
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.fail

/**
 * Covers the gate wiring done by [createBufferedAnalyticsService]: the
 * runtimeOptInProvider handed to the SDK must be the AND of the dev build flag
 * and the LIVE user-settings toggle (a settings flip propagates without
 * rebuilding the service).
 */
class BufferedAnalyticsServiceFactoryTest {
    private class FakeInitializer : NativeSentryInitializer {
        var capturedOptInProvider: (() -> Boolean)? = null

        override fun init(
            dsn: String,
            environment: String,
            release: String,
            redactor: AnalyticsRedactor,
            isDebug: Boolean,
            socksProxyHost: String?,
            socksProxyPort: Int?,
            runtimeOptInProvider: () -> Boolean,
        ) {
            capturedOptInProvider = runtimeOptInProvider
        }
    }

    private class FakeSettingsRepository(
        initial: Settings,
    ) : SettingsRepository {
        private val settings = MutableStateFlow(initial)
        override val data: Flow<Settings> get() = settings

        fun setAnalytics(value: Boolean) {
            settings.update { it.copy(analyticsEnabled = value) }
        }

        override suspend fun setFirstLaunch(value: Boolean) = Unit

        override suspend fun setShowChatRulesWarnBox(value: Boolean) = Unit

        override suspend fun setSelectedMarketCode(value: String) = Unit

        override suspend fun setNotificationPermissionState(value: PermissionState) = Unit

        override suspend fun setBatteryOptimizationPermissionState(value: BatteryOptimizationState) = Unit

        override suspend fun update(transform: suspend (t: Settings) -> Settings) = Unit

        override suspend fun clear() = Unit

        override suspend fun setMarketSortBy(value: MarketSortBy) = Unit

        override suspend fun setMarketFilter(value: MarketFilter) = Unit

        override suspend fun setDontShowAgainHyperlinksOpenInBrowser(value: Boolean) = Unit

        override suspend fun setPermitOpeningBrowser(value: Boolean) = Unit

        override suspend fun setAnalyticsEnabled(value: Boolean) = Unit

        override suspend fun setAnalyticsPromptSeen(value: Boolean) = Unit

        override suspend fun setAnalyticsBaselineSent(value: Boolean) = Unit

        override suspend fun setRememberOfferbookFilterPreferences(value: Boolean) = Unit
    }

    private fun buildAndCaptureProvider(
        repository: FakeSettingsRepository,
        analyticsDevEnabled: Boolean,
    ): () -> Boolean {
        val initializer = FakeInitializer()
        val service =
            createBufferedAnalyticsService(
                settingsRepository = repository,
                nativeInitializer = initializer,
                analyticsDevEnabled = analyticsDevEnabled,
            )
        service.init(
            dsn = "https://key@glitchtip.example/1",
            environment = "test",
            release = "test@1",
            isDebug = true,
            socksProxyHost = null,
            socksProxyPort = null,
        )
        val provider = initializer.capturedOptInProvider
        assertNotNull(provider, "factory must thread the opt-in provider into the native SDK init")
        return provider
    }

    /**
     * The settings-derived StateFlow inside the factory is collected eagerly on
     * Dispatchers.Default, so propagation is asynchronous — poll with a timeout.
     */
    private fun awaitCondition(
        message: String,
        condition: () -> Boolean,
    ) {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(10)
        }
        fail(message)
    }

    @Test
    fun `provider is false when dev flag is off even if user opted in`() {
        val repository = FakeSettingsRepository(Settings(analyticsEnabled = true))

        val provider = buildAndCaptureProvider(repository, analyticsDevEnabled = false)

        // Give the settings flow time to propagate — the dev gate must still win
        Thread.sleep(100)
        assertFalse(provider(), "dev flag off must gate emission regardless of user opt-in")
    }

    @Test
    fun `provider is false while user has not opted in`() {
        val repository = FakeSettingsRepository(Settings(analyticsEnabled = false))

        val provider = buildAndCaptureProvider(repository, analyticsDevEnabled = true)

        Thread.sleep(100)
        assertFalse(provider(), "user opt-out must gate emission")
    }

    @Test
    fun `provider becomes true when dev flag is on and user opted in`() {
        val repository = FakeSettingsRepository(Settings(analyticsEnabled = true))

        val provider = buildAndCaptureProvider(repository, analyticsDevEnabled = true)

        awaitCondition("provider should turn true once the settings flow propagates") { provider() }
    }

    @Test
    fun `runtime settings flip propagates to the provider without rebuilding`() {
        val repository = FakeSettingsRepository(Settings(analyticsEnabled = false))

        val provider = buildAndCaptureProvider(repository, analyticsDevEnabled = true)

        Thread.sleep(100)
        assertFalse(provider(), "starts opted out")

        repository.setAnalytics(true)
        awaitCondition("opt-in flip should propagate to the provider") { provider() }

        repository.setAnalytics(false)
        awaitCondition("opt-out flip should propagate to the provider") { !provider() }
    }
}
