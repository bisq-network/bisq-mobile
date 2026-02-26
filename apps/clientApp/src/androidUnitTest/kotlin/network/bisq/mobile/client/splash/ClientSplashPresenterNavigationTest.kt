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
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.presentation.navigation.TrustedNodeSetup
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.network.ConnectivityService.ConnectivityStatus
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
 * Tests ClientSplashPresenter's connectivity checks and navigation logic.
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
    private lateinit var connectivityService: ConnectivityService
    private lateinit var sensitiveSettingsRepository: SensitiveSettingsRepository

    private val progressFlow = MutableStateFlow(0f)
    private val connectivityStatusFlow = MutableStateFlow(ConnectivityStatus.BOOTSTRAPPING)
    private val torBootstrapFailedFlow = MutableStateFlow(false)
    private val bootstrapFailedFlow = MutableStateFlow(false)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        settingsServiceFacade = mockk(relaxed = true)
        userProfileService = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        applicationBootstrapFacade = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)
        versionProvider = mockk(relaxed = true)
        connectivityService = mockk(relaxed = true)
        sensitiveSettingsRepository = mockk(relaxed = true)
        navigationManager = mockk(relaxed = true)

        // Default: have valid sensitive settings so tests don't auto-redirect to pairing
        coEvery { sensitiveSettingsRepository.fetch() } returns
            SensitiveSettings(
                bisqApiUrl = "http://test:8080",
                clientId = "test-client-id",
                clientSecret = "test-client-secret",
            )

        every { applicationBootstrapFacade.state } returns MutableStateFlow("")
        every { applicationBootstrapFacade.progress } returns progressFlow
        every { applicationBootstrapFacade.isTimeoutDialogVisible } returns MutableStateFlow(false)
        every { applicationBootstrapFacade.isBootstrapFailed } returns bootstrapFailedFlow
        every { applicationBootstrapFacade.torBootstrapFailed } returns torBootstrapFailedFlow
        every { applicationBootstrapFacade.currentBootstrapStage } returns MutableStateFlow("")
        every { applicationBootstrapFacade.shouldShowProgressToast } returns MutableStateFlow(false)
        every { versionProvider.getAppNameAndVersion(any(), any()) } returns "Test 1.0"

        every { connectivityService.status } returns connectivityStatusFlow

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
        torBootstrapFailedFlow.value = false
        bootstrapFailedFlow.value = false
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
            connectivityService,
            versionProvider,
            sensitiveSettingsRepository,
        )

    @Test
    fun `navigates to trusted node setup with connection failed flag when not connected`() =
        runTest(testDispatcher) {
            // Given: ConnectivityService stays in BOOTSTRAPPING (never reaches CONNECTED)
            coEvery { settingsServiceFacade.getSettings() } returns
                Result.success(SettingsVO(isTacAccepted = true))

            val presenter = createPresenter()
            presenter.onViewAttached()
            // Only run currently queued tasks (don't advance past the 30s safety net)
            testScheduler.runCurrent()

            // When: progress reaches 1.0 triggering navigateToNextScreen
            progressFlow.value = 1.0f
            // Advance past the 15s CONNECTIVITY_WAIT_TIMEOUT_MS
            advanceTimeBy(16_000)
            testScheduler.runCurrent()

            // Then: should navigate to trusted node setup with showConnectionFailed = true
            verify {
                navigationManager.navigate(
                    match { navRoute ->
                        navRoute is TrustedNodeSetup && navRoute.showConnectionFailed
                    },
                    any(),
                    any(),
                )
            }
        }

    @Test
    fun `navigates to trusted node setup immediately when Tor bootstrap fails`() =
        runTest(testDispatcher) {
            // Given: ConnectivityService stays in BOOTSTRAPPING
            val presenter = createPresenter()
            presenter.onViewAttached()
            testScheduler.runCurrent()

            // When: Tor bootstrap fails (e.g., flight mode / no internet)
            torBootstrapFailedFlow.value = true
            advanceUntilIdle()

            // Then: should navigate to trusted node setup with showConnectionFailed = true
            verify {
                navigationManager.navigate(
                    match { navRoute ->
                        navRoute is TrustedNodeSetup && navRoute.showConnectionFailed
                    },
                    any(),
                    any(),
                )
            }
        }

    @Test
    fun `navigates to trusted node setup immediately when bootstrap fails`() =
        runTest(testDispatcher) {
            // Given: ConnectivityService stays in BOOTSTRAPPING
            val presenter = createPresenter()
            presenter.onViewAttached()
            testScheduler.runCurrent()

            // When: General bootstrap fails
            bootstrapFailedFlow.value = true
            advanceUntilIdle()

            // Then: should navigate to trusted node setup with showConnectionFailed = true
            verify {
                navigationManager.navigate(
                    match { navRoute ->
                        navRoute is TrustedNodeSetup && navRoute.showConnectionFailed
                    },
                    any(),
                    any(),
                )
            }
        }

    @Test
    fun `navigates to home when connected`() =
        runTest(testDispatcher) {
            // Given: ConnectivityService reports connected with data
            connectivityStatusFlow.value = ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED

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
            // Given: Demo mode is enabled and connectivity is bootstrapping
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
            // Given: ConnectivityService stays in BOOTSTRAPPING, progress never reaches 1.0
            val presenter = createPresenter()
            presenter.onViewAttached()

            // When: CONNECTIVITY_SAFETY_NET_TIMEOUT_MS (40s) elapses
            // Use advanceUntilIdle to ensure all coroutines complete
            advanceTimeBy(45_000)
            advanceUntilIdle()

            // Then: should navigate to trusted node setup with showConnectionFailed = true
            verify {
                navigationManager.navigate(
                    match { navRoute ->
                        navRoute is TrustedNodeSetup && navRoute.showConnectionFailed
                    },
                    any(),
                    any(),
                )
            }
        }

    @Test
    fun `safety net does not trigger in demo mode`() =
        runTest(testDispatcher) {
            // Given: Demo mode is enabled, connectivity is bootstrapping
            ApplicationBootstrapFacade.isDemo = true

            val presenter = createPresenter()
            presenter.onViewAttached()

            // When: 45s elapses with advanceUntilIdle
            advanceTimeBy(45_000)
            advanceUntilIdle()

            // Then: safety net should NOT trigger (no navigation to TrustedNodeSetup)
            verify(exactly = 0) {
                navigationManager.navigate(
                    match { navRoute ->
                        navRoute is TrustedNodeSetup
                    },
                    any(),
                    any(),
                )
            }
        }

    @Test
    fun `navigates to trusted node setup WITHOUT connection failed when no saved configuration`() =
        runTest(testDispatcher) {
            // Given: No saved trusted node configuration (first-time user)
            coEvery { sensitiveSettingsRepository.fetch() } returns SensitiveSettings()

            val presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then: should navigate to trusted node setup with showConnectionFailed = false
            verify {
                navigationManager.navigate(
                    match { navRoute ->
                        navRoute is TrustedNodeSetup && !navRoute.showConnectionFailed
                    },
                    any(),
                    any(),
                )
            }
        }

    @Test
    fun `navigates to home when connectivity reaches REQUESTING_INVENTORY`() =
        runTest(testDispatcher) {
            // Given: ConnectivityService reports requesting inventory
            connectivityStatusFlow.value = ConnectivityStatus.REQUESTING_INVENTORY

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
}
