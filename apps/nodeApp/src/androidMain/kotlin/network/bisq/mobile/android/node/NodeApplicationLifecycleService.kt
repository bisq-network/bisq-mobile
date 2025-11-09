package network.bisq.mobile.android.node

import android.app.Activity
import android.content.Context
import android.os.Process
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import bisq.common.network.TransportType
import bisq.network.NetworkServiceConfig
import com.jakewharton.processphoenix.ProcessPhoenix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import network.bisq.mobile.android.node.service.AndroidMemoryReportService
import network.bisq.mobile.android.node.service.network.NodeConnectivityService
import network.bisq.mobile.domain.service.BaseService
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.explorer.ExplorerServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade
import network.bisq.mobile.domain.service.message_delivery.MessageDeliveryServiceFacade
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.domain.service.network.NetworkServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.MainActivity
import network.bisq.mobile.presentation.service.OpenTradesNotificationService
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

/**
 * Node main presenter has a very different setup than the rest of the apps (bisq2 core dependencies)
 */
class NodeApplicationLifecycleService(
    private val openTradesNotificationService: OpenTradesNotificationService,
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
    private val provider: AndroidApplicationService.Provider,
    private val androidApplicationService: AndroidApplicationService,
    private val androidMemoryReportService: AndroidMemoryReportService,
    private val kmpTorService: KmpTorService,
    private val networkServiceFacade: NetworkServiceFacade,
    private val messageDeliveryServiceFacade: MessageDeliveryServiceFacade,
    private val connectivityService: NodeConnectivityService,
) : BaseService() {
    private val alreadyKilled = AtomicBoolean(false)
    private val isRestarting = AtomicBoolean(false)


    fun initialize() {
        log.i { "Initialize core services and Tor" }

        launchIO {
            runCatching {
                androidMemoryReportService.initialize()
                networkServiceFacade.activate()
                applicationBootstrapFacade.activate()

                val networkServiceConfig: NetworkServiceConfig = androidApplicationService.networkServiceConfig
                if (isTorSupported(networkServiceConfig)) {
                    // Block until tor is started to initialize or a timeout exception is thrown
                    // We handle bootstrap timeout of tor in ApplicationBootstrapFacade
                    val torStarted = kmpTorService.startTor()
                    if (!torStarted) {
                        throw IllegalStateException("Failed to start Tor - cannot initialize application")
                    }
                }

                log.i { "Start initializing applicationService" }
                // Block until applicationService initialization is completed
                androidApplicationService.initialize().join()

                log.i { "ApplicationService initialization completed" }
                activateServiceFacades()
            }.onFailure { e ->
                log.e("Error at initializeTorAndServices", e)
                runCatching { networkServiceFacade.deactivate() }
                applicationBootstrapFacade.handleBootstrapFailure(e)
            }.also {
                // ApplicationBootstrapFacade life cycle ends here in success and failure case.
                applicationBootstrapFacade.deactivate()
            }
        }
    }

    suspend fun shutdown() {
        log.i { "Destroying NodeMainPresenter" }
        androidMemoryReportService.shutdown()
        shutdownServicesAndTor()
    }

    private suspend fun shutdownServicesAndTor() {
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
            kmpTorService.stopTor()
            log.i { "Tor stopped" }
        } catch (e: Exception) {
            log.e("Error at stopTor", e)
        }
    }

    fun restartApp(activity: Activity) {
        // One-shot guard to avoid double-triggered restarts
        if (!isRestarting.compareAndSet(false, true)) {
            log.w { "restartApp called multiple times; ignoring duplicate" }
            return
        }

        // Stop foreground notifications early to avoid flicker during restart
        runCatching { openTradesNotificationService.stopNotificationService() }

        val appContext = activity.applicationContext
        launchIO {
            try {
                // Perform shutdown off the UI thread
                shutdownServicesAndTor()
            } catch (e: Exception) {
                log.e("Error at shutdownServicesAndTor", e)
            } finally {
                // Ensure Tor is fully stopped, wait for control port to close, then purge the Tor dir
                runCatching { kmpTorService.stopAndPurgeWorkingDir() }
                    .onFailure { e -> log.w(e) { "Failed to fully stop and purge Tor before restart" } }

                // Trigger rebirth on the main thread
                withContext(Dispatchers.Main) {
                    ProcessPhoenix.triggerRebirth(appContext)
                }
            }
        }
    }


    fun restartForRestoreDataDirectory(applicationContext: Context) {
        // One-shot guard to avoid double-triggered restarts
        if (!isRestarting.compareAndSet(false, true)) {
            log.w { "restartForRestoreDataDirectory called multiple times; ignoring duplicate" }
            return
        }

        // Stop foreground notifications early to avoid flicker during restart
        runCatching { openTradesNotificationService.stopNotificationService() }

        launchIO {
            try {
                // Perform shutdown off the UI thread
                shutdownServicesAndTor()
            } catch (e: Exception) {
                log.e("Error at shutdownServicesAndTor", e)
            } finally {
                // After we have shut down the services we delete the private and settings directories.
                // Those will get restored from our backup at next startup.
                val dbDir = File(applicationContext.filesDir, "Bisq2_mobile/db")
                listOf("private", "settings").forEach { subDirName ->
                    val dir = File(dbDir, subDirName)
                    if (dir.exists()) {
                        val deleted = dir.deleteRecursively()
                        if (deleted) {
                            log.i { "Successfully deleted $subDirName directory" }
                        } else {
                            log.w { "Failed to delete $subDirName directory - restore may be incomplete" }


                        }
                    }
                }

                // Ensure Tor is fully stopped, wait for control port to close, then purge the Tor dir
                runCatching { kmpTorService.stopAndPurgeWorkingDir() }
                    .onFailure { e -> log.w(e) { "Failed to fully stop and purge Tor before restore-restart" } }

                // Trigger rebirth on the main thread
                withContext(Dispatchers.Main) {
                    ProcessPhoenix.triggerRebirth(applicationContext)
                }
            }
        }
    }

    fun terminateApp(activity: Activity) {
        (activity as? MainActivity)?.lifecycle?.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    launchIO {
                        // Add a bit of delay to give activity shutdown more time.
                        delay(200)
                        killProcess()
                    }
                }
            }
        )

        // Stop ForegroundService even it would get stopped by the onDestroy as well, but as that is not guaranteed to be executed,
        // lets stop early to avoid that its not stopped gracefully and cause warnings or restarts.
        openTradesNotificationService.stopNotificationService()

        launchIO {
            try {
                // Blocking wait until services and tor is shut down
                shutdownServicesAndTor()
            } catch (e: Exception) {
                log.e("Error at shutdownServicesAndTor", e)
            } finally {
                // Ensure all UI is finished
                withContext(Dispatchers.Main) {
                    activity.finishAffinity()
                }

                // ForegroundService is stopped by onDestroy at MainPresenter

                delay(600)
                log.w {
                    "We have called activity.finishAffinity() but our onDestroy callback was not called yet. " +
                            "We kill the process now even the activity might have still not shut down"
                }

                // In case we would not get called the DefaultLifecycleObserver.onDestroy we exit after a 1 second delay
                // We should never reach that point...

                // I case the ForegroundService was not stopped yet we try again
                openTradesNotificationService.stopNotificationService()

                delay(200)
                killProcess()
            }
        }
    }

    private fun killProcess() {
        if (alreadyKilled.compareAndSet(false, true)) {
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }
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

    private fun isTorSupported(networkServiceConfig: NetworkServiceConfig): Boolean {
        return networkServiceConfig.supportedTransportTypes.contains(TransportType.TOR)
    }
}
