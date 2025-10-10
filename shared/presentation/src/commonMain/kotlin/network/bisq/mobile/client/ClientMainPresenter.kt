package network.bisq.mobile.client

import network.bisq.mobile.client.service.network.ClientConnectivityService
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.service.OpenTradesNotificationService

/**
 * Contains all the share code for each client. Each specific app might extend this class if needed.
 */
open class ClientMainPresenter(
    private val connectivityService: ClientConnectivityService,
    private val settingsServiceFacade: SettingsServiceFacade,
    tradesServiceFacade: TradesServiceFacade,
    userProfileServiceFacade: UserProfileServiceFacade,
    openTradesNotificationService: OpenTradesNotificationService,
    tradeReadStateRepository: TradeReadStateRepository,
    private val webSocketClientProvider: WebSocketClientProvider,
    urlLauncher: UrlLauncher
) : MainPresenter(
    tradesServiceFacade,
    userProfileServiceFacade,
    openTradesNotificationService,
    settingsServiceFacade,
    tradeReadStateRepository,
    urlLauncher,
) {

    override fun onViewAttached() {
        super.onViewAttached()
//        activateServices()
//        validateVersion()
        listenForConnectivity()
    }


    private fun listenForConnectivity() {
        connectivityService.startMonitoring()
    }

    override fun onResumeServices() {
        super.onResumeServices()
        connectivityService.startMonitoring()
    }

    override fun onPauseServices() {
        super.onPauseServices()
        connectivityService.stopMonitoring()
    }

    override fun isDevMode(): Boolean {
        return isDemo() || BuildConfig.IS_DEBUG
    }

    override fun isDemo(): Boolean = ApplicationBootstrapFacade.isDemo
}
