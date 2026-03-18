package network.bisq.mobile.node.tabs.dashboard

import network.bisq.mobile.data.service.ForegroundDetector
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.network.NetworkServiceFacade
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.push_notification.PushNotificationServiceFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.platform_settings.PlatformSettingsManager
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.tabs.dashboard.DashboardPresenter

class NodeDashboardPresenter(
    mainPresenter: MainPresenter,
    userProfileServiceFacade: UserProfileServiceFacade,
    marketPriceServiceFacade: MarketPriceServiceFacade,
    offersServiceFacade: OffersServiceFacade,
    settingsServiceFacade: SettingsServiceFacade,
    networkServiceFacade: NetworkServiceFacade,
    settingsRepository: SettingsRepository,
    notificationController: NotificationController,
    foregroundDetector: ForegroundDetector,
    platformSettingsManager: PlatformSettingsManager,
    pushNotificationServiceFacade: PushNotificationServiceFacade,
) : DashboardPresenter(
        mainPresenter,
        userProfileServiceFacade,
        marketPriceServiceFacade,
        offersServiceFacade,
        settingsServiceFacade,
        networkServiceFacade,
        settingsRepository,
        notificationController,
        foregroundDetector,
        platformSettingsManager,
        pushNotificationServiceFacade,
    ) {
    override val showNumConnections: Boolean = true
}
