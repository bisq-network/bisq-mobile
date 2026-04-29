package network.bisq.mobile.client.common.domain.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import network.bisq.mobile.client.common.domain.access.ApiAccessService
import network.bisq.mobile.data.service.accounts.UserDefinedAccountsServiceFacade
import network.bisq.mobile.data.service.alert.AlertNotificationsServiceFacade
import network.bisq.mobile.data.service.alert.TradeRestrictingAlertServiceFacade
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.bootstrap.ApplicationLifecycleService
import network.bisq.mobile.data.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.data.service.common.LanguageServiceFacade
import network.bisq.mobile.data.service.explorer.ExplorerServiceFacade
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.mediation.MediationServiceFacade
import network.bisq.mobile.data.service.message_delivery.MessageDeliveryServiceFacade
import network.bisq.mobile.data.service.network.ConnectivityService
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.data.service.network.NetworkServiceFacade
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.push_notification.PushNotificationServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.getPlatformInfo
import network.bisq.mobile.domain.model.PlatformType
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService

class ClientApplicationLifecycleService(
    private val openTradesNotificationService: OpenTradesNotificationService,
    private val kmpTorService: KmpTorService,
    private val userDefinedAccountsServiceFacade: UserDefinedAccountsServiceFacade,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val tradeChatMessagesServiceFacade: TradeChatMessagesServiceFacade,
    private val languageServiceFacade: LanguageServiceFacade,
    private val explorerServiceFacade: ExplorerServiceFacade,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val mediationServiceFacade: MediationServiceFacade,
    private val offersServiceFacade: OffersServiceFacade,
    private val reputationServiceFacade: ReputationServiceFacade,
    private val alertNotificationsServiceFacade: AlertNotificationsServiceFacade,
    private val tradeRestrictingAlertServiceFacade: TradeRestrictingAlertServiceFacade,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val tradesServiceFacade: TradesServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val networkServiceFacade: NetworkServiceFacade,
    private val messageDeliveryServiceFacade: MessageDeliveryServiceFacade,
    private val connectivityService: ConnectivityService,
    private val apiAccessService: ApiAccessService,
    private val pushNotificationServiceFacade: PushNotificationServiceFacade,
) : ApplicationLifecycleService(applicationBootstrapFacade, kmpTorService) {
    /**
     * Dedicated scope for the local-vs-relayed orchestration job. Kept separate
     * from the Koin-injected `serviceScope` so this class stays unit-testable
     * without bootstrapping Koin in the test fixture (the orchestration is pure
     * plumbing — no platform dependencies).
     */
    private val pushModeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Orchestrates local-vs-relayed notification mode by mirroring the user's
     * push opt-in setting onto the local foreground service. Tracked here so we
     * can cancel it cleanly on deactivate (otherwise it would keep ticking
     * across app restarts).
     */
    private var pushModeOrchestrationJob: Job? = null

    override suspend fun activateServiceFacades() {
        // Start foreground service FIRST on Android, before any heavy work, to avoid
        // ForegroundServiceDidNotStartInTimeException. iOS doesn't need this.
        // If relayed notifications are enabled, the orchestrator below will stop it
        // immediately after the setting loads.
        if (getPlatformInfo().type == PlatformType.ANDROID) {
            log.i { "Starting foreground notification service" }
            openTradesNotificationService.startService()
        }

        apiAccessService.activate()
        applicationBootstrapFacade.activate() // sets bootstraps states and listeners
        networkServiceFacade.activate()
        settingsServiceFacade.activate()
        connectivityService.activate()
        offersServiceFacade.activate()
        marketPriceServiceFacade.activate()
        tradesServiceFacade.activate()
        tradeChatMessagesServiceFacade.activate()
        languageServiceFacade.activate()

        userDefinedAccountsServiceFacade.activate()
        explorerServiceFacade.activate()
        mediationServiceFacade.activate()
        reputationServiceFacade.activate()
        alertNotificationsServiceFacade.activate()
        tradeRestrictingAlertServiceFacade.activate()
        userProfileServiceFacade.activate()
        messageDeliveryServiceFacade.activate()

        // Activate push notification service - will auto-register if user has granted permission
        pushNotificationServiceFacade.activate()

        // Mirror the push-opt-in setting onto the local foreground service so that
        // exactly one delivery path runs at a time:
        //   - opt-in ON  → relayed (FCM/APNs); the local FG service stops.
        //   - opt-in OFF → local FG service runs; no relay.
        // The first emission lands shortly after `activate()` loads the persisted
        // setting, which is why we accept a brief overlap when the FG service was
        // just unconditionally started above.
        pushModeOrchestrationJob?.cancel()
        pushModeOrchestrationJob =
            pushNotificationServiceFacade.isPushNotificationsEnabled
                .onEach { enabled ->
                    openTradesNotificationService.setRelayedNotificationsEnabled(enabled)
                }.launchIn(pushModeScope)
    }

    override suspend fun deactivateServiceFacades() {
        // Stop mirroring push opt-in to the FG service before tearing things down,
        // otherwise the orchestrator could re-issue start/stop calls during teardown.
        pushModeOrchestrationJob?.cancel()
        pushModeOrchestrationJob = null

        // Tear down notification service on Android
        if (getPlatformInfo().type == PlatformType.ANDROID) {
            try {
                openTradesNotificationService.stopNotificationService()
            } catch (e: Exception) {
                log.w(e) { "Error at openTradesNotificationService.stopNotificationService" }
            }
        }

        // deactivation should happen in the opposite direction of activation
        pushNotificationServiceFacade.deactivate()
        messageDeliveryServiceFacade.deactivate()
        userProfileServiceFacade.deactivate()
        tradeRestrictingAlertServiceFacade.deactivate()
        alertNotificationsServiceFacade.deactivate()
        reputationServiceFacade.deactivate()
        mediationServiceFacade.deactivate()
        explorerServiceFacade.deactivate()
        userDefinedAccountsServiceFacade.deactivate()

        languageServiceFacade.deactivate()
        tradeChatMessagesServiceFacade.deactivate()
        tradesServiceFacade.deactivate()
        marketPriceServiceFacade.deactivate()
        offersServiceFacade.deactivate()
        connectivityService.deactivate()
        settingsServiceFacade.deactivate()

        networkServiceFacade.deactivate()
        applicationBootstrapFacade.deactivate()
        apiAccessService.deactivate()
    }
}
