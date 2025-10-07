package network.bisq.mobile.client

import network.bisq.mobile.domain.service.BaseService
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.explorer.ExplorerServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade
import network.bisq.mobile.domain.service.message_delivery.MessageDeliveryServiceFacade
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.network.NetworkServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade

class ClientApplicationLifecycleService(
    private val accountsServiceFacade: AccountsServiceFacade,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val tradeChatMessagesServiceFacade: TradeChatMessagesServiceFacade,
    private val languageServiceFacade: LanguageServiceFacade,
    private val explorerServiceFacade: ExplorerServiceFacade,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val mediationServiceFacade: MediationServiceFacade,
    private val offersServiceFacade: OffersServiceFacade,
    private val reputationServiceFacade: ReputationServiceFacade,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val tradesServiceFacade: TradesServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val networkServiceFacade: NetworkServiceFacade,
    private val messageDeliveryServiceFacade: MessageDeliveryServiceFacade,
    private val connectivityService: ConnectivityService,
) : BaseService() {

    fun initialize() {
        log.i { "Initialize core services and Tor" }

        launchIO {
            runCatching {
                networkServiceFacade.activate()
                applicationBootstrapFacade.activate()
                activateServiceFacades()
            }.onFailure { e ->
                log.e("Error at initializeTorAndServices", e)
                runCatching { networkServiceFacade.deactivate() }
                applicationBootstrapFacade.handleBootstrapFailure(e)
            }.also {
                // TODO implement the bootstrap in applicationBootstrapFacade and networkServiceFacade
                // ApplicationBootstrapFacade life cycle ends here in success and failure case.
                //applicationBootstrapFacade.deactivate()
            }
        }
    }

    fun shutdown() {
        log.i { "Destroying NodeMainPresenter" }
        deactivateServiceFacades()
    }


    private fun activateServiceFacades() {
        settingsServiceFacade.activate()
        connectivityService.activate()
        offersServiceFacade.activate()
        marketPriceServiceFacade.activate()
        tradesServiceFacade.activate()
        tradeChatMessagesServiceFacade.activate()
        languageServiceFacade.activate()

        accountsServiceFacade.activate()
        explorerServiceFacade.activate()
        mediationServiceFacade.activate()
        reputationServiceFacade.activate()
        userProfileServiceFacade.activate()
        messageDeliveryServiceFacade.activate()
    }

    private fun deactivateServiceFacades() {
        connectivityService.deactivate()
        networkServiceFacade.deactivate()
        applicationBootstrapFacade.deactivate()
        settingsServiceFacade.deactivate()
        offersServiceFacade.deactivate()
        marketPriceServiceFacade.deactivate()
        tradesServiceFacade.deactivate()
        tradeChatMessagesServiceFacade.deactivate()
        languageServiceFacade.deactivate()

        accountsServiceFacade.deactivate()
        explorerServiceFacade.deactivate()
        mediationServiceFacade.deactivate()
        reputationServiceFacade.deactivate()
        userProfileServiceFacade.deactivate()
        messageDeliveryServiceFacade.deactivate()
    }
}
