package network.bisq.mobile.android.node.presentation

import android.app.Activity
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.android.node.MainActivity
import network.bisq.mobile.android.node.service.AndroidMemoryReportService
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.explorer.ExplorerServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.notifications.OpenTradesNotificationService
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.MainPresenter

/**
 * Node main presenter has a very different setup than the rest of the apps (bisq2 core dependencies)
 */
class NodeMainPresenter(
    urlLauncher: UrlLauncher,
    openTradesNotificationService: OpenTradesNotificationService,
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
    private val tradeReadStateRepository: TradeReadStateRepository,
    private val provider: AndroidApplicationService.Provider,
    private val androidMemoryReportService: AndroidMemoryReportService,
) : MainPresenter(connectivityService, openTradesNotificationService, settingsServiceFacade, tradesServiceFacade, tradeReadStateRepository, urlLauncher) {

    private var applicationServiceCreated = false

    init {
        openTradesNotificationService.notificationServiceController.activityClassForIntents = MainActivity::class.java
    }

    override fun onViewAttached() {
        super.onViewAttached()
        initNodeServices()
    }

    override fun isDevMode(): Boolean {
        return isDemo() || BuildNodeConfig.IS_DEBUG
    }

    private fun initNodeServices() {
        launchIO {
            runCatching {
                if (applicationServiceCreated) {
                    log.d { "Application service already created, ensuring its activated" }
                    activateServices()
                } else {
                    log.d { "Application service not created, creating.." }
                    val filesDirsPath = (view as Activity).filesDir.toPath()
                    val applicationContext = (view as Activity).applicationContext
                    val applicationService =
                        AndroidApplicationService(
                            androidMemoryReportService,
                            applicationContext,
                            filesDirsPath
                        )
                    provider.applicationService = applicationService

                    applicationBootstrapFacade.activate()
                    settingsServiceFacade.activate()
                    log.i { "Start initializing applicationService" }
                    applicationService.initialize()
                        .whenComplete { _: Boolean?, throwable: Throwable? ->
                            if (throwable == null) {
                                log.i { "ApplicationService initialization completed" }
                                applicationBootstrapFacade.deactivate()
                                activateServices(skipSettings = true)
                            } else {
                                log.e("Initializing applicationService failed", throwable)
                            }
                        }
                    applicationServiceCreated = true
                    connectivityService.startMonitoring()
                    log.d { "Application service created, monitoring connectivity.." }
                }
            }.onFailure { e ->
                // TODO give user feedback (we could have a general error screen covering usual
                //  issues like connection issues and potential solutions)
                log.e("Error at onViewAttached", e)
            }
        }
    }

    override fun onViewUnattaching() {
        launchIO {
            deactivateServices()
        }
        super.onViewUnattaching()
    }

    override fun onDestroying() {
        log.i { "Destroying NodeMainPresenter" }

        if (applicationServiceCreated) {
            try {
                log.i { "Stopping application service, ensuring persistent services stop" }
                provider.applicationService.shutdown().join()
                stopPersistentServices()
                applicationServiceCreated = false
                log.i { "Application service stopped successfully" }
            } catch (e: Exception) {
                log.e("Error stopping application service", e)
            }
        }
        
        super.onDestroying()
    }

    private fun stopPersistentServices() {
        try {
            // Explicitly stop services that might continue running
            val networkService = provider.applicationService.networkService
            networkService.shutdown().join()
            
            // Stop UserIdentityService
            provider.applicationService.identityService.shutdown().join()
            // TODO need to implement ? Stop AuthenticatedDataStorageService (part of persistence service)
//            provider.applicationService.persistenceService.shutdown().join()
            // Stop PeerExchangeService (part of network service)
            // This is already covered by networkService.shutdown() but we'll be explicit
            
            log.i { "Persistent services stopped successfully" }
        } catch (e: Exception) {
            log.e("Error stopping persistent services", e)
        }
    }

    private fun activateServices(skipSettings: Boolean = false) {
        if (!skipSettings) {
            settingsServiceFacade.activate()
        }
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
    }

    private fun deactivateServices() {
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
    }
}