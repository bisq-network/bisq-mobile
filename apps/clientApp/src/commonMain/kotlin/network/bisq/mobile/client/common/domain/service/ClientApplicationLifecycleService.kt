package network.bisq.mobile.client.common.domain.service

import network.bisq.mobile.domain.PlatformType
import network.bisq.mobile.domain.getPlatformInfo
import network.bisq.mobile.domain.service.accounts.FiatAccountsServiceFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationLifecycleService
import network.bisq.mobile.domain.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.explorer.ExplorerServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade
import network.bisq.mobile.domain.service.message_delivery.MessageDeliveryServiceFacade
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.domain.service.network.NetworkServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService

class ClientApplicationLifecycleService(
    private val openTradesNotificationService: OpenTradesNotificationService,
    private val kmpTorService: KmpTorService,
    private val fiatAccountsServiceFacade: FiatAccountsServiceFacade,
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
) : ApplicationLifecycleService(applicationBootstrapFacade, kmpTorService) {
    override suspend fun activateServiceFacades() {
        // Start foreground service FIRST on Android, before any heavy work, to avoid
        // ForegroundServiceDidNotStartInTimeException. iOS doesn't need this.
        if (getPlatformInfo().type == PlatformType.ANDROID) {
            log.i { "Starting foreground notification service" }
            openTradesNotificationService.startService()
        }

        applicationBootstrapFacade.activate() // sets bootstraps states and listeners
        networkServiceFacade.activate()
        settingsServiceFacade.activate()
        connectivityService.activate()
        offersServiceFacade.activate()
        marketPriceServiceFacade.activate()
        tradesServiceFacade.activate()
        tradeChatMessagesServiceFacade.activate()
        languageServiceFacade.activate()

        fiatAccountsServiceFacade.activate()
        explorerServiceFacade.activate()
        mediationServiceFacade.activate()
        reputationServiceFacade.activate()
        userProfileServiceFacade.activate()
        messageDeliveryServiceFacade.activate()
    }

    override suspend fun deactivateServiceFacades() {
        // Tear down notification service on Android
        if (getPlatformInfo().type == PlatformType.ANDROID) {
            try {
                openTradesNotificationService.stopNotificationService()
            } catch (e: Exception) {
                log.w(e) { "Error at openTradesNotificationService.stopNotificationService" }
            }
        }

        // deactivation should happen in the opposite direction of activation
        messageDeliveryServiceFacade.deactivate()
        userProfileServiceFacade.deactivate()
        reputationServiceFacade.deactivate()
        mediationServiceFacade.deactivate()
        explorerServiceFacade.deactivate()
        fiatAccountsServiceFacade.deactivate()

        languageServiceFacade.deactivate()
        tradeChatMessagesServiceFacade.deactivate()
        tradesServiceFacade.deactivate()
        marketPriceServiceFacade.deactivate()
        offersServiceFacade.deactivate()
        connectivityService.deactivate()
        settingsServiceFacade.deactivate()

        networkServiceFacade.deactivate()
        applicationBootstrapFacade.deactivate()
    }
}
