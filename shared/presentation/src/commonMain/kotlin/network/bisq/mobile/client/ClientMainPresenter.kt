package network.bisq.mobile.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.network.ClientConnectivityService
import network.bisq.mobile.domain.service.notifications.OpenTradesNotificationService
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.error.GenericErrorHandler

/**
 * Contains all the share code for each client. Each specific app might extend this class if needed.
 */
open class ClientMainPresenter(
    private val connectivityService: ClientConnectivityService,
    openTradesNotificationService: OpenTradesNotificationService,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val tradesServiceFacade: TradesServiceFacade,
    private val tradeChatMessagesServiceFacade: TradeChatMessagesServiceFacade,
    private val webSocketClientProvider: WebSocketClientProvider,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val offersServiceFacade: OffersServiceFacade,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val languageServiceFacade: LanguageServiceFacade,
    urlLauncher: UrlLauncher
) : MainPresenter(connectivityService, openTradesNotificationService, settingsServiceFacade, urlLauncher) {

    override fun onViewAttached() {
        super.onViewAttached()
        validateVersion()
        activateServices()
        listenForConnectivity()
    }

    override fun onViewUnattaching() {
        // For Tor we might want to leave it running while in background to avoid delay of re-connect
        // when going into foreground again.
        // coroutineScope.launch {  webSocketClient.disconnect() }
        deactivateServices()
        super.onViewUnattaching()
    }

    private fun listenForConnectivity() {
        backgroundScope.launch {
            connectivityService.startMonitoring()
            webSocketClientProvider.get().connected.collect {
                if (webSocketClientProvider.get().isConnected()) {
                    log.d { "connectivity status changed to $it - reconnecting services" }
                    reactiveServices()
                }
            }
        }
    }

    private fun validateVersion() {
        CoroutineScope(BackgroundDispatcher).launch {
            if (settingsServiceFacade.isApiCompatible()) {
                log.d { "trusted node is compatible, continue" }
            } else {
                log.w { "configured trusted node doesn't have a compatible api version" }
                val trustedNodeVersion = settingsServiceFacade.getTrustedNodeVersion()
                GenericErrorHandler.handleGenericError(
                    "Your configured trusted node is running Bisq version $trustedNodeVersion.\n" +
                            "Bisq Connect requires version ${BuildConfig.BISQ_API_VERSION} to run properly.\n"
                )
            }
        }
    }

    private fun reactiveServices() {
        deactivateServices()
        activateServices()
    }

    private fun activateServices() {
        runCatching {
            applicationBootstrapFacade.activate()
            userProfileServiceFacade.activate()
            offersServiceFacade.activate()
            marketPriceServiceFacade.activate()
            tradesServiceFacade.activate()
            tradeChatMessagesServiceFacade.activate()
            settingsServiceFacade.activate()
            languageServiceFacade.activate()
        }.onFailure { e ->
            // TODO give user feedback (we could have a general error screen covering usual
            //  issues like connection issues and potential solutions)
            log.e("Error at onViewAttached", e)
        }
    }

    private fun deactivateServices() {
        applicationBootstrapFacade.deactivate()
        userProfileServiceFacade.deactivate()
        offersServiceFacade.deactivate()
        marketPriceServiceFacade.deactivate()
        tradesServiceFacade.deactivate()
        tradeChatMessagesServiceFacade.deactivate()
        settingsServiceFacade.deactivate()
        languageServiceFacade.deactivate()
        super.onViewUnattaching()
    }

    override fun isDemo(): Boolean = ApplicationBootstrapFacade.isDemo
}