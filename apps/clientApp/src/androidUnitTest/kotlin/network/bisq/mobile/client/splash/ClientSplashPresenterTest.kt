package network.bisq.mobile.client.splash

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.common.di.clientTestModule
import network.bisq.mobile.client.common.domain.websocket.ConnectionState
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.domain.data.model.BatteryOptimizationState
import network.bisq.mobile.domain.data.model.PermissionState
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.replicated.chat.notifications.ChatChannelNotificationTypeEnum
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.presentation.main.MainPresenter
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Tests for ClientSplashPresenter navigation logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClientSplashPresenterTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var webSocketClientService: WebSocketClientService
    private lateinit var applicationBootstrapFacade: ApplicationBootstrapFacade
    private lateinit var userProfileService: UserProfileServiceFacade
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var settingsServiceFacade: SettingsServiceFacade
    private lateinit var versionProvider: VersionProvider
    private lateinit var mainPresenter: MainPresenter

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mocks
        webSocketClientService = mockk(relaxed = true)
        applicationBootstrapFacade = mockk(relaxed = true)
        userProfileService = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)

        // Setup applicationBootstrapFacade state flows
        every { applicationBootstrapFacade.state } returns MutableStateFlow("Test State")
        every { applicationBootstrapFacade.progress } returns MutableStateFlow(0f)
        every { applicationBootstrapFacade.isTimeoutDialogVisible } returns MutableStateFlow(false)
        every { applicationBootstrapFacade.isBootstrapFailed } returns MutableStateFlow(false)
        every { applicationBootstrapFacade.torBootstrapFailed } returns MutableStateFlow(false)
        every { applicationBootstrapFacade.currentBootstrapStage } returns MutableStateFlow("")
        every { applicationBootstrapFacade.shouldShowProgressToast } returns MutableStateFlow(false)

        settingsRepository = FakeSettingsRepository()
        settingsServiceFacade = FakeSettingsServiceFacade()
        versionProvider = FakeVersionProvider()

        startKoin {
            modules(
                clientTestModule,
                module {
                    single<WebSocketClientService> { webSocketClientService }
                    single<ApplicationBootstrapFacade> { applicationBootstrapFacade }
                },
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun `isConnected returns true when connection state is Connected`() =
        runTest {
            // Given
            every { webSocketClientService.connectionState } returns
                MutableStateFlow<ConnectionState>(ConnectionState.Connected)
            every { webSocketClientService.isConnected() } returns true

            // Then
            assertTrue(webSocketClientService.isConnected())
        }

    @Test
    fun `isConnected returns false when connection state is Disconnected`() =
        runTest {
            // Given
            every { webSocketClientService.connectionState } returns
                MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
            every { webSocketClientService.isConnected() } returns false

            // Then
            assertFalse(webSocketClientService.isConnected())
        }

    @Test
    fun `isConnected returns false when connection state is Connecting`() =
        runTest {
            // Given
            every { webSocketClientService.connectionState } returns
                MutableStateFlow<ConnectionState>(ConnectionState.Connecting)
            every { webSocketClientService.isConnected() } returns false

            // Then
            assertFalse(webSocketClientService.isConnected())
        }

    // --- Fake implementations ---

    private class FakeSettingsRepository : SettingsRepository {
        private val _data = MutableStateFlow(Settings())
        override val data: StateFlow<Settings> = _data

        override suspend fun setFirstLaunch(value: Boolean) {}

        override suspend fun setShowChatRulesWarnBox(value: Boolean) {}

        override suspend fun setSelectedMarketCode(value: String) {}

        override suspend fun setNotificationPermissionState(value: PermissionState) {}

        override suspend fun setBatteryOptimizationPermissionState(value: BatteryOptimizationState) {}

        override suspend fun update(transform: suspend (t: Settings) -> Settings) {
            _data.value = transform(_data.value)
        }

        override suspend fun clear() {
            _data.value = Settings()
        }
    }

    private class FakeSettingsServiceFacade : SettingsServiceFacade {
        override suspend fun getSettings() =
            Result.success(
                SettingsVO(isTacAccepted = true, tradeRulesConfirmed = true),
            )

        override val isTacAccepted: StateFlow<Boolean?> = MutableStateFlow(true)

        override suspend fun confirmTacAccepted(value: Boolean) {}

        override val tradeRulesConfirmed: StateFlow<Boolean> = MutableStateFlow(true)

        override suspend fun confirmTradeRules(value: Boolean) {}

        override val languageCode: StateFlow<String> = MutableStateFlow("en")

        override suspend fun setLanguageCode(value: String) {}

        override val supportedLanguageCodes: StateFlow<Set<String>> = MutableStateFlow(setOf("en"))

        override suspend fun setSupportedLanguageCodes(value: Set<String>) {}

        override val chatNotificationType = MutableStateFlow(ChatChannelNotificationTypeEnum.ALL)

        override suspend fun setChatNotificationType(value: ChatChannelNotificationTypeEnum) {}

        override val closeMyOfferWhenTaken: StateFlow<Boolean> = MutableStateFlow(false)

        override suspend fun setCloseMyOfferWhenTaken(value: Boolean) {}

        override val maxTradePriceDeviation: StateFlow<Double> = MutableStateFlow(0.1)

        override suspend fun setMaxTradePriceDeviation(value: Double) {}

        override val useAnimations: StateFlow<Boolean> = MutableStateFlow(true)

        override suspend fun setUseAnimations(value: Boolean) {}

        override val difficultyAdjustmentFactor: StateFlow<Double> = MutableStateFlow(1.0)

        override suspend fun setDifficultyAdjustmentFactor(value: Double) {}

        override val ignoreDiffAdjustmentFromSecManager: StateFlow<Boolean> = MutableStateFlow(false)

        override suspend fun setIgnoreDiffAdjustmentFromSecManager(value: Boolean) {}

        override val numDaysAfterRedactingTradeData: StateFlow<Int> = MutableStateFlow(30)

        override suspend fun setNumDaysAfterRedactingTradeData(days: Int) {}
    }

    private class FakeVersionProvider : VersionProvider {
        override fun getVersionInfo(
            isDemo: Boolean,
            isIOS: Boolean,
        ) = "Test v1.0.0"

        override fun getAppNameAndVersion(
            isDemo: Boolean,
            isIOS: Boolean,
        ) = "Test v1.0.0"
    }
}
