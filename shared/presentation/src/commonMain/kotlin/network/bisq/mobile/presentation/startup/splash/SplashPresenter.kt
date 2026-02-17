package network.bisq.mobile.presentation.startup.splash

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.PlatformType
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.getPlatformInfo
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter

abstract class SplashPresenter(
    mainPresenter: MainPresenter,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val userProfileService: UserProfileServiceFacade,
    private val settingsRepository: SettingsRepository,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val versionProvider: VersionProvider,
) : BasePresenter(mainPresenter) {
    companion object {
        private const val MAX_NAVIGATION_RETRIES = 3
        private const val NAVIGATION_RETRY_DELAY_MS = 1000L
    }

    abstract val state: StateFlow<String>

    val progress: StateFlow<Float> get() = applicationBootstrapFacade.progress
    val isTimeoutDialogVisible: StateFlow<Boolean> get() = applicationBootstrapFacade.isTimeoutDialogVisible
    val isBootstrapFailed: StateFlow<Boolean> get() = applicationBootstrapFacade.isBootstrapFailed

    val torBootstrapFailed: StateFlow<Boolean> get() = applicationBootstrapFacade.torBootstrapFailed
    val currentBootstrapStage: StateFlow<String> get() = applicationBootstrapFacade.currentBootstrapStage
    val shouldShowProgressToast: StateFlow<Boolean> get() = applicationBootstrapFacade.shouldShowProgressToast

    private val _appNameAndVersion: MutableStateFlow<String> = MutableStateFlow("")
    val appNameAndVersion: StateFlow<String> get() = _appNameAndVersion.asStateFlow()

    val isIos = getPlatformInfo().type == PlatformType.IOS

    override fun onViewAttached() {
        super.onViewAttached()

        presenterScope.launch {
            state.collect { value ->
                log.d { "Splash State: $value" }
            }
        }

        presenterScope.launch {
            progress.collect { value ->
                if (value >= 1.0f) {
                    navigateToNextScreen()
                }
            }
        }

        presenterScope.launch {
            shouldShowProgressToast.collect { shouldShow ->
                if (shouldShow) {
                    showSnackbar("mobile.bootstrap.progress.continuing".i18n(), isError = false)
                    applicationBootstrapFacade.setShouldShowProgressToast(false)
                }
            }
        }
        _appNameAndVersion.value = versionProvider.getAppNameAndVersion(isDemo, isIOS())
    }

    protected open suspend fun navigateToNextScreen() {
        log.d { "Navigating to next screen" }

        // TODO this logic with delay is a bad practice but couldn't find a better solution to consider all the
        //      scenarios related to changing security setups on different trusted nodes + reconnection mechanism
        //      We need to improve this in the near future.
        for (attempt in 0 until MAX_NAVIGATION_RETRIES) {
            val result =
                runCatching {
                    val profileSettings: SettingsVO = settingsServiceFacade.getSettings().getOrThrow()
                    val deviceSettings: Settings = settingsRepository.fetch()
                    if (!profileSettings.isTacAccepted) {
                        navigateToAgreement()
                    } else {
                        // only fetch profile with connectivity
                        val hasProfile: Boolean = userProfileService.hasUserProfile()
                        if (hasProfile) {
                            // Scenario 1: All good and setup for both androidNode and xClients
                            navigateToHome()
                        } else if (deviceSettings.firstLaunch) {
                            // Scenario 2: Loading up
                            // for first time for both androidNode and xClients
                            navigateToOnboarding()
                        } else {
                            // Scenario 3: Create profile
                            navigateToCreateProfile()
                        }
                    }
                }

            if (result.isSuccess) return

            val error = result.exceptionOrNull()!!

            // If our own scope is cancelled (view detached), bail immediately.
            if (error is CancellationException) {
                currentCoroutineContext().ensureActive()
            }

            // Retry on any error (network, transient CancellationException, etc.)
            if (attempt < MAX_NAVIGATION_RETRIES - 1) {
                log.w { "Navigation failed (attempt ${attempt + 1}/$MAX_NAVIGATION_RETRIES): ${error.message}" }
                delay(NAVIGATION_RETRY_DELAY_MS)
                continue
            }

            // All retries exhausted — navigate to onboarding as fallback to unblock the user
            log.e(error) { "Navigation failed after $MAX_NAVIGATION_RETRIES attempts, falling back to onboarding" }
            navigateToOnboarding()
            return
        }
    }

    private fun navigateToOnboarding() {
        navigateTo(NavRoute.Onboarding) {
            it.popUpTo(NavRoute.Splash) { inclusive = true }
        }
    }

    protected fun navigateToCreateProfile() {
        navigateTo(NavRoute.CreateProfile(true)) {
            it.popUpTo(NavRoute.Splash) { inclusive = true }
        }
    }

    protected fun navigateToHome() {
        navigateTo(NavRoute.TabContainer) {
            it.popUpTo(NavRoute.Splash) { inclusive = true }
        }
    }

    private fun navigateToAgreement() {
        log.d { "Navigating to agreement" }
        navigateTo(NavRoute.UserAgreement) {
            it.popUpTo(NavRoute.Splash) { inclusive = true }
        }
    }

    fun onTimeoutDialogContinue() {
        applicationBootstrapFacade.extendTimeout()
    }

    fun onRestartApp() {
        restartApp()
    }

    fun onRestartTor() {
        applicationBootstrapFacade.startTor(false)
    }

    fun onPurgeRestartTor() {
        applicationBootstrapFacade.startTor(true)
    }

    fun onTerminateApp() {
        terminateApp()
    }
}
