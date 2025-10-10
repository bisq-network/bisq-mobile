package network.bisq.mobile.android.node.presentation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.NetworkServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.SplashPresenter

class NodeSplashPresenter(
    mainPresenter: MainPresenter,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    userProfileService: UserProfileServiceFacade,
    settingsRepository: SettingsRepository,
    settingsServiceFacade: SettingsServiceFacade,
    networkServiceFacade: NetworkServiceFacade,
) : SplashPresenter(
    mainPresenter,
    applicationBootstrapFacade,
    userProfileService,
    settingsRepository,
    settingsServiceFacade,
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
}
