package network.bisq.mobile.presentation.tabs.dashboard

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.data.model.PermissionState
import network.bisq.mobile.domain.data.repository.SettingsRepositoryMock
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.network.NetworkServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.push_notification.PushNotificationServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.platform_settings.PlatformSettingsManager
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.NoopNavigationManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.platform.getScreenWidthDp
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardPresenterPushNotificationTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private val settingsRepository = SettingsRepositoryMock()
    private val pushNotificationServiceFacade = mockk<PushNotificationServiceFacade>(relaxed = true)
    private val notificationController = mockk<NotificationController>(relaxed = true)

    private lateinit var presenter: DashboardPresenter

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
        every { getScreenWidthDp() } returns 480

        val koinModule =
            module {
                single { CoroutineExceptionHandlerSetup() }
                factory<CoroutineJobsManager> {
                    DefaultCoroutineJobsManager().apply {
                        get<CoroutineExceptionHandlerSetup>().setupExceptionHandler(this)
                    }
                }
                single { NoopNavigationManager() as network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager }
                single { GlobalUiManager() }
            }
        startKoin { modules(koinModule) }

        every { pushNotificationServiceFacade.isDeviceRegistered } returns MutableStateFlow(false)
        every { pushNotificationServiceFacade.isPushNotificationsEnabled } returns MutableStateFlow(false)

        val mainPresenter = MainPresenterTestFactory.create()
        val offersServiceFacade = mockk<OffersServiceFacade>(relaxed = true)
        every { offersServiceFacade.offerbookMarketItems } returns MutableStateFlow(emptyList())
        val userProfileServiceFacade = mockk<UserProfileServiceFacade>(relaxed = true)
        every { userProfileServiceFacade.numUserProfiles } returns MutableStateFlow(0)
        val networkServiceFacade = mockk<NetworkServiceFacade>(relaxed = true)
        every { networkServiceFacade.numConnections } returns MutableStateFlow(0)
        val marketPriceServiceFacade = mockk<MarketPriceServiceFacade>(relaxed = true)
        every { marketPriceServiceFacade.selectedFormattedMarketPrice } returns MutableStateFlow("")
        val settingsServiceFacade = mockk<SettingsServiceFacade>(relaxed = true)
        every { settingsServiceFacade.tradeRulesConfirmed } returns MutableStateFlow(false)

        presenter =
            DashboardPresenter(
                mainPresenter = mainPresenter,
                userProfileServiceFacade = userProfileServiceFacade,
                marketPriceServiceFacade = marketPriceServiceFacade,
                offersServiceFacade = offersServiceFacade,
                settingsServiceFacade = settingsServiceFacade,
                networkServiceFacade = networkServiceFacade,
                settingsRepository = settingsRepository,
                notificationController = notificationController,
                foregroundDetector = mockk(relaxed = true),
                platformSettingsManager = mockk<PlatformSettingsManager>(relaxed = true),
                pushNotificationServiceFacade = pushNotificationServiceFacade,
            )
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
        unmockkStatic("network.bisq.mobile.presentation.common.ui.platform.PlatformPresentationAbstractions_androidKt")
    }

    @Test
    fun `GRANTED triggers registerForPushNotifications`() =
        runBlocking {
            coEvery { pushNotificationServiceFacade.registerForPushNotifications() } returns Result.success(Unit)

            presenter.saveNotificationPermissionState(PermissionState.GRANTED)

            coVerify { pushNotificationServiceFacade.registerForPushNotifications() }
        }

    @Test
    fun `GRANTED skips registration when device already registered`() =
        runBlocking {
            every { pushNotificationServiceFacade.isDeviceRegistered } returns MutableStateFlow(true)
            coEvery { pushNotificationServiceFacade.registerForPushNotifications() } returns Result.success(Unit)

            presenter.saveNotificationPermissionState(PermissionState.GRANTED)

            coVerify(exactly = 0) { pushNotificationServiceFacade.registerForPushNotifications() }
        }

    @Test
    fun `NOT_GRANTED triggers unregister`() =
        runBlocking {
            coEvery { pushNotificationServiceFacade.unregisterFromPushNotifications() } returns Result.success(Unit)

            presenter.saveNotificationPermissionState(PermissionState.NOT_GRANTED)

            coVerify { pushNotificationServiceFacade.unregisterFromPushNotifications() }
        }

    @Test
    fun `DENIED triggers unregister`() =
        runBlocking {
            coEvery { pushNotificationServiceFacade.unregisterFromPushNotifications() } returns Result.success(Unit)

            presenter.saveNotificationPermissionState(PermissionState.DENIED)

            coVerify { pushNotificationServiceFacade.unregisterFromPushNotifications() }
        }

    @Test
    fun `re-enable after disable triggers re-registration`() =
        runBlocking {
            val isRegistered = MutableStateFlow(true)
            every { pushNotificationServiceFacade.isDeviceRegistered } returns isRegistered
            coEvery { pushNotificationServiceFacade.unregisterFromPushNotifications() } answers {
                isRegistered.value = false
                Result.success(Unit)
            }
            coEvery { pushNotificationServiceFacade.registerForPushNotifications() } returns Result.success(Unit)

            // User disables notifications from iOS Settings
            presenter.saveNotificationPermissionState(PermissionState.NOT_GRANTED)
            coVerify { pushNotificationServiceFacade.unregisterFromPushNotifications() }

            // User re-enables notifications from iOS Settings
            presenter.saveNotificationPermissionState(PermissionState.GRANTED)
            coVerify { pushNotificationServiceFacade.registerForPushNotifications() }
        }

    @Test
    fun `DONT_ASK_AGAIN does not trigger unregister`() =
        runBlocking {
            presenter.saveNotificationPermissionState(PermissionState.DONT_ASK_AGAIN)

            coVerify(exactly = 0) { pushNotificationServiceFacade.unregisterFromPushNotifications() }
            coVerify(exactly = 0) { pushNotificationServiceFacade.registerForPushNotifications() }
        }
}
