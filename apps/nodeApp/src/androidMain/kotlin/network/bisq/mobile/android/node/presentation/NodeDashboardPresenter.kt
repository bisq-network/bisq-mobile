package network.bisq.mobile.android.node.presentation

import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.network.NetworkServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.notification.NotificationController
import network.bisq.mobile.presentation.ui.uicases.DashboardPresenter

class NodeDashboardPresenter(
    mainPresenter: MainPresenter,
    userProfileServiceFacade: UserProfileServiceFacade,
    marketPriceServiceFacade: MarketPriceServiceFacade,
    offersServiceFacade: OffersServiceFacade,
    settingsServiceFacade: SettingsServiceFacade,
    networkServiceFacade: NetworkServiceFacade,
    settingsRepository: SettingsRepository,
    notificationController: NotificationController,
) : DashboardPresenter(
    mainPresenter,
    userProfileServiceFacade,
    marketPriceServiceFacade,
    offersServiceFacade,
    settingsServiceFacade,
    networkServiceFacade,
    settingsRepository,
    notificationController,
) {
    override val showNumConnections: Boolean = true
}