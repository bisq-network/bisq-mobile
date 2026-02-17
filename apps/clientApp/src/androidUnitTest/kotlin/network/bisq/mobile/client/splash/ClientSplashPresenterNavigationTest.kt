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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.common.domain.websocket.ConnectionState
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.main.MainPresenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests ClientSplashPresenter's WebSocket connectivity checks and navigation logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClientSplashPresenterNavigationTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var navigationManager: NavigationManager
    private lateinit var settingsServiceFacade: SettingsServiceFacade
    private lateinit var userProfileService: UserProfileServiceFacade
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var applicationBootstrapFacade: ApplicationBootstrapFacade
    private lateinit var mainPresenter: MainPresenter
    private lateinit var versionProvider: VersionProvider
    private lateinit var webSocketClientService: WebSocketClientService

    private val progressFlow = MutableStateFlow(0f)
    private val connectionStateFlow = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        settingsServiceFacade = mockk(relaxed = true)
        userProfileService = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        applicationBootstrapFacade = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)
        versionProvider = mockk(relaxed = true)
        webSocketClientService = mockk(relaxed = true)
        navigationManager = mockk(relaxed = true)

        every { applicationBootstrapFacade.state } returns MutableStateFlow("")
        every { applicationBootstrapFacade.progress } returns progressFlow
        every { applicationBootstrapFacade.isTimeoutDialogVisible } returns MutableStateFlow(false)
        every { applicationBootstrapFacade.isBootstrapFailed } returns MutableStateFlow(false)
        every { applicationBootstrapFacade.torBootstrapFailed } returns MutableStateFlow(false)
        every { applicationBootstrapFacade.currentBootstrapStage } returns MutableStateFlow("")
        every { applicationBootstrapFacade.shouldShowProgressToast } returns MutableStateFlow(false)
        every { versionProvider.getAppNameAndVersion(any(), any()) } returns "Test 1.0"

        every { webSocketClientService.connectionState } returns connectionStateFlow
        every { webSocketClientService.isConnected() } answers { connectionStateFlow.value is ConnectionState.Connected }

        ApplicationBootstrapFacade.isDemo = false

        startKoin {
            modules(
                module {
                    single { CoroutineExceptionHandlerSetup() }
                    factory<CoroutineJobsManager> {
                        DefaultCoroutineJobsManager().apply {
                            get<CoroutineExceptionHandlerSetup>().setupExceptionHandler(this)
                        }
                    }
                    single<NavigationManager> { navigationManager }
                },
            )
        }
    }

    @AfterTest
    fun tearDown() {
        ApplicationBootstrapFacade.isDemo = false
        stopKoin()
        Dispatchers.resetMain()
    }

    private fun createPresenter(): ClientSplashPresenter =
        ClientSplashPresenter(
            mainPresenter,
            userProfileService,
            applicationBootstrapFacade,
            settingsRepository,
            settingsServiceFacade,
            webSocketClientService,
            versionProvider,
        )

    @Test
    fun `navigates to trusted node setup when not connected and reconnect times out`() =
        runTest(testDispatcher) {
            // Given: WebSocket is disconnected and stays disconnected
            coEvery { settingsServiceFacade.getSettings() } returns
                Result.success(SettingsVO(isTacAccepted = true))

            val presenter = createPresenter()
            presenter.onViewAttached()
            // Only run currently queued tasks (don't advance past the 20s safety net)
            testScheduler.runCurrent()

            // When: progress reaches 1.0 triggering navigateToNextScreen
            progressFlow.value = 1.0f
            // Advance past the 5s CONNECTION_SETTLE_TIMEOUT_MS
            advanceTimeBy(6_000)
            testScheduler.runCurrent()

            // Then: should navigate to trusted node setup
            verify {
                navigationManager.navigate(
                    match { it.toString().contains("TrustedNodeSetup") },
                    any(),
                    any(),
                )
            }
        }

    @Test
    fun `navigates to home when connected`() =
        runTest(testDispatcher) {
            // Given: WebSocket is connected
            connectionStateFlow.value = ConnectionState.Connected

            coEvery { settingsServiceFacade.getSettings() } returns
                Result.success(SettingsVO(isTacAccepted = true))
            coEvery { settingsRepository.fetch() } returns Settings(firstLaunch = false)
            coEvery { userProfileService.hasUserProfile() } returns true

            val presenter = createPresenter()
            presenter.onViewAttached()
            testScheduler.runCurrent()

            // When: progress reaches 1.0
            progressFlow.value = 1.0f
            advanceUntilIdle()

            // Then: should navigate to TabContainer (home)
            verify { navigationManager.navigate(NavRoute.TabContainer, any(), any()) }
        }

    @Test
    fun `demo mode skips connectivity check`() =
        runTest(testDispatcher) {
            // Given: Demo mode is enabled and WebSocket is disconnected
            ApplicationBootstrapFacade.isDemo = true

            coEvery { settingsServiceFacade.getSettings() } returns
                Result.success(SettingsVO(isTacAccepted = true))
            coEvery { settingsRepository.fetch() } returns Settings(firstLaunch = false)
            coEvery { userProfileService.hasUserProfile() } returns true

            val presenter = createPresenter()
            presenter.onViewAttached()
            testScheduler.runCurrent()

            // When: progress reaches 1.0
            progressFlow.value = 1.0f
            advanceUntilIdle()

            // Then: should navigate to home despite no connection
            verify { navigationManager.navigate(NavRoute.TabContainer, any(), any()) }
        }

    @Test
    fun `safety net triggers after timeout when not connected`() =
        runTest(testDispatcher) {
            // Given: WebSocket stays disconnected, progress never reaches 1.0
            val presenter = createPresenter()
            presenter.onViewAttached()

            // When: CONNECTIVITY_SAFETY_NET_TIMEOUT_MS (20s) elapses
            advanceTimeBy(21_000)
            testScheduler.runCurrent()

            // Then: should navigate to trusted node setup
            verify {
                navigationManager.navigate(
                    match { it.toString().contains("TrustedNodeSetup") },
                    any(),
                    any(),
                )
            }
        }

    @Test
    fun `safety net does not trigger in demo mode`() =
        runTest(testDispatcher) {
            // Given: Demo mode is enabled, WebSocket is disconnected
            ApplicationBootstrapFacade.isDemo = true

            val presenter = createPresenter()
            presenter.onViewAttached()

            // When: 20s elapses
            advanceTimeBy(21_000)
            testScheduler.runCurrent()

            // Then: safety net should NOT trigger (no navigation to TrustedNodeSetup)
            verify(exactly = 0) {
                navigationManager.navigate(
                    match { it.toString().contains("TrustedNodeSetup") },
                    any(),
                    any(),
                )
            }
        }
}
