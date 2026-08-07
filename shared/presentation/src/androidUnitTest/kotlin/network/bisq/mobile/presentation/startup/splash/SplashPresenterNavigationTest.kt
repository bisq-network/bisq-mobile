package network.bisq.mobile.presentation.startup.splash

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.data.replicated.settings.SettingsVO
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests the navigation and fallback logic in [SplashPresenter.navigateToNextScreen].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplashPresenterNavigationTest : PresentationKoinTestBase() {
    override val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    private val settingsServiceFacade: SettingsServiceFacade = mockk(relaxed = true)
    private val userProfileService: UserProfileServiceFacade = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val applicationBootstrapFacade: ApplicationBootstrapFacade = mockk(relaxed = true)
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val versionProvider: VersionProvider = mockk(relaxed = true)

    private lateinit var stateFlow: MutableStateFlow<String>
    private lateinit var progressFlow: MutableStateFlow<Float>
    private lateinit var timeoutDialogVisibleFlow: MutableStateFlow<Boolean>
    private lateinit var bootstrapFailedFlow: MutableStateFlow<Boolean>
    private lateinit var torBootstrapFailedFlow: MutableStateFlow<Boolean>
    private lateinit var bootstrapStageFlow: MutableStateFlow<String>
    private lateinit var progressToastFlow: MutableStateFlow<Boolean>

    override fun beforeStartKoin() {
        super.beforeStartKoin()
        globalUiManager = GlobalUiManager(testDispatcher)
    }

    override fun onKoinReady() {
        stateFlow = MutableStateFlow("")
        progressFlow = MutableStateFlow(0f)
        timeoutDialogVisibleFlow = MutableStateFlow(false)
        bootstrapFailedFlow = MutableStateFlow(false)
        torBootstrapFailedFlow = MutableStateFlow(false)
        bootstrapStageFlow = MutableStateFlow("")
        progressToastFlow = MutableStateFlow(false)

        every { applicationBootstrapFacade.state } returns stateFlow
        every { applicationBootstrapFacade.progress } returns progressFlow
        every { applicationBootstrapFacade.isTimeoutDialogVisible } returns timeoutDialogVisibleFlow
        every { applicationBootstrapFacade.isBootstrapFailed } returns bootstrapFailedFlow
        every { applicationBootstrapFacade.torBootstrapFailed } returns torBootstrapFailedFlow
        every { applicationBootstrapFacade.currentBootstrapStage } returns bootstrapStageFlow
        every { applicationBootstrapFacade.shouldShowProgressToast } returns progressToastFlow
        every { versionProvider.getAppNameAndVersion(any(), any()) } returns "Test 1.0"
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
