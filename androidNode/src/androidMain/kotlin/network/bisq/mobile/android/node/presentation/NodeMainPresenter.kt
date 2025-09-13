package network.bisq.mobile.android.node.presentation

import android.app.Activity
import bisq.common.network.TransportType
import bisq.network.NetworkServiceConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.android.node.MainActivity
import network.bisq.mobile.android.node.service.AndroidMemoryReportService
import network.bisq.mobile.android.node.service.network.KmpTorService
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
    userProfileServiceFacade: UserProfileServiceFacade,
    tradeReadStateRepository: TradeReadStateRepository,
    private val provider: AndroidApplicationService.Provider,
    private val androidMemoryReportService: AndroidMemoryReportService,
    private val kmpTorService: KmpTorService
) : MainPresenter(
    connectivityService,
    openTradesNotificationService,
    settingsServiceFacade,
    tradesServiceFacade,
    userProfileServiceFacade,
    tradeReadStateRepository,
    urlLauncher
) {

    private var applicationServiceCreated = false

    init {
        openTradesNotificationService.notificationServiceController.activityClassForIntents = MainActivity::class.java
    }

    override fun onViewAttached() {
        super.onViewAttached()
        initNodeServices()
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

                    if (isTorSupported(applicationService.networkServiceConfig!!)) {
                        initializeTor(applicationService).await()
                    }

                    // Wait for Tor to be ready before proceeding (no-op for CLEARNET)
                    //applicationBootstrapFacade.waitForTor()

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
                                applicationBootstrapFacade.deactivate()
                                handleInitializationError(throwable, "Application service initialization")
                            }
                        }
                    applicationServiceCreated = true
                    connectivityService.startMonitoring()
                    log.d { "Application service created, monitoring connectivity.." }
                }
            }.onFailure { e ->
                log.e("Error at onViewAttached", e)
                applicationBootstrapFacade.deactivate()
                handleInitializationError(e, "Node initialization")
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

                runBlocking { kmpTorService.stopTor().await() }

                applicationServiceCreated = false
                log.i { "Application service stopped successfully" }
            } catch (e: Exception) {
                log.e("Error stopping application service", e)
            }
        }

        super.onDestroying()
    }

    override fun isDevMode(): Boolean {
        return isDemo() || BuildNodeConfig.IS_DEBUG
    }

    fun restartApp() {
        try {
            val activity = view as Activity
            val packageManager = activity.packageManager
            val packageName = activity.packageName

            // Create restart intent
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.let { restartIntent ->
                restartIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                restartIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                restartIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)

                // launch process
                activity.startActivity(restartIntent)
                // now suicide
                android.os.Process.killProcess(android.os.Process.myPid())
//                kotlin.system.exitProcess(0)
            } ?: run {
                log.e { "Could not create restart intent" }
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to restart app" }
        }
    }

    private fun initializeTor(applicationService: AndroidApplicationService): CompletableDeferred<Boolean> {
        val result = CompletableDeferred<Boolean>()
        launchIO {
            try {
                log.i { "Starting Tor" }
                val baseDir = applicationService.config.baseDir!!
                // This blocks until Tor is ready
                kmpTorService.startTor(baseDir).await()
                log.i { "Tor successfully started" }
                result.complete(true)
            } catch (e: Exception) {
                val failure = kmpTorService.startupFailure.value
                val errorMessage = listOfNotNull(
                    failure?.message,
                    failure?.cause?.message
                ).firstOrNull() ?: "Unknown Tor error"
                result.complete(false)
                log.e(e) { "Tor initialization failed - $errorMessage" }
            }
        }
        return result
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

    private fun isTorSupported(networkServiceConfig: NetworkServiceConfig): Boolean {
        return networkServiceConfig.supportedTransportTypes.contains(TransportType.TOR)
    }
}