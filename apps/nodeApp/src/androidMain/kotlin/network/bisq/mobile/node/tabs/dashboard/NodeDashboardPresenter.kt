package network.bisq.mobile.node.tabs.dashboard

import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.ForegroundDetector
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.network.NetworkServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.tabs.dashboard.DashboardPresenter
import network.bisq.mobile.presentation.common.platform_settings.PlatformSettingsManager

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
) {
    override val showNumConnections: Boolean = true
}