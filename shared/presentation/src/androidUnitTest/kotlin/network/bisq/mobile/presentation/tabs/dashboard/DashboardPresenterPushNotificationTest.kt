package network.bisq.mobile.presentation.tabs.dashboard

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import network.bisq.mobile.data.model.PermissionState
import network.bisq.mobile.data.service.ForegroundDetector
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.network.NetworkServiceFacade
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.push_notification.PushNotificationServiceFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.getPlatformInfo
import network.bisq.mobile.domain.model.PlatformInfo
import network.bisq.mobile.domain.model.PlatformType
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.platform_settings.PlatformSettingsManager
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import network.bisq.mobile.test.presentation.coroutines.PlatformPresentationKoinTestBase
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardPresenterPushNotificationTest : PlatformPresentationKoinTestBase() {
    override val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    private val settingsRepository = SettingsRepositoryMock()
    private val pushNotificationServiceFacade = mockk<PushNotificationServiceFacade>(relaxed = true)
    private val notificationController = mockk<NotificationController>(relaxed = true)
    private val offersServiceFacade = mockk<OffersServiceFacade>(relaxed = true)
    private val userProfileServiceFacade = mockk<UserProfileServiceFacade>(relaxed = true)
    private val networkServiceFacade = mockk<NetworkServiceFacade>(relaxed = true)
    private val marketPriceServiceFacade = mockk<MarketPriceServiceFacade>(relaxed = true)
    private val settingsServiceFacade = mockk<SettingsServiceFacade>(relaxed = true)
    private val foregroundDetector = mockk<ForegroundDetector>(relaxed = true)
    private val platformSettingsManager = mockk<PlatformSettingsManager>(relaxed = true)

    private lateinit var presenter: DashboardPresenter

    override fun onKoinReady() {
        // Mock platform as iOS since push notification logic is iOS-only
        mockkStatic("network.bisq.mobile.data.utils.PlatformDomainAbstractions_androidKt")
        every { getPlatformInfo() } returns
            object : PlatformInfo {
                override val name = "iOS"
                override val type = PlatformType.IOS
            }

        every { pushNotificationServiceFacade.isDeviceRegistered } returns MutableStateFlow(false)
        every { pushNotificationServiceFacade.isPushNotificationsEnabled } returns MutableStateFlow(false)
        every { offersServiceFacade.offerbookMarketItems } returns MutableStateFlow(emptyList())
        every { userProfileServiceFacade.numUserProfiles } returns MutableStateFlow(0)
        every { networkServiceFacade.numConnections } returns MutableStateFlow(0)
        every { marketPriceServiceFacade.selectedFormattedMarketPrice } returns MutableStateFlow("")
        every { settingsServiceFacade.tradeRulesConfirmed } returns MutableStateFlow(false)

        val mainPresenter = MainPresenterTestFactory.create()

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
                foregroundDetector = foregroundDetector,
                platformSettingsManager = platformSettingsManager,
                pushNotificationServiceFacade = pushNotificationServiceFacade,
            )
    }

    override fun onTearDown() {
        try {
            unmockkStatic("network.bisq.mobile.data.utils.PlatformDomainAbstractions_androidKt")
        } finally {
            super.onTearDown()
        }
    }

    @Test
    fun `GRANTED triggers registerForPushNotifications`() =
        runTest {
            coEvery { pushNotificationServiceFacade.registerForPushNotifications() } returns Result.success(Unit)

            presenter.saveNotificationPermissionState(PermissionState.GRANTED)

            coVerify { pushNotificationServiceFacade.registerForPushNotifications() }
        }

    @Test
    fun `GRANTED skips registration when device already registered`() =
        runTest {
            every { pushNotificationServiceFacade.isDeviceRegistered } returns MutableStateFlow(true)
            coEvery { pushNotificationServiceFacade.registerForPushNotifications() } returns Result.success(Unit)

            presenter.saveNotificationPermissionState(PermissionState.GRANTED)

            coVerify(exactly = 0) { pushNotificationServiceFacade.registerForPushNotifications() }
        }

    @Test
    fun `NOT_GRANTED triggers unregister`() =
        runTest {
            coEvery { pushNotificationServiceFacade.unregisterFromPushNotifications() } returns Result.success(Unit)

            presenter.saveNotificationPermissionState(PermissionState.NOT_GRANTED)

            coVerify { pushNotificationServiceFacade.unregisterFromPushNotifications() }
        }

    @Test
    fun `DENIED triggers unregister`() =
        runTest {
            coEvery { pushNotificationServiceFacade.unregisterFromPushNotifications() } returns Result.success(Unit)

            presenter.saveNotificationPermissionState(PermissionState.DENIED)

            coVerify { pushNotificationServiceFacade.unregisterFromPushNotifications() }
        }

    @Test
    fun `re-enable after disable triggers re-registration`() =
        runTest {
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
        runTest {
            presenter.saveNotificationPermissionState(PermissionState.DONT_ASK_AGAIN)

            coVerify(exactly = 0) { pushNotificationServiceFacade.unregisterFromPushNotifications() }
            coVerify(exactly = 0) { pushNotificationServiceFacade.registerForPushNotifications() }
        }

    @Test
    fun `Android platform skips push notification registration entirely`() =
        runTest {
            // Override platform mock to Android
            every { getPlatformInfo() } returns
                object : PlatformInfo {
                    override val name = "Android"
                    override val type = PlatformType.ANDROID
                }

            presenter.saveNotificationPermissionState(PermissionState.GRANTED)

            coVerify(exactly = 0) { pushNotificationServiceFacade.registerForPushNotifications() }
            coVerify(exactly = 0) { pushNotificationServiceFacade.unregisterFromPushNotifications() }
        }
}
