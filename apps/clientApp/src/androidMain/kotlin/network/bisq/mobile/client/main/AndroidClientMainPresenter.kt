package network.bisq.mobile.client.main

import network.bisq.mobile.client.common.domain.service.network.ClientConnectivityService
import network.bisq.mobile.data.service.bootstrap.ApplicationLifecycleService
import network.bisq.mobile.data.service.network.NetworkServiceFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.UrlLauncher
import network.bisq.mobile.domain.repository.TradeReadStateRepository
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService

/**
 * Redefinition to be able to access activity for trading notifications click handling
 */
class AndroidClientMainPresenter(
    connectivityService: ClientConnectivityService,
    networkServiceFacade: NetworkServiceFacade,
    settingsServiceFacade: SettingsServiceFacade,
    tradesServiceFacade: TradesServiceFacade,
    userProfileServiceFacade: UserProfileServiceFacade,
    tradeReadStateRepository: TradeReadStateRepository,
    openTradesNotificationService: OpenTradesNotificationService,
    applicationLifecycleService: ApplicationLifecycleService,
    urlLauncher: UrlLauncher,
) : ClientMainPresenter(
        connectivityService,
        networkServiceFacade,
        settingsServiceFacade,
        tradesServiceFacade,
        userProfileServiceFacade,
        openTradesNotificationService,
        tradeReadStateRepository,
        applicationLifecycleService,
        urlLauncher,
    )
