package network.bisq.mobile.android.node.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import network.bisq.mobile.android.node.NodeApplicationLifecycleService
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.network.NetworkServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.SplashPresenter

class NodeSplashPresenter(
    private val mainPresenter: MainPresenter,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    userProfileService: UserProfileServiceFacade,
    userRepository: UserRepository,
    settingsRepository: SettingsRepository,
    settingsServiceFacade: SettingsServiceFacade,
    languageServiceFacade: LanguageServiceFacade,
    networkServiceFacade: NetworkServiceFacade,
    private val nodeApplicationLifecycleService: NodeApplicationLifecycleService
) : SplashPresenter(
    mainPresenter,
    applicationBootstrapFacade,
    userProfileService,
    userRepository,
    settingsRepository,
    settingsServiceFacade,
    languageServiceFacade,
    null
) {

    private val _state = MutableStateFlow("")
    override val state: StateFlow<String> get() = _state.asStateFlow()
    val numConnections: StateFlow<Int> = networkServiceFacade.numConnections

    override fun onViewAttached() {
        super.onViewAttached()

        collectUI(
            combine(
                applicationBootstrapFacade.state,
                numConnections
            ) { state, numConnections ->
                if (numConnections >= 0) {
                    "mobile.splash.bootstrapState.stateAndNumConnections".i18n(state, numConnections)
                } else {
                    state
                }

            }
        ) { stateAndNumConnections ->
            _state.value = stateAndNumConnections
        }
    }

    override fun doCustomNavigationLogic(settings: Settings, hasProfile: Boolean): Boolean {
        navigateToCreateProfile()
        return false
    }

    override suspend fun isClientAndHasNoConnectivity(): Boolean {
        return false
    }

    // TODO: Refactor so we dont have Client related methods
    override suspend fun handleDemoModeForClient() {
        // Do nothing, only used in client mode
    }

    // TODO Would be better if all such code is extracted in a ClientSplashPresenter making such guards not needed
    override fun navigateToTrustedNodeSetup() {
        log.w { "navigateToTrustedNodeSetup called on node app. This should never happen." }
        // Ensure we never call TrustedNodeSetup in node mode
    }
}
