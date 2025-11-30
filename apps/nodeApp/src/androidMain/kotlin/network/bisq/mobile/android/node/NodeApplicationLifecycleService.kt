package network.bisq.mobile.android.node

import android.app.Activity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.bisq.mobile.android.node.service.AndroidMemoryReportService
import network.bisq.mobile.android.node.service.network.NodeConnectivityService
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationLifecycleService
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
import network.bisq.mobile.domain.utils.restartProcess
import network.bisq.mobile.presentation.service.OpenTradesNotificationService
import java.io.File

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
) : ApplicationLifecycleService(applicationBootstrapFacade, kmpTorService) {

    fun restartForRestoreDataDirectory(view: Any?) {
        val activity =
            view as? Activity ?: throw IllegalStateException("Passed view is not an Activity")

        val appContext = activity.applicationContext

        // One-shot guard to avoid double-triggered restarts
        if (!compareAndSetIsTerminating(expect = false, update = true)) {
            log.w { "App has already been scheduled for termination; ignoring call to restartForRestoreDataDirectory." }
            return
        }

        serviceScope.launch {
            // Cancellation should not happen at this point, so we ignore all errors and just log them
            // Till the process is killed
            try {
                // Perform shutdown off the UI thread
                deactivateServiceFacades()
            } catch (e: Throwable) {
                log.e("Error at deactivateServiceFacades", e)
            }
            try {
                withContext(Dispatchers.IO) {
                    // After we have shut down the services we delete the private and settings directories.
                    // Those will get restored from our backup at next startup.
                    val dbDir = File(appContext.filesDir, "Bisq2_mobile/db")
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
                }
            } catch (e: Exception) {
                log.e(e) { "Error when deleting bisq data" }
            } finally {
                // Ensure Tor is fully stopped, wait for control port to close, then purge the Tor dir
                try {
                    kmpTorService.stopAndPurgeWorkingDir()
                } catch (e: Throwable) {
                    log.w(e) { "Failed to fully stop and purge Tor before restore-restart" }
                }
                restartProcess(view)
            }
        }
    }

    override suspend fun activateServiceFacades() {
        androidMemoryReportService.initialize()
        applicationBootstrapFacade.activate() // sets bootstraps states and listeners
        networkServiceFacade.activate()

        log.i { "Start initializing applicationService" }
        // androidApplicationService.initialize() contains thread blocking calls
        withContext(Dispatchers.IO) {
            // Block until applicationService initialization is completed
            androidApplicationService.initialize().await()
        }
        log.i { "ApplicationService initialization completed" }

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

    override suspend fun deactivateServiceFacades() {
        // tear down notification service, since we may be terminating the app
        // and cleaning it up later makes it unnecessarily complex
        try {
            openTradesNotificationService.stopNotificationService()
        } catch (e: Exception) {
            log.w(e) { "Error at openTradesNotificationService.stopNotificationService" }
        }

        // deactivate in opposite direction of activation
        messageDeliveryServiceFacade.deactivate()
        userProfileServiceFacade.deactivate()
        reputationServiceFacade.deactivate()
        mediationServiceFacade.deactivate()
        explorerServiceFacade.deactivate()
        accountsServiceFacade.deactivate()

        languageServiceFacade.deactivate()
        tradeChatMessagesServiceFacade.deactivate()
        tradesServiceFacade.deactivate()
        marketPriceServiceFacade.deactivate()
        offersServiceFacade.deactivate()
        connectivityService.deactivate()
        settingsServiceFacade.deactivate()

        try {
            log.i { "Stopping applicationService" }
            provider.applicationService.shutdown().await()
            log.i { "ApplicationService stopped" }
        } catch (e: Exception) {
            log.e("Error at applicationService.shutdown", e)
        }

        applicationBootstrapFacade.deactivate()
        networkServiceFacade.deactivate()
    }
}
