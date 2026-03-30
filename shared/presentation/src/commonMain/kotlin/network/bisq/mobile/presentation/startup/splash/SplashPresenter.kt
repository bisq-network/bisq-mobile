package network.bisq.mobile.presentation.startup.splash

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.data.replicated.settings.SettingsVO
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.getPlatformInfo
import network.bisq.mobile.domain.model.PlatformType
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.domain.utils.combine
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
    versionProvider: VersionProvider,
    private val isIos: Boolean = getPlatformInfo().type == PlatformType.IOS,
) : BasePresenter(mainPresenter) {
    abstract val state: StateFlow<String>

    private val progress: StateFlow<Float> get() = applicationBootstrapFacade.progress
    private val isTimeoutDialogVisible: StateFlow<Boolean> get() = applicationBootstrapFacade.isTimeoutDialogVisible
    private val isBootstrapFailed: StateFlow<Boolean> get() = applicationBootstrapFacade.isBootstrapFailed
    private val torBootstrapFailed: StateFlow<Boolean> get() = applicationBootstrapFacade.torBootstrapFailed
    private val currentBootstrapStage: StateFlow<String> get() = applicationBootstrapFacade.currentBootstrapStage
    private val shouldShowProgressToast: StateFlow<Boolean> get() = applicationBootstrapFacade.shouldShowProgressToast

    private val _uiState =
        MutableStateFlow(SplashUiState(appNameAndVersion = versionProvider.getAppNameAndVersion(isDemo, isIos)))
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()

        presenterScope.launch {
            state.collect { value ->
                log.d { "Splash State: $value" }
            }
        }

        presenterScope.launch {
            combine(
                state,
                progress,
                isTimeoutDialogVisible,
                isBootstrapFailed,
                torBootstrapFailed,
                currentBootstrapStage,
            ) { status, progressValue, timeoutVisible, bootstrapFailed, torFailed, bootstrapStage ->
                _uiState.value.copy(
                    progress = progressValue,
                    status = status,
                    currentBootstrapStage = bootstrapStage,
                    activeDialog =
                        resolveActiveDialog(
                            isTimeoutDialogVisible = timeoutVisible,
                            isBootstrapFailed = bootstrapFailed,
                            torBootstrapFailed = torFailed,
                        ),
                )
            }.collect { uiState ->
                _uiState.value = uiState
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
                    showSnackbar("mobile.bootstrap.progress.continuing".i18n())
                    applicationBootstrapFacade.setShouldShowProgressToast(false)
                }
            }
        }
    }

    fun onAction(action: SplashUiAction) {
        when (action) {
            SplashUiAction.OnTimeoutDialogContinue -> onTimeoutDialogContinue()
            SplashUiAction.OnRestartApp -> onRestartApp()
            SplashUiAction.OnRestartTor -> onRestartTor()
            SplashUiAction.OnPurgeRestartTor -> onPurgeRestartTor()
            SplashUiAction.OnTerminateApp -> onTerminateApp()
        }
    }

    protected open suspend fun navigateToNextScreen() {
        log.d { "Navigating to next screen" }

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

        // Navigation failed — fall back to onboarding to unblock the user
        log.e(error) { "Navigation failed, falling back to onboarding" }
        navigateToOnboarding()
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

    private fun resolveActiveDialog(
        isTimeoutDialogVisible: Boolean,
        isBootstrapFailed: Boolean,
        torBootstrapFailed: Boolean,
    ): SplashActiveDialog? =
        when {
            isBootstrapFailed && isIos -> SplashActiveDialog.BootstrapFailedIos
            isBootstrapFailed -> SplashActiveDialog.BootstrapFailedAndroid
            torBootstrapFailed -> SplashActiveDialog.TorBootstrapFailed
            isTimeoutDialogVisible && isIos -> SplashActiveDialog.TimeoutIos
            isTimeoutDialogVisible -> SplashActiveDialog.TimeoutAndroid
            else -> null
        }

    private fun onTimeoutDialogContinue() {
        applicationBootstrapFacade.extendTimeout()
    }

    private fun onRestartApp() {
        restartApp()
    }

    private fun onRestartTor() {
        applicationBootstrapFacade.startTor(false)
    }

    private fun onPurgeRestartTor() {
        applicationBootstrapFacade.startTor(true)
    }

    private fun onTerminateApp() {
        terminateApp()
    }
}
