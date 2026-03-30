package network.bisq.mobile.presentation.startup.splash

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.data.replicated.settings.SettingsVO
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.SettingsRepository
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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests the navigation and fallback logic in [SplashPresenter.navigateToNextScreen].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplashPresenterNavigationTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var navigationManager: NavigationManager
    private lateinit var settingsServiceFacade: SettingsServiceFacade
    private lateinit var userProfileService: UserProfileServiceFacade
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var applicationBootstrapFacade: ApplicationBootstrapFacade
    private lateinit var mainPresenter: MainPresenter
    private lateinit var versionProvider: VersionProvider

    private val stateFlow = MutableStateFlow("")
    private val progressFlow = MutableStateFlow(0f)
    private val timeoutDialogVisibleFlow = MutableStateFlow(false)
    private val bootstrapFailedFlow = MutableStateFlow(false)
    private val torBootstrapFailedFlow = MutableStateFlow(false)
    private val bootstrapStageFlow = MutableStateFlow("")
    private val progressToastFlow = MutableStateFlow(false)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        settingsServiceFacade = mockk(relaxed = true)
        userProfileService = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        applicationBootstrapFacade = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)
        versionProvider = mockk(relaxed = true)
        navigationManager = mockk(relaxed = true)

        stateFlow.value = ""
        progressFlow.value = 0f
        timeoutDialogVisibleFlow.value = false
        bootstrapFailedFlow.value = false
        torBootstrapFailedFlow.value = false
        bootstrapStageFlow.value = ""
        progressToastFlow.value = false

        every { applicationBootstrapFacade.state } returns stateFlow
        every { applicationBootstrapFacade.progress } returns progressFlow
        every { applicationBootstrapFacade.isTimeoutDialogVisible } returns timeoutDialogVisibleFlow
        every { applicationBootstrapFacade.isBootstrapFailed } returns bootstrapFailedFlow
        every { applicationBootstrapFacade.torBootstrapFailed } returns torBootstrapFailedFlow
        every { applicationBootstrapFacade.currentBootstrapStage } returns bootstrapStageFlow
        every { applicationBootstrapFacade.shouldShowProgressToast } returns progressToastFlow
        every { versionProvider.getAppNameAndVersion(any(), any()) } returns "Test 1.0"

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
        stopKoin()
        Dispatchers.resetMain()
    }

    private fun createPresenter(isIos: Boolean = false): TestSplashPresenter =
        TestSplashPresenter(
            mainPresenter = mainPresenter,
            applicationBootstrapFacade = applicationBootstrapFacade,
            userProfileService = userProfileService,
            settingsRepository = settingsRepository,
            settingsServiceFacade = settingsServiceFacade,
            versionProvider = versionProvider,
            isIos = isIos,
        )

    @Test
    fun `navigates to home when TAC accepted and has profile`() =
        runTest {
            coEvery { settingsServiceFacade.getSettings() } returns
                Result.success(SettingsVO(isTacAccepted = true))
            coEvery { settingsRepository.fetch() } returns Settings(firstLaunch = false)
            coEvery { userProfileService.hasUserProfile() } returns true

            val presenter = createPresenter()
            presenter.callNavigateToNextScreen()

            verify { navigationManager.navigate(NavRoute.TabContainer, any(), any()) }
        }

    @Test
    fun `navigates to agreement when TAC not accepted`() =
        runTest {
            coEvery { settingsServiceFacade.getSettings() } returns
                Result.success(SettingsVO(isTacAccepted = false))

            val presenter = createPresenter()
            presenter.callNavigateToNextScreen()

            verify { navigationManager.navigate(NavRoute.UserAgreement, any(), any()) }
        }

    @Test
    fun `navigates to onboarding on first launch without profile`() =
        runTest {
            coEvery { settingsServiceFacade.getSettings() } returns
                Result.success(SettingsVO(isTacAccepted = true))
            coEvery { settingsRepository.fetch() } returns Settings(firstLaunch = true)
            coEvery { userProfileService.hasUserProfile() } returns false

            val presenter = createPresenter()
            presenter.callNavigateToNextScreen()

            verify { navigationManager.navigate(NavRoute.Onboarding, any(), any()) }
        }

    @Test
    fun `navigates to create profile when not first launch and no profile`() =
        runTest {
            coEvery { settingsServiceFacade.getSettings() } returns
                Result.success(SettingsVO(isTacAccepted = true))
            coEvery { settingsRepository.fetch() } returns Settings(firstLaunch = false)
            coEvery { userProfileService.hasUserProfile() } returns false

            val presenter = createPresenter()
            presenter.callNavigateToNextScreen()

            verify { navigationManager.navigate(NavRoute.CreateProfile(true), any(), any()) }
        }

    @Test
    fun `falls back to onboarding on getSettings failure`() =
        runTest {
            coEvery { settingsServiceFacade.getSettings() } returns
                Result.failure(RuntimeException("Network error"))

            val presenter = createPresenter()
            presenter.callNavigateToNextScreen()

            verify { navigationManager.navigate(NavRoute.Onboarding, any(), any()) }
        }

    @Test
    fun `falls back to onboarding on hasUserProfile failure`() =
        runTest {
            coEvery { settingsServiceFacade.getSettings() } returns
                Result.success(SettingsVO(isTacAccepted = true))
            coEvery { settingsRepository.fetch() } returns Settings(firstLaunch = false)
            coEvery { userProfileService.hasUserProfile() } throws RuntimeException("API unavailable")

            val presenter = createPresenter()
            presenter.callNavigateToNextScreen()

            verify { navigationManager.navigate(NavRoute.Onboarding, any(), any()) }
        }

    @Test
    fun `ui state prioritizes bootstrap failure dialog over tor and timeout dialogs`() =
        runTest {
            val presenter = createPresenter()
            presenter.onViewAttached()

            stateFlow.value = "Bootstrapping"
            bootstrapStageFlow.value = "network"
            timeoutDialogVisibleFlow.value = true
            torBootstrapFailedFlow.value = true
            bootstrapFailedFlow.value = true

            val activeDialog = presenter.uiState.value.activeDialog

            assertNotNull(activeDialog)
            assertEquals(SplashActiveDialog.BootstrapFailedAndroid, activeDialog)
        }

    @Test
    fun `ui state prioritizes tor dialog over timeout dialog`() =
        runTest {
            val presenter = createPresenter()
            presenter.onViewAttached()

            bootstrapStageFlow.value = "tor"
            timeoutDialogVisibleFlow.value = true
            torBootstrapFailedFlow.value = true

            val activeDialog = presenter.uiState.value.activeDialog

            assertNotNull(activeDialog)
            assertEquals(SplashActiveDialog.TorBootstrapFailed, activeDialog)
        }

    @Test
    fun `ui state uses iOS bootstrap failed dialog when presenter is iOS`() =
        runTest {
            val presenter = createPresenter(isIos = true)
            presenter.onViewAttached()

            bootstrapFailedFlow.value = true

            val activeDialog = presenter.uiState.value.activeDialog

            assertNotNull(activeDialog)
            assertEquals(SplashActiveDialog.BootstrapFailedIos, activeDialog)
        }

    @Test
    fun `ui state uses iOS timeout dialog when presenter is iOS`() =
        runTest {
            val presenter = createPresenter(isIos = true)
            presenter.onViewAttached()

            timeoutDialogVisibleFlow.value = true

            val activeDialog = presenter.uiState.value.activeDialog

            assertNotNull(activeDialog)
            assertEquals(SplashActiveDialog.TimeoutIos, activeDialog)
        }
}

/**
 * Concrete test subclass of [SplashPresenter] that exposes [navigateToNextScreen] for testing.
 */
private class TestSplashPresenter(
    mainPresenter: MainPresenter,
    applicationBootstrapFacade: ApplicationBootstrapFacade,
    userProfileService: UserProfileServiceFacade,
    settingsRepository: SettingsRepository,
    settingsServiceFacade: SettingsServiceFacade,
    versionProvider: VersionProvider,
    isIos: Boolean,
) : SplashPresenter(
        mainPresenter,
        applicationBootstrapFacade,
        userProfileService,
        settingsRepository,
        settingsServiceFacade,
        versionProvider,
        isIos,
    ) {
    override val state: StateFlow<String> = MutableStateFlow("")

    suspend fun callNavigateToNextScreen() {
        navigateToNextScreen()
    }
}
