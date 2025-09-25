package network.bisq.mobile.android.node.presentation

import android.app.Activity
import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.android.node.NodeApplicationLifecycleService
import network.bisq.mobile.android.node.service.network.NodeConnectivityService
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.network.ConnectivityService.ConnectivityStatus
import network.bisq.mobile.domain.service.notifications.OpenTradesNotificationService
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.MainPresenter

class NodeMainPresenter(
    urlLauncher: UrlLauncher,
    openTradesNotificationService: OpenTradesNotificationService,
    private val connectivityService: NodeConnectivityService,
    settingsServiceFacade: SettingsServiceFacade,
    tradesServiceFacade: TradesServiceFacade,
    userProfileServiceFacade: UserProfileServiceFacade,
    tradeReadStateRepository: TradeReadStateRepository,
    private val nodeApplicationLifecycleService: NodeApplicationLifecycleService
) : MainPresenter(
    tradesServiceFacade,
    userProfileServiceFacade,
    openTradesNotificationService,
    settingsServiceFacade,
    tradeReadStateRepository,
    urlLauncher
) {

    override fun onViewAttached() {
        super.onViewAttached()

        collectUI(connectivityService.status) { status ->
            _showAllConnectionsLostDialogue.value = ConnectivityStatus.DISCONNECTED == status
        }
    }

    override fun isDevMode(): Boolean {
        return isDemo() || BuildNodeConfig.IS_DEBUG
    }

    override fun onRestartApp() {
        val activity = view as? Activity
        if (activity == null) {
            log.e { "onRestartApp: view is not an Activity" }
            return
        }
        nodeApplicationLifecycleService.restartApp(activity)
    }

    override fun onTerminateApp() {
        val activity = view as? Activity
        if (activity == null) {
            log.e { "onTerminateApp: view is not an Activity" }
            return
        }
        nodeApplicationLifecycleService.terminateApp(activity)
    }
}