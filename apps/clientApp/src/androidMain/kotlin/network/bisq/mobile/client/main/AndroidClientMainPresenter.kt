package network.bisq.mobile.client.main

import network.bisq.mobile.client.common.domain.service.network.ClientConnectivityService
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationLifecycleService
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.service.OpenTradesNotificationService

/**
 * Redefinition to be able to access activity for trading notifications click handling
 */
class AndroidClientMainPresenter(
    connectivityService: ClientConnectivityService,
    settingsServiceFacade: SettingsServiceFacade,
    tradesServiceFacade: TradesServiceFacade,
    userProfileServiceFacade: UserProfileServiceFacade,
    tradeReadStateRepository: TradeReadStateRepository,
    openTradesNotificationService: OpenTradesNotificationService,
    applicationLifecycleService: ApplicationLifecycleService,
    urlLauncher: UrlLauncher
) : ClientMainPresenter(
    connectivityService,
    settingsServiceFacade,
    tradesServiceFacade,
    userProfileServiceFacade,
    openTradesNotificationService,
    tradeReadStateRepository,
    applicationLifecycleService,
    urlLauncher
)