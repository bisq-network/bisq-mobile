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

        every { applicationBootstrapFacade.state } returns MutableStateFlow("")
        every { applicationBootstrapFacade.progress } returns MutableStateFlow(0f)
        every { applicationBootstrapFacade.isTimeoutDialogVisible } returns MutableStateFlow(false)
        every { applicationBootstrapFacade.isBootstrapFailed } returns MutableStateFlow(false)
        every { applicationBootstrapFacade.torBootstrapFailed } returns MutableStateFlow(false)
        every { applicationBootstrapFacade.currentBootstrapStage } returns MutableStateFlow("")
        every { applicationBootstrapFacade.shouldShowProgressToast } returns MutableStateFlow(false)
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

    private fun createPresenter(): TestSplashPresenter =
        TestSplashPresenter(
            mainPresenter = mainPresenter,
            applicationBootstrapFacade = applicationBootstrapFacade,
            userProfileService = userProfileService,
            settingsRepository = settingsRepository,
            settingsServiceFacade = settingsServiceFacade,
            versionProvider = versionProvider,
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
) : SplashPresenter(
        mainPresenter,
        applicationBootstrapFacade,
        userProfileService,
        settingsRepository,
        settingsServiceFacade,
        versionProvider,
    ) {
    override val state: StateFlow<String> = MutableStateFlow("")

    suspend fun callNavigateToNextScreen() {
        navigateToNextScreen()
    }
}
