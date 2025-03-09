package network.bisq.mobile.presentation.ui.uicases.startup

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.model.User
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.getDeviceLanguageCode
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

open class SplashPresenter(
    mainPresenter: MainPresenter,
    applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val userProfileService: UserProfileServiceFacade,
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val languageServiceFacade: LanguageServiceFacade,
    private val webSocketClientProvider: WebSocketClientProvider?,
) : BasePresenter(mainPresenter) {

    val state: StateFlow<String> = applicationBootstrapFacade.state
    val progress: StateFlow<Float> = applicationBootstrapFacade.progress

    private var jobs: MutableSet<Job> = mutableSetOf()

    override fun onViewAttached() {
        jobs.add(backgroundScope.launch {
            val settingsMobile: Settings = settingsRepository.fetch() ?: Settings()
            if (settingsMobile.firstLaunch) {
                val deviceLanguageCode = getDeviceLanguageCode()
                val i18nSupportedCodes = languageServiceFacade.i18nPairs.value.map { it.first }
                if (i18nSupportedCodes.contains(deviceLanguageCode)) {
                    settingsServiceFacade.setLanguageCode(deviceLanguageCode)
                } else {
                    settingsServiceFacade.setLanguageCode("en")
                }
            }

            userRepository.fetch()?.let {
                it.lastActivity = Clock.System.now().toEpochMilliseconds()
                userRepository.update(it)
            }
            progress.collect { value ->
                when {
                    value == 1.0f -> navigateToNextScreen()
                }
            }
        })

        // todo this should happen after connection to backend is configured if client mode
        // and it must not be cancelled at onViewUnattaching in case the presenter is just quickly activated
        // best its not called from a presenter but a service which checks if the url to backend is set...
        backgroundScope.launch { settingsServiceFacade.getSettings() }
    }

    override fun onViewUnattaching() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }

    private fun navigateToNextScreen() {
        uiScope.launch {
            if (!hasConnectivity()) {
                navigateToTrustedNodeSetup()
            }

            runCatching {
                val settings: SettingsVO = settingsServiceFacade.getSettings().getOrThrow()
                val settingsMobile: Settings = settingsRepository.fetch() ?: Settings()
                val user: User? = userRepository.fetch()

                if (!settings.isTacAccepted) {
                    navigateToAgreement()
                } else {
                    // only fetch profile with connectivity
                    val hasProfile: Boolean = userProfileService.hasUserProfile()

                    // Scenario 1: All good and setup for both androidNode and xClients
                    if (user != null && hasProfile) {
                        // TOOD:
                        // 1) Is this the right condition?
                        // 2a) androidNode being able to connect with other peers and
                        // 2b) xClients being able to connect with remote instance happening successfuly as part of services init?
                        navigateToHome()
                        // Scenario 2: Loading up for first time for both androidNode and xClients
                    } else if (settingsMobile.firstLaunch) {
                        navigateToOnboarding()
                        // Scenario 3: Handle others based on app type
                    } else {
                        doCustomNavigationLogic(settingsMobile, hasProfile)
                    }
                }
            }.onFailure {
                log.e(it) { "Failed to navigate out of splash" }
            }
        }
    }

    private fun navigateToTrustedNodeSetup() {
        navigateTo(Routes.TrustedNodeSetup) {
            it.popUpTo(Routes.Splash.name) { inclusive = true }
        }
    }

    private fun navigateToOnboarding() {
        navigateTo(Routes.Onboarding) {
            it.popUpTo(Routes.Splash.name) { inclusive = true }
        }
    }

    protected fun navigateToCreateProfile() {
        navigateTo(Routes.CreateProfile) {
            it.popUpTo(Routes.Splash.name) { inclusive = true }
        }
    }

    private fun navigateToHome() {
        navigateTo(Routes.TabContainer) {
            it.popUpTo(Routes.Splash.name) { inclusive = true }
        }
    }

    open suspend fun hasConnectivity(): Boolean {
        return webSocketClientProvider?.get()?.isConnected() ?: false
    }

    private fun navigateToAgreement() {
        navigateTo(Routes.Agreement) {
            it.popUpTo(Routes.Splash.name) { inclusive = true }
        }
    }

    /**
     * Default implementation in shared is for xClients. Override on node to avoid this.
     * @return true if handled, false otherwise
     */
    open fun doCustomNavigationLogic(settings: Settings, hasProfile: Boolean): Boolean {
        when {
            settings.bisqApiUrl.isEmpty() -> navigateToTrustedNodeSetup()
            settings.bisqApiUrl.isNotEmpty() && hasProfile -> navigateToCreateProfile()
            else -> navigateToHome() // TODO: Ideally this shouldn't happen here
        }
//        when {
//            settings.bisqApiUrl.isEmpty() -> {
//                navigateTo(Routes.TrustedNodeSetup.name) {
//                    popUpTo(Routes.Splash.name) { inclusive = true }
//                }
//            }
////            settings.firstLaunch -> navigateToCreateProfile()
//            else -> navigateToHome()
//        }
        return true
    }
}
