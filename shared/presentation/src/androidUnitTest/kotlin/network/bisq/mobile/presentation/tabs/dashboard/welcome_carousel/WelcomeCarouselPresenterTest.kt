package network.bisq.mobile.presentation.tabs.dashboard.welcome_carousel

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.model.BatteryOptimizationState
import network.bisq.mobile.data.model.PermissionState
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.push_notification.PushNotificationServiceFacade
import network.bisq.mobile.domain.model.PlatformInfo
import network.bisq.mobile.domain.model.PlatformType
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.utils.BisqLinks
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.coroutines.TestCoroutineJobsManager
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeCarouselPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mainPresenter: MainPresenter
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var pushNotificationServiceFacade: PushNotificationServiceFacade
    private lateinit var settingsFlow: MutableStateFlow<Settings>
    private lateinit var pushEnabledFlow: MutableStateFlow<Boolean>

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        ApplicationBootstrapFacade.isDemo = false

        mainPresenter = mockk(relaxed = true)

        settingsFlow = MutableStateFlow(Settings())
        settingsRepository = mockk(relaxed = true)
        every { settingsRepository.data } returns settingsFlow
        coEvery { settingsRepository.setNotificationPermissionState(any()) } answers {
            val next = firstArg<PermissionState>()
            settingsFlow.value = settingsFlow.value.copy(notificationPermissionState = next)
        }
        coEvery { settingsRepository.setBatteryOptimizationPermissionState(any()) } answers {
            val next = firstArg<BatteryOptimizationState>()
            settingsFlow.value = settingsFlow.value.copy(batteryOptimizationState = next)
        }
        coEvery { settingsRepository.setAnalyticsPromptSeen(any()) } answers {
            val next = firstArg<Boolean>()
            settingsFlow.value = settingsFlow.value.copy(analyticsPromptSeen = next)
        }
        // The presenter uses `settingsRepository.update { it.copy(...) }` for the
        // analytics-enable path so both fields land atomically. Run the transform
        // against the live state to keep test reactivity working.
        val transformSlot = slot<suspend (Settings) -> Settings>()
        coEvery { settingsRepository.update(capture(transformSlot)) } coAnswers {
            settingsFlow.value = transformSlot.captured.invoke(settingsFlow.value)
        }

        pushEnabledFlow = MutableStateFlow(false)
        pushNotificationServiceFacade = mockk(relaxed = true)
        every { pushNotificationServiceFacade.isPushNotificationsEnabled } returns pushEnabledFlow

        startKoin {
            modules(
                module {
                    single<NavigationManager> { mockk(relaxed = true) }
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<GlobalUiManager> { mockk(relaxed = true) }
                },
            )
        }
    }

    @AfterTest
    fun tearDown() {
        try {
            stopKoin()
        } finally {
            ApplicationBootstrapFacade.isDemo = false
            Dispatchers.resetMain()
        }
    }

    private fun newPresenter(platform: PlatformType = PlatformType.ANDROID): WelcomeCarouselPresenter =
        WelcomeCarouselPresenter(
            mainPresenter = mainPresenter,
            settingsRepository = settingsRepository,
            pushNotificationServiceFacade = pushNotificationServiceFacade,
            platformInfo =
                object : PlatformInfo {
                    override val name: String = "Test"
                    override val type: PlatformType = platform
                },
        )

    @Test
    fun `pending pages include NOTIFICATIONS when state is NOT_GRANTED`() =
        runTest(testDispatcher) {
            // Other cards suppressed to isolate NOTIFICATIONS logic.
            settingsFlow.value =
                Settings(
                    notificationPermissionState = PermissionState.NOT_GRANTED,
                    batteryOptimizationState = BatteryOptimizationState.IGNORED,
                    analyticsPromptSeen = true,
                )
            val presenter = newPresenter()

            advanceUntilIdle()

            assertEquals(
                listOf(CarouselPageType.NOTIFICATIONS),
                presenter.uiState.value.pages,
            )
        }

    @Test
    fun `pending pages include NOTIFICATIONS when state is DENIED`() =
        runTest(testDispatcher) {
            settingsFlow.value =
                Settings(
                    notificationPermissionState = PermissionState.DENIED,
                    batteryOptimizationState = BatteryOptimizationState.IGNORED,
                    analyticsPromptSeen = true,
                )
            val presenter = newPresenter()

            advanceUntilIdle()

            assertEquals(
                listOf(CarouselPageType.NOTIFICATIONS),
                presenter.uiState.value.pages,
            )
        }

    @Test
    fun `NOTIFICATIONS resolved by DONT_ASK_AGAIN`() =
        runTest(testDispatcher) {
            settingsFlow.value =
                Settings(
                    notificationPermissionState = PermissionState.DONT_ASK_AGAIN,
                    batteryOptimizationState = BatteryOptimizationState.IGNORED,
                    analyticsPromptSeen = true,
                )
            val presenter = newPresenter()

            advanceUntilIdle()

            assertTrue(
                presenter.uiState.value.pages
                    .isEmpty(),
            )
        }

    @Test
    fun `BATTERY pending on Android when notifications GRANTED and battery NOT_IGNORED and relayed push off`() =
        runTest(testDispatcher) {
            settingsFlow.value =
                Settings(
                    notificationPermissionState = PermissionState.GRANTED,
                    batteryOptimizationState = BatteryOptimizationState.NOT_IGNORED,
                    analyticsPromptSeen = true,
                )
            val presenter = newPresenter(platform = PlatformType.ANDROID)

            advanceUntilIdle()

            assertEquals(
                listOf(CarouselPageType.BATTERY),
                presenter.uiState.value.pages,
            )
        }

    @Test
    fun `BATTERY never pending on iOS`() =
        runTest(testDispatcher) {
            settingsFlow.value =
                Settings(
                    notificationPermissionState = PermissionState.GRANTED,
                    batteryOptimizationState = BatteryOptimizationState.NOT_IGNORED,
                    analyticsPromptSeen = true,
                )
            val presenter = newPresenter(platform = PlatformType.IOS)

            advanceUntilIdle()

            assertTrue(
                presenter.uiState.value.pages
                    .isEmpty(),
            )
        }

    @Test
    fun `BATTERY suppressed when relayed push notifications enabled`() =
        runTest(testDispatcher) {
            settingsFlow.value =
                Settings(
                    notificationPermissionState = PermissionState.GRANTED,
                    batteryOptimizationState = BatteryOptimizationState.NOT_IGNORED,
                    analyticsPromptSeen = true,
                )
            pushEnabledFlow.value = true
            val presenter = newPresenter(platform = PlatformType.ANDROID)

            advanceUntilIdle()

            assertTrue(
                presenter.uiState.value.pages
                    .isEmpty(),
            )
        }

    @Test
    fun `NOTIFICATIONS and BATTERY are pending independently when analytics already seen`() =
        runTest(testDispatcher) {
            settingsFlow.value =
                Settings(
                    notificationPermissionState = PermissionState.NOT_GRANTED,
                    batteryOptimizationState = BatteryOptimizationState.NOT_IGNORED,
                    analyticsPromptSeen = true,
                )
            val presenter = newPresenter(platform = PlatformType.ANDROID)

            advanceUntilIdle()

            // Both cards are independently pending — BATTERY does not wait for
            // notifications to be GRANTED. The user can address them in any order.
            assertEquals(
                listOf(CarouselPageType.NOTIFICATIONS, CarouselPageType.BATTERY),
                presenter.uiState.value.pages,
            )
        }

    @Test
    fun `BATTERY remains pending after NOTIFICATIONS is dismissed via Don't ask again`() =
        runTest(testDispatcher) {
            settingsFlow.value =
                Settings(
                    notificationPermissionState = PermissionState.NOT_GRANTED,
                    batteryOptimizationState = BatteryOptimizationState.NOT_IGNORED,
                    analyticsPromptSeen = true,
                )
            val presenter = newPresenter(platform = PlatformType.ANDROID)
            advanceUntilIdle()

            presenter.onAction(
                WelcomeCarouselUiAction.OnDontAskAgain(CarouselPageType.NOTIFICATIONS),
            )
            advanceUntilIdle()

            // NOTIFICATIONS dismissal does not collapse BATTERY — each card is
            // independent of the user's choice on any other card.
            assertEquals(
                listOf(CarouselPageType.BATTERY),
                presenter.uiState.value.pages,
            )
        }

    @Test
    fun `demo mode forces empty pages regardless of underlying state`() =
        runTest(testDispatcher) {
            ApplicationBootstrapFacade.isDemo = true
            settingsFlow.value =
                Settings(
                    notificationPermissionState = PermissionState.NOT_GRANTED,
                    batteryOptimizationState = BatteryOptimizationState.NOT_IGNORED,
                    // analyticsPromptSeen left default (false) — demo must override
                    // analytics card too.
                )
            val presenter = newPresenter(platform = PlatformType.ANDROID)

            advanceUntilIdle()

            assertTrue(
                presenter.uiState.value.pages
                    .isEmpty(),
            )
        }

    @Test
    fun `pages transition live as cards resolve in any order`() =
        runTest(testDispatcher) {
            // First-launch: both OS-coupled cards pending; analytics deferred for
            // isolation in this scenario.
            settingsFlow.value =
                Settings(
                    notificationPermissionState = PermissionState.NOT_GRANTED,
                    batteryOptimizationState = BatteryOptimizationState.NOT_IGNORED,
                    analyticsPromptSeen = true,
                )
            val presenter = newPresenter(platform = PlatformType.ANDROID)
            advanceUntilIdle()
            assertEquals(
                listOf(CarouselPageType.NOTIFICATIONS, CarouselPageType.BATTERY),
                presenter.uiState.value.pages,
            )

            // User grants notifications → NOTIFICATIONS resolved, BATTERY still pending
            settingsFlow.value =
                settingsFlow.value.copy(notificationPermissionState = PermissionState.GRANTED)
            advanceUntilIdle()
            assertEquals(
                listOf(CarouselPageType.BATTERY),
                presenter.uiState.value.pages,
            )

            // Battery resolved — carousel dismisses
            settingsFlow.value =
                settingsFlow.value.copy(batteryOptimizationState = BatteryOptimizationState.IGNORED)
            advanceUntilIdle()
            assertTrue(
                presenter.uiState.value.pages
                    .isEmpty(),
            )
        }

    @Test
    fun `onDontAskAgain NOTIFICATIONS persists DONT_ASK_AGAIN`() =
        runTest(testDispatcher) {
            val presenter = newPresenter()
            advanceUntilIdle()

            presenter.onAction(
                WelcomeCarouselUiAction.OnDontAskAgain(CarouselPageType.NOTIFICATIONS),
            )
            advanceUntilIdle()

            coVerify(exactly = 1) {
                settingsRepository.setNotificationPermissionState(PermissionState.DONT_ASK_AGAIN)
            }
        }

    @Test
    fun `onDontAskAgain BATTERY persists DONT_ASK_AGAIN`() =
        runTest(testDispatcher) {
            val presenter = newPresenter()
            advanceUntilIdle()

            presenter.onAction(
                WelcomeCarouselUiAction.OnDontAskAgain(CarouselPageType.BATTERY),
            )
            advanceUntilIdle()

            coVerify(exactly = 1) {
                settingsRepository.setBatteryOptimizationPermissionState(
                    BatteryOptimizationState.DONT_ASK_AGAIN,
                )
            }
        }

    // ------------------------------------------------------------------
    // ANALYTICS card
    // ------------------------------------------------------------------

    @Test
    fun `ANALYTICS pending when analyticsPromptSeen is false`() =
        runTest(testDispatcher) {
            // Other cards suppressed to isolate ANALYTICS logic.
            settingsFlow.value =
                Settings(
                    notificationPermissionState = PermissionState.GRANTED,
                    batteryOptimizationState = BatteryOptimizationState.IGNORED,
                    analyticsPromptSeen = false,
                )
            val presenter = newPresenter()

            advanceUntilIdle()

            assertEquals(
                listOf(CarouselPageType.ANALYTICS),
                presenter.uiState.value.pages,
            )
        }

    @Test
    fun `ANALYTICS not pending when analyticsPromptSeen is true`() =
        runTest(testDispatcher) {
            settingsFlow.value =
                Settings(
                    notificationPermissionState = PermissionState.GRANTED,
                    batteryOptimizationState = BatteryOptimizationState.IGNORED,
                    analyticsPromptSeen = true,
                )
            val presenter = newPresenter()

            advanceUntilIdle()

            assertTrue(
                presenter.uiState.value.pages
                    .isEmpty(),
            )
        }

    @Test
    fun `ANALYTICS not pending for users who already opted in via Settings`() =
        runTest(testDispatcher) {
            // Settings → Analytics toggle handler always sets promptSeen=true alongside
            // analyticsEnabled, so users who engaged via Settings should not see the
            // carousel card. Mirrors that observed state.
            settingsFlow.value =
                Settings(
                    notificationPermissionState = PermissionState.GRANTED,
                    batteryOptimizationState = BatteryOptimizationState.IGNORED,
                    analyticsEnabled = true,
                    analyticsPromptSeen = true,
                )
            val presenter = newPresenter()

            advanceUntilIdle()

            assertTrue(
                presenter.uiState.value.pages
                    .isEmpty(),
            )
        }

    @Test
    fun `fresh install shows all three cards in order NOTIFICATIONS BATTERY ANALYTICS`() =
        runTest(testDispatcher) {
            // Default Settings() values: no permission granted, battery unset,
            // analytics never prompted. This is what an existing user looks like
            // after upgrading to the version that adds the analytics card.
            settingsFlow.value = Settings()
            val presenter = newPresenter(platform = PlatformType.ANDROID)

            advanceUntilIdle()

            assertEquals(
                listOf(
                    CarouselPageType.NOTIFICATIONS,
                    CarouselPageType.BATTERY,
                    CarouselPageType.ANALYTICS,
                ),
                presenter.uiState.value.pages,
            )
        }

    @Test
    fun `OnEnableAnalytics persists analyticsEnabled and analyticsPromptSeen atomically`() =
        runTest(testDispatcher) {
            settingsFlow.value =
                Settings(
                    notificationPermissionState = PermissionState.GRANTED,
                    batteryOptimizationState = BatteryOptimizationState.IGNORED,
                    analyticsPromptSeen = false,
                )
            val presenter = newPresenter()
            advanceUntilIdle()

            presenter.onAction(WelcomeCarouselUiAction.OnEnableAnalytics)
            advanceUntilIdle()

            assertTrue(settingsFlow.value.analyticsEnabled)
            assertTrue(settingsFlow.value.analyticsPromptSeen)
            // And the carousel dismisses (analytics card resolved, other cards already
            // suppressed in this setup).
            assertTrue(
                presenter.uiState.value.pages
                    .isEmpty(),
            )
        }

    @Test
    fun `OnDontAskAgain ANALYTICS sets analyticsPromptSeen only and leaves analyticsEnabled off`() =
        runTest(testDispatcher) {
            settingsFlow.value =
                Settings(
                    notificationPermissionState = PermissionState.GRANTED,
                    batteryOptimizationState = BatteryOptimizationState.IGNORED,
                    analyticsPromptSeen = false,
                )
            val presenter = newPresenter()
            advanceUntilIdle()

            presenter.onAction(
                WelcomeCarouselUiAction.OnDontAskAgain(CarouselPageType.ANALYTICS),
            )
            advanceUntilIdle()

            coVerify(exactly = 1) { settingsRepository.setAnalyticsPromptSeen(true) }
            assertTrue(settingsFlow.value.analyticsPromptSeen)
            // Critical: dismissal must NOT opt the user in.
            assertFalse(settingsFlow.value.analyticsEnabled)
            assertTrue(
                presenter.uiState.value.pages
                    .isEmpty(),
            )
        }

    @Test
    fun `OnAnalyticsLearnMore opens the analytics wiki URL`() =
        runTest(testDispatcher) {
            val presenter = newPresenter()
            advanceUntilIdle()

            presenter.onAction(WelcomeCarouselUiAction.OnAnalyticsLearnMore)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                mainPresenter.navigateToUrlWithLauncher(BisqLinks.BISQ_MOBILE_ANALYTICS_WIKI_URL)
            }
        }

    @Test
    fun `OnAnalyticsLearnMore does not change analyticsPromptSeen`() =
        runTest(testDispatcher) {
            // Tapping "Learn more" navigates to the wiki; it must NOT count as
            // engagement that dismisses the carousel — the user still has to
            // either Enable or Don't ask again.
            settingsFlow.value =
                Settings(
                    notificationPermissionState = PermissionState.GRANTED,
                    batteryOptimizationState = BatteryOptimizationState.IGNORED,
                    analyticsPromptSeen = false,
                )
            val presenter = newPresenter()
            advanceUntilIdle()

            presenter.onAction(WelcomeCarouselUiAction.OnAnalyticsLearnMore)
            advanceUntilIdle()

            assertFalse(settingsFlow.value.analyticsPromptSeen)
            assertEquals(
                listOf(CarouselPageType.ANALYTICS),
                presenter.uiState.value.pages,
            )
        }
}
