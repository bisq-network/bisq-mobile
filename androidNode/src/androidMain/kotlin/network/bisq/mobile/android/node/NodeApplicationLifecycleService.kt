package network.bisq.mobile.android.node

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Process
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import bisq.common.network.TransportType
import bisq.network.NetworkServiceConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import network.bisq.mobile.android.node.service.AndroidMemoryReportService
import network.bisq.mobile.android.node.service.network.KmpTorService
import network.bisq.mobile.android.node.service.network.NodeConnectivityService
import network.bisq.mobile.domain.service.BaseService
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.explorer.ExplorerServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade
import network.bisq.mobile.domain.service.network.NetworkServiceFacade
import network.bisq.mobile.domain.service.notifications.OpenTradesNotificationService
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.MainActivity
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
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
    private val androidMemoryReportService: AndroidMemoryReportService,
    private val kmpTorService: KmpTorService,
    private val networkServiceFacade: NetworkServiceFacade,
    private val connectivityService: NodeConnectivityService,
) : BaseService() {

    companion object Companion {
        const val TIMEOUT_SEC: Long = 60
    }

    init {
        openTradesNotificationService.notificationServiceController.activityClassForIntents = NodeMainActivity::class.java
    }

    private val alreadyKilled = AtomicBoolean(false)

    fun initialize(filesDirsPath: Path, applicationContext: Context) {
        log.i { "Initialize core services and Tor" }

        val applicationService = AndroidApplicationService(androidMemoryReportService, applicationContext, filesDirsPath)
        provider.applicationService = applicationService

        launchIO {
            runCatching {
                networkServiceFacade.activate()
                applicationBootstrapFacade.activate()

                val networkServiceConfig: NetworkServiceConfig = applicationService.networkServiceConfig
                if (isTorSupported(networkServiceConfig)) {
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
                runCatching { networkServiceFacade.deactivate() }
                applicationBootstrapFacade.handleBootstrapFailure(e)
            }.also {
                // ApplicationBootstrapFacade life cycle ends here in success and failure case.
                applicationBootstrapFacade.deactivate()
            }
        }
    }

    fun shutdown() {
        log.i { "Destroying NodeMainPresenter" }
        shutdownServicesAndTor()
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


    fun restartApp(activity: Activity) {
        launchIO {
            try {
                // Blocking wait until services and tor is shut down
                shutdownServicesAndTor()
            } catch (e: Exception) {
                log.e("Error at shutdownServicesAndTor", e)
            } finally {
                try {
                    val restartIntent = activity.packageManager
                        .getLaunchIntentForPackage(activity.packageName)
                        ?.apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                            )
                        }

                    if (restartIntent != null) {
                        withContext(Dispatchers.Main) {
                            // Start from application context so we don’t depend on dying Activity
                            activity.applicationContext.startActivity(restartIntent)
                            activity.finishAffinity()
                        }
                    } else {
                        log.e { "Could not create restart intent" }
                    }
                } catch (e: Exception) {
                    log.e("Error at shutdownServicesAndTor", e)
                } finally {
                    // Add a bit of delay to give activity shutdown and start more time.
                    delay(400)
                    killProcess()
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

                // BisqForegroundService is stopped by onDestroy at MainPresenter

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

    private fun initializeTor(applicationService: AndroidApplicationService): CompletableDeferred<Boolean> {
        val result = CompletableDeferred<Boolean>()
        launchIO {

            try {
                log.i { "Starting Tor" }
                val baseDir: Path = applicationService.config.baseDir
                // We block until Tor is ready, or timeout after 60 sec
                withTimeout(TIMEOUT_SEC * 1000) { kmpTorService.startTor(baseDir).await() }
                log.i { "Tor successfully started" }
                result.complete(true)
            } catch (e: TimeoutCancellationException) {
                log.e(e) { "Tor initialization not completed after $TIMEOUT_SEC seconds" }
                result.completeExceptionally(e)
            } catch (e: CancellationException) {
                result.cancel(e)
                throw e
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
    }

    private fun isTorSupported(networkServiceConfig: NetworkServiceConfig): Boolean {
        return networkServiceConfig.supportedTransportTypes.contains(TransportType.TOR)
    }
}