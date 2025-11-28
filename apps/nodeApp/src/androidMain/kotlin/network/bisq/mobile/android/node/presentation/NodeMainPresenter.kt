package network.bisq.mobile.android.node.presentation

import kotlinx.coroutines.launch
import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.android.node.service.network.NodeConnectivityService
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationLifecycleService
import network.bisq.mobile.domain.service.network.ConnectivityService.ConnectivityStatus
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.service.OpenTradesNotificationService

class NodeMainPresenter(
    urlLauncher: UrlLauncher,
    openTradesNotificationService: OpenTradesNotificationService,
    private val connectivityService: NodeConnectivityService,
    settingsServiceFacade: SettingsServiceFacade,
    tradesServiceFacade: TradesServiceFacade,
    userProfileServiceFacade: UserProfileServiceFacade,
    tradeReadStateRepository: TradeReadStateRepository,
    applicationLifecycleService: ApplicationLifecycleService,
) : MainPresenter(
    tradesServiceFacade,
    userProfileServiceFacade,
    openTradesNotificationService,
    settingsServiceFacade,
    tradeReadStateRepository,
    urlLauncher,
    applicationLifecycleService,
) {

    override fun onViewAttached() {
        super.onViewAttached()

        presenterScope.launch {
            connectivityService.status.collect { status ->
                _showAllConnectionsLostDialogue.value = ConnectivityStatus.DISCONNECTED == status
                _showReconnectOverlay.value = ConnectivityStatus.RECONNECTING == status
            }
        }
    }

    override fun isDevMode(): Boolean {
        return isDemo() || BuildNodeConfig.IS_DEBUG
    }
}