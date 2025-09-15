package network.bisq.mobile.android.node.presentation

import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.android.node.NodeMainActivity
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.notifications.OpenTradesNotificationService
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.MainPresenter

class NodeMainPresenter(
    urlLauncher: UrlLauncher,
    openTradesNotificationService: OpenTradesNotificationService,
    connectivityService: ConnectivityService,
    settingsServiceFacade: SettingsServiceFacade,
    tradesServiceFacade: TradesServiceFacade,
    userProfileServiceFacade: UserProfileServiceFacade,
    tradeReadStateRepository: TradeReadStateRepository,
) : MainPresenter(
    connectivityService,
    openTradesNotificationService,
    settingsServiceFacade,
    tradesServiceFacade,
    userProfileServiceFacade,
    tradeReadStateRepository,
    urlLauncher
) {

    init {
        openTradesNotificationService.notificationServiceController.activityClassForIntents = NodeMainActivity::class.java
    }

    override fun isDevMode(): Boolean {
        return isDemo() || BuildNodeConfig.IS_DEBUG
    }
}