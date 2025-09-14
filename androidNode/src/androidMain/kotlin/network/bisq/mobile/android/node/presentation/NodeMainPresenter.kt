package network.bisq.mobile.android.node.presentation

import android.app.Activity
import android.content.Intent
import android.os.Process
import bisq.common.network.TransportType
import bisq.network.NetworkServiceConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
import kotlin.system.exitProcess

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
    connectivityService: ConnectivityService,
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

    init {
        openTradesNotificationService.notificationServiceController.activityClassForIntents = MainActivity::class.java
    }

    override fun onViewAttached() {
        super.onViewAttached()

        val filesDirsPath = (view as Activity).filesDir.toPath()
        val applicationContext = (view as Activity).applicationContext
        val applicationService = AndroidApplicationService(androidMemoryReportService, applicationContext, filesDirsPath)
        provider.applicationService = applicationService

        initializeTorAndServices(applicationService)
    }

    private fun initializeTorAndServices(applicationService: AndroidApplicationService) {
        launchIO {
            runCatching {
                applicationBootstrapFacade.activate()

                if (isTorSupported(applicationService.networkServiceConfig!!)) {
                    // Block until tor is ready or a timeout exception is thrown
                    initializeTor(applicationService).await()
                }

                log.i { "Start initializing applicationService" }
                // Block until applicationService initialization is completed
                applicationService.initialize().join()

                log.i { "ApplicationService initialization completed" }
                activateServiceFacades()
            }.onFailure { e ->
                log.e("Error at initializeTorAndServices", e)
                applicationBootstrapFacade.handleBootstrapFailure(e)
            }.also {
                // ApplicationBootstrapFacade life cycle ends here in success and failure case.
                applicationBootstrapFacade.deactivate()
            }
        }
    }

    override fun onDestroying() {
        super.onDestroying()
        log.i { "Destroying NodeMainPresenter" }
        shutdownServicesAndTorAsync()
    }

    private fun shutdownServicesAndTorAsync() {
        launchIO { shutdownServicesAndTor() }
    }

    private fun shutdownServicesAndTor() {
        try {
            log.i { "Stopping service facades" }
            deactivateServiceFacades()
        } catch (e: Exception) {
            log.e("Error at deactivateServiceFacades", e)
        }

        try {
            log.i { "Stopping application service" }
            provider.applicationService.shutdown().join()
        } catch (e: Exception) {
            log.e("Error at applicationService.shutdown", e)
        }

        try {
            log.i { "Stopping Tor" }
            kmpTorService.stopTorSync()
            log.i { "Tor stopped" }
        } catch (e: Exception) {
            log.e("Error at stopTor", e)
        }
    }

    override fun isDevMode(): Boolean {
        return isDemo() || BuildNodeConfig.IS_DEBUG
    }

    fun restartApp() {
        launchIO {
            runCatching {
                try {
                    // Blocking wait until services and tor is shut down
                    shutdownServicesAndTor()
                } catch (e: Exception) {
                    log.e("Error at shutdownServicesAndTor", e)
                }
                try {
                    val activity = view as Activity
                    withContext(Dispatchers.Main) {
                        activity.finishAffinity()
                    }

                    // Create restart intent
                    val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
                    intent?.let { restartIntent ->
                        restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

                        // launch process
                        activity.startActivity(restartIntent)
                    } ?: run {
                        log.e { "Could not create restart intent" }
                    }
                } catch (e: Exception) {
                    log.e("Error at shutdownServicesAndTor", e)
                } finally {
                    Process.killProcess(Process.myPid())
                    exitProcess(0) // Guarantees JVM exit
                }
            }.onFailure { e ->
                log.e("Error at restartApp", e)
            }
        }
    }

    fun shutdownApp() {
        launchIO {
            try {
                // Blocking wait until services and tor is shut down
                shutdownServicesAndTor()
            } catch (e: Exception) {
                log.e("Error at shutdownServicesAndTor", e)
            } finally {
                // Ensure all UI is finished
                val activity = view as Activity
                withContext(Dispatchers.Main) {
                    activity.finishAffinity()
                }

                // TODO check handling of BisqForegroundService in that case

                Process.killProcess(Process.myPid())
                exitProcess(0) // Guarantees JVM exit
            }
        }
    }

    private fun initializeTor(applicationService: AndroidApplicationService): CompletableDeferred<Boolean> {
        val result = CompletableDeferred<Boolean>()
        launchIO {
            try {
                log.i { "Starting Tor" }
                val baseDir = applicationService.config.baseDir!!
                try {
                    // We block until Tor is ready, or timeout after 60 sec
                    withTimeout(60_000) { kmpTorService.startTor(baseDir).await() }
                } catch (e: TimeoutCancellationException) {
                    log.e(e) { "Tor initialization not completed after 60 seconds" }
                    result.completeExceptionally(e)
                }

                log.i { "Tor successfully started" }
                result.complete(true)
            } catch (e: Exception) {
                val failure = kmpTorService.startupFailure.value
                val errorMessage = listOfNotNull(
                    failure?.message,
                    failure?.cause?.message
                ).firstOrNull() ?: "Unknown Tor error"
                result.completeExceptionally(e)
                log.e(e) { "Tor initialization failed - $errorMessage" }
            }
        }
        return result
    }

    private fun activateServiceFacades() {
        settingsServiceFacade.activate()
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

    private fun deactivateServiceFacades() {
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