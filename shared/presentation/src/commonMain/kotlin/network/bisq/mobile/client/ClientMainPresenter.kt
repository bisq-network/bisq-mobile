package network.bisq.mobile.client

import kotlinx.coroutines.withContext
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.explorer.ExplorerServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.network_stats.ProfileStatsServiceFacade
import network.bisq.mobile.domain.service.notifications.OpenTradesNotificationService
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.error.GenericErrorHandler

/**
 * Contains all the share code for each client. Each specific app might extend this class if needed.
 */
open class ClientMainPresenter(
    private val accountsServiceFacade: AccountsServiceFacade,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val tradeChatMessagesServiceFacade: TradeChatMessagesServiceFacade,
    private val languageServiceFacade: LanguageServiceFacade,
    private val explorerServiceFacade: ExplorerServiceFacade,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val mediationServiceFacade: MediationServiceFacade,
    private val connectivityService: ConnectivityService,
    private val offersServiceFacade: OffersServiceFacade,
    private val reputationServiceFacade: ReputationServiceFacade,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val tradesServiceFacade: TradesServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    openTradesNotificationService: OpenTradesNotificationService,
    private val tradeReadStateRepository: TradeReadStateRepository,
    private val profileStatsServiceFacade: ProfileStatsServiceFacade,
    private val webSocketClientProvider: WebSocketClientProvider,
    urlLauncher: UrlLauncher
) : MainPresenter(connectivityService, openTradesNotificationService, settingsServiceFacade, tradesServiceFacade, tradeReadStateRepository, urlLauncher) {

    private var lastConnectedStatus: Boolean? = null

    override fun onViewAttached() {
        super.onViewAttached()
        validateVersion()
        activateServices()
        listenForConnectivity()
    }

    override fun onViewUnattaching() {
        deactivateServices()
        super.onViewUnattaching()
    }

    private fun listenForConnectivity() {
        connectivityService.startMonitoring()
        launchUI {
            webSocketClientProvider.get().webSocketClientStatus.collect {
                if (webSocketClientProvider.get().isConnected() && lastConnectedStatus != true) {
                    log.d { "connectivity status changed to $it - reconnecting services" }
                    reactivateServices()
                    lastConnectedStatus = true
                } else {
                    lastConnectedStatus = false
                }
            }
        }
    }

    private fun validateVersion() {
        launchUI {
            val isApiCompatible = withContext(IODispatcher) { settingsServiceFacade.isApiCompatible() }
            if (!isApiCompatible) {
                log.w { "configured trusted node doesn't have a compatible api version" }

                val trustedNodeVersion = withContext(IODispatcher) { settingsServiceFacade.getTrustedNodeVersion() }
                GenericErrorHandler.handleGenericError(
                    "Your configured trusted node is running Bisq version $trustedNodeVersion.\n" +
                            "Bisq Connect requires version ${BuildConfig.BISQ_API_VERSION} to run properly.\n"
                )
            } else {
                log.d { "trusted node is compatible, continue" }
            }
        }
    }

    override fun reactivateServices() {
        log.d { "Reactivating services" }
        deactivateServices()
        activateServices()
    }

    override fun isDevMode(): Boolean {
        return isDemo() || BuildConfig.IS_DEBUG
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

            accountsServiceFacade.activate()
            explorerServiceFacade.activate()
            mediationServiceFacade.activate()
            reputationServiceFacade.activate()

            profileStatsServiceFacade.activate()
        }.onFailure { e ->
            // Roll back any partially activated services
            runCatching { deactivateServices() }.onFailure { deactEx ->
                log.w { "Failed to rollback services after activation error: ${deactEx.message}" }
            }
            log.w { "Error activating services: ${e.message}" }
            handleInitializationError(e, "Service activation")
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

        accountsServiceFacade.deactivate()
        explorerServiceFacade.deactivate()
        mediationServiceFacade.deactivate()
        reputationServiceFacade.deactivate()

        profileStatsServiceFacade.deactivate()
    }

    override fun isDemo(): Boolean = ApplicationBootstrapFacade.isDemo
}