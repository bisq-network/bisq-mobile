package network.bisq.mobile.client.common.domain.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.presentation.common.notification.NotificationController
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
    private val settingsRepository: SettingsRepository,
    private val notificationController: NotificationController,
) : ApplicationLifecycleService(applicationBootstrapFacade, kmpTorService) {
    /**
     * Dedicated scope for the local-vs-relayed orchestration job. Kept separate
     * from the Koin-injected `serviceScope` so this class stays unit-testable
     * without bootstrapping Koin in the test fixture (the orchestration is pure
     * plumbing — no platform dependencies).
     *
     * Intentionally NOT cancelled in [deactivateServiceFacades]. The base class
     * supports `deactivate()` followed by `activate()` (e.g., the trigger-full-
     * lifecycle-restart flow). Cancelling the scope on deactivate would mean
     * the next `launchIn(pushModeScope)` attaches to a cancelled scope and
     * silently no-ops. For permanent teardown (terminateApp / restartApp) the
     * process dies and the scope goes with it — not a leak in practice. The
     * scope holds a SupervisorJob with no children between cycles, which is
     * essentially free.
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
        // Decide BEFORE the start call whether the local foreground service should run.
        maybeLaunchForegroundNotificationService()

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

        launchForegroundNotificationServiceSuppressorJob()
    }

    override suspend fun deactivateServiceFacades() {
        // Stop mirroring push opt-in to the FG service before tearing things down.
        //
        // `cancelAndJoin` (not just `cancel`) is required: `cancel()` is non-blocking
        // and only signals cancellation. The in-flight `onEach { setLocalDeliverySuppressed(...) }`
        // block runs synchronous calls into `OpenTradesNotificationService`
        // (which in turn calls `foregroundServiceController.startService()` /
        // `stopService()`). Without joining, the orchestrator can race against
        // `stopNotificationService()` below — `setLocalDeliverySuppressed(false)`
        // could fire `startService()` AFTER the controller has already been
        // disposed, leaving a stale FG service start that bypasses teardown.
        // Joining drains any in-flight emission before we proceed.
        pushModeOrchestrationJob?.cancelAndJoin()
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

    /**
     * Decides at bootstrap whether to start the local foreground notification service.
     * Two things can suppress it:
     *
     *  1. The user has opted in to relayed (FCM/APNs) notifications AND has NOT
     *     additionally enabled "keep connected in background". In the pure-relayed
     *     mode the trusted node delivers via FCM and the local FG service would only
     *     burn battery and risk a double-notification. If the user explicitly opts
     *     into the power-user combo (relayed ON + keep-connected ON), the FG service
     *     runs to keep the WebSocket alive while FCM acts as a backstop for killed
     *     app delivery.
     *  2. The OS-level POST_NOTIFICATIONS permission is denied. The FG service exists
     *     to keep the process alive so we can post trade / chat notifications when in
     *     background — without the permission those `notify(...)` calls are silently
     *     dropped. Running the service then is pure overhead with no user-visible
     *     benefit (and a persistent foreground notification users may find confusing).
     *
     * We can't lazily start-then-stop: on a cold start triggered by an FCM push
     * (relayed mode), or on a fresh install where the user hasn't granted
     * POST_NOTIFICATIONS yet, calling `startForegroundService()` and then immediately
     * stopping it can still trip `ForegroundServiceDidNotStartInTimeException` if the
     * bootstrap finishes faster than the orchestrator's first emission. The repo read
     * and the OS permission check are both cheap and `activate` is already suspend,
     * so awaiting them costs nothing observable.
     */
    private suspend fun maybeLaunchForegroundNotificationService() {
        val settings = settingsRepository.fetch()
        val pushNotificationsEnabled = settings.pushNotificationsEnabled
        val keepConnectedInBackground = settings.keepConnectedInBackground
        val notificationPermissionGranted = notificationController.hasPermission()
        if (getPlatformInfo().type == PlatformType.ANDROID) {
            // The foreground service has two independent purposes (see
            // [OpenTradesNotificationService.setKeepProcessAlive] kdoc):
            //   1. Keep the process + WS alive in background (driven by
            //      `keepConnectedInBackground`, useful even in pure-relayed mode).
            //   2. Host the local-delivery observers that post trade-state /
            //      chat notifications via `notify(...)`.
            // Suppression (purpose 2) is now separate from FG-service start
            // (purpose 1) — see [launchForegroundNotificationServiceSuppressorJob].
            val keepProcessAlive =
                notificationPermissionGranted && (!pushNotificationsEnabled || keepConnectedInBackground)
            val localDeliverySuppressed = pushNotificationsEnabled || !notificationPermissionGranted

            // Set suppression first so that if the FG service starts and the
            // app transitions straight to background, observers see the
            // correct suppressed flag at registration time.
            openTradesNotificationService.setLocalDeliverySuppressed(localDeliverySuppressed)

            if (keepProcessAlive) {
                log.i {
                    "Starting foreground notification service " +
                        "(relayed=$pushNotificationsEnabled, keepConnected=$keepConnectedInBackground, " +
                        "suppressed=$localDeliverySuppressed)"
                }
                openTradesNotificationService.setKeepProcessAlive(true)
            } else {
                log.i {
                    "Skipping foreground notification service start " +
                        "(relayed=$pushNotificationsEnabled, keepConnected=$keepConnectedInBackground, " +
                        "permission=$notificationPermissionGranted) — nothing for the FG service to do"
                }
            }
        }
    }

    /**
     * Orchestrates two independent decisions on every relevant settings/permission change:
     *
     *  1. **Foreground service** ([OpenTradesNotificationService.setKeepProcessAlive]) —
     *     should the Android FG service run? The FG service keeps the process + WS alive
     *     in background, and hosts the local-delivery observers when those are active.
     *  2. **Local notification posting** ([OpenTradesNotificationService.setLocalDeliverySuppressed]) —
     *     should the local observers post `notify(...)` calls? Suppressed when the relay
     *     handles delivery, to avoid duplicate notifications. See bisq-mobile#1450.
     *
     * Three inputs drive both decisions:
     *  - relayed-push opt-in (read from [settingsRepository] — the canonical source the
     *    push-notifications facade itself mirrors)
     *  - "keep connected in background" sub-setting (Android-only; only user-visible when
     *    relayed is on, but read unconditionally)
     *  - OS POST_NOTIFICATIONS permission (queried directly from the OS each tick)
     *
     * Truth table:
     *
     *   relayed  keep  perm |  FG service  |  local delivery
     *   -------- ----- ---- |  ----------- |  --------------
     *   off      *     yes  |  RUN         |  post (default mode)
     *   off      *     no   |  off         |  suppressed (nothing to deliver)
     *   on       off   yes  |  off         |  suppressed (pure relayed mode)
     *   on       off   no   |  off         |  suppressed
     *   on       on    yes  |  RUN         |  suppressed (FG keeps WS alive; relay posts)
     *   on       on    no   |  off         |  suppressed (no permission overrides keep)
     *
     * `keepConnected` no longer doubles notification delivery — the relay (bisq-relay
     * → FCM/APNs) has no awareness of the client's WS state and pushes for every mobile-
     * eligible event regardless. If `keepConnected=true` also armed the local observers,
     * every event would fire twice: rich local notification + generic relay notification.
     * `keepConnected` retains its WS-lifecycle role (FG service runs) but the local
     * observers stay suppressed.
     *
     * Suppression is set BEFORE keep-alive on each emission so that observers see the
     * correct flag at registration time when the FG service is being started fresh.
     *
     * Catches the runtime transitions the bootstrap gate misses:
     *  - User flips the relayed-push toggle in Settings.
     *  - User flips the keep-connected sub-toggle in Settings.
     *  - User grants/revokes POST_NOTIFICATIONS — the dashboard's `LaunchedEffect` writes
     *    the new state into `settingsRepository`, which triggers a re-evaluation here that
     *    picks up the now-current OS truth.
     */
    private fun launchForegroundNotificationServiceSuppressorJob() {
        pushModeOrchestrationJob?.cancel()
        // Single source of truth: derive both `relayed` and the orchestration state from
        // [settingsRepository.data]. The push-notifications facade also exposes an
        // `isPushNotificationsEnabled` StateFlow, but it is seeded to `false` and only
        // catches up to the persisted setting asynchronously inside its own `activate()`.
        // Combining both flows here let the orchestrator fire a transient
        // `(relayed=false, settings.relayed=true)` first emission — i.e. start the FG
        // service — followed milliseconds later by the corrected `(relayed=true, …)`
        // emission stopping it. The rapid start→stop trips Android's
        // `ForegroundServiceDidNotStartInTimeException`.
        pushModeOrchestrationJob =
            settingsRepository.data
                .map { settings ->
                    val relayed = settings.pushNotificationsEnabled
                    val keepConnected = settings.keepConnectedInBackground
                    val osGranted = notificationController.hasPermission()
                    PushOrchestrationState(
                        keepProcessAlive = osGranted && (!relayed || keepConnected),
                        localDeliverySuppressed = relayed || !osGranted,
                    )
                }.distinctUntilChanged()
                .onEach { state ->
                    openTradesNotificationService.setLocalDeliverySuppressed(state.localDeliverySuppressed)
                    openTradesNotificationService.setKeepProcessAlive(state.keepProcessAlive)
                }.launchIn(pushModeScope)
    }

    private data class PushOrchestrationState(
        val keepProcessAlive: Boolean,
        val localDeliverySuppressed: Boolean,
    )
}
