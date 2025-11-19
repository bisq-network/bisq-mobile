package network.bisq.mobile.presentation.ui.components.molecules

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.MainPresenter

/**
 * Client-specific TopBar presenter: relaxes connectivity mapping so the shine animation
 * appears even on slow (e.g., Tor) connections where status is REQUESTING_INVENTORY.
 */
class ClientTopBarPresenter(
    userProfileServiceFacade: UserProfileServiceFacade,
    settingsServiceFacade: SettingsServiceFacade,
    connectivityService: ConnectivityService,
    mainPresenter: MainPresenter,
) : TopBarPresenter(
    userProfileServiceFacade,
    settingsServiceFacade,
    connectivityService,
    mainPresenter,
) {

    private val mappedConnectivityStatus: StateFlow<ConnectivityService.ConnectivityStatus> =
        connectivityService.status
            .map { status ->
                if (status == ConnectivityService.ConnectivityStatus.REQUESTING_INVENTORY) {
                    ConnectivityService.ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED
                } else {
                    status
                }
            }
            .stateIn(
                presenterScope,
                SharingStarted.Eagerly,
                connectivityService.status.value
            )

    override val connectivityStatus: StateFlow<ConnectivityService.ConnectivityStatus>
        get() = mappedConnectivityStatus
}

