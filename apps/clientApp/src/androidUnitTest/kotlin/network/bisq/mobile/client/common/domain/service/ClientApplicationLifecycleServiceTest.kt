package network.bisq.mobile.client.common.domain.service

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.client.common.domain.access.ApiAccessService
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.data.service.accounts.UserDefinedAccountsServiceFacade
import network.bisq.mobile.data.service.alert.AlertNotificationsServiceFacade
import network.bisq.mobile.data.service.alert.TradeRestrictingAlertServiceFacade
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
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
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class ClientApplicationLifecycleServiceTest {
    private val order = mutableListOf<String>()

    private val openTradesNotificationService: OpenTradesNotificationService = mockk(relaxed = true)
    private val kmpTorService: KmpTorService = mockk(relaxed = true)
    private val userDefinedAccountsServiceFacade: UserDefinedAccountsServiceFacade = mockk(relaxed = true)
    private val applicationBootstrapFacade: ApplicationBootstrapFacade = mockk(relaxed = true)
    private val tradeChatMessagesServiceFacade: TradeChatMessagesServiceFacade = mockk(relaxed = true)
    private val languageServiceFacade: LanguageServiceFacade = mockk(relaxed = true)
    private val explorerServiceFacade: ExplorerServiceFacade = mockk(relaxed = true)
    private val marketPriceServiceFacade: MarketPriceServiceFacade = mockk(relaxed = true)
    private val mediationServiceFacade: MediationServiceFacade = mockk(relaxed = true)
    private val offersServiceFacade: OffersServiceFacade = mockk(relaxed = true)
    private val reputationServiceFacade: ReputationServiceFacade = mockk(relaxed = true)
    private val alertNotificationsServiceFacade: AlertNotificationsServiceFacade = mockk(relaxed = true)
    private val tradeRestrictingAlertServiceFacade: TradeRestrictingAlertServiceFacade = mockk(relaxed = true)
    private val settingsServiceFacade: SettingsServiceFacade = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)
    private val userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true)
    private val networkServiceFacade: NetworkServiceFacade = mockk(relaxed = true)
    private val messageDeliveryServiceFacade: MessageDeliveryServiceFacade = mockk(relaxed = true)
    private val connectivityService: ConnectivityService = mockk(relaxed = true)
    private val apiAccessService: ApiAccessService = mockk(relaxed = true)
    private val pushNotificationServiceFacade: PushNotificationServiceFacade = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val notificationController: NotificationController = mockk(relaxed = true)

    private lateinit var service: ClientApplicationLifecycleService

    @Before
    fun setUp() {
        configureActivationTracking()
        configureDeactivationTracking()
        // Default the persisted opt-in to false and OS notification permission
        // to granted so the activation-order test continues to assert that the
        // local foreground service starts first (the default delivery path).
        // Tests that need other paths override these themselves.
        coEvery { settingsRepository.fetch() } returns Settings(pushNotificationsEnabled = false)
        coEvery { notificationController.hasPermission() } returns true
        service =
            ClientApplicationLifecycleService(
                openTradesNotificationService = openTradesNotificationService,
                kmpTorService = kmpTorService,
                userDefinedAccountsServiceFacade = userDefinedAccountsServiceFacade,
                applicationBootstrapFacade = applicationBootstrapFacade,
                tradeChatMessagesServiceFacade = tradeChatMessagesServiceFacade,
                languageServiceFacade = languageServiceFacade,
                explorerServiceFacade = explorerServiceFacade,
                marketPriceServiceFacade = marketPriceServiceFacade,
                mediationServiceFacade = mediationServiceFacade,
                offersServiceFacade = offersServiceFacade,
                reputationServiceFacade = reputationServiceFacade,
                alertNotificationsServiceFacade = alertNotificationsServiceFacade,
                tradeRestrictingAlertServiceFacade = tradeRestrictingAlertServiceFacade,
                settingsServiceFacade = settingsServiceFacade,
                tradesServiceFacade = tradesServiceFacade,
                userProfileServiceFacade = userProfileServiceFacade,
                networkServiceFacade = networkServiceFacade,
                messageDeliveryServiceFacade = messageDeliveryServiceFacade,
                connectivityService = connectivityService,
                apiAccessService = apiAccessService,
                pushNotificationServiceFacade = pushNotificationServiceFacade,
                settingsRepository = settingsRepository,
                notificationController = notificationController,
            )
    }

    @Test
    fun `activate starts notification service first and then activates dependencies in order`() =
        runTest {
            service.activate()

            assertEquals(
                listOf(
                    "notification.start",
                    "apiAccess.activate",
                    "bootstrap.activate",
                    "network.activate",
                    "settings.activate",
                    "connectivity.activate",
                    "offers.activate",
                    "marketPrice.activate",
                    "trades.activate",
                    "tradeChat.activate",
                    "language.activate",
                    "fiat.activate",
                    "explorer.activate",
                    "mediation.activate",
                    "reputation.activate",
                    "alert.activate",
                    "tradeRestrictingAlert.activate",
                    "userProfile.activate",
                    "messageDelivery.activate",
                    "push.activate",
                ),
                order,
            )
        }

    @Test
    fun `activate skips foreground notification service start when relayed is on and keep-connected is off`() =
        runTest {
            order.clear()
            coEvery { settingsRepository.fetch() } returns
                Settings(
                    pushNotificationsEnabled = true,
                    keepConnectedInBackground = false,
                )

            service.activate()

            // Pure-relayed mode: FCM/APNs is the only delivery path. The local FG
            // service should never even briefly start (battery + risk of
            // ForegroundServiceDidNotStartInTimeException).
            assertEquals(false, order.contains("notification.start"))
            // Sanity check: the rest of the activation chain still runs.
            assertEquals("apiAccess.activate", order.first())
            assertEquals("push.activate", order.last())
        }

    @Test
    fun `activate starts FG service and suppresses local delivery in power-user combo (relayed on AND keep-connected on)`() =
        runTest {
            order.clear()
            coEvery { settingsRepository.fetch() } returns
                Settings(
                    pushNotificationsEnabled = true,
                    keepConnectedInBackground = true,
                )

            service.activate()

            // Power-user combo: FG service MUST run to keep the WS connection alive
            // in background (this is the whole point of the keep-connected toggle —
            // fast app resumes, live trade chat). The FG service is decoupled from
            // local notification posting: local delivery is SUPPRESSED because the
            // relay (FCM/APNs) handles user-facing notifications, so the FG service
            // hosts only the WS without arming the notification observers. See
            // bisq-mobile#1450.
            assertEquals(true, order.contains("notification.start"))
            assertEquals("notification.start", order.first())
            io.mockk.coVerify { openTradesNotificationService.setLocalDeliverySuppressed(true) }
        }

    @Test
    fun `activate skips foreground notification service start when notification permission is not granted`() =
        runTest {
            order.clear()
            // Default delivery mode (relayed off) BUT POST_NOTIFICATIONS not granted.
            // The FG service exists to keep the process alive so we can post local
            // notifications; without permission those `notify` calls are dropped,
            // making the service pure overhead. Bootstrap should skip starting it.
            coEvery { settingsRepository.fetch() } returns Settings(pushNotificationsEnabled = false)
            coEvery { notificationController.hasPermission() } returns false

            service.activate()

            assertEquals(false, order.contains("notification.start"))
            // Sanity check: the rest of the activation chain still runs.
            assertEquals("apiAccess.activate", order.first())
            assertEquals("push.activate", order.last())
        }

    /**
     * Verifies the runtime orchestrator job — the [map]+[onEach] pipeline that
     * mirrors `relayed × keepConnected × osPermission` into TWO independent decisions:
     *  - [OpenTradesNotificationService.setKeepProcessAlive] (FG service lifecycle)
     *  - [OpenTradesNotificationService.setLocalDeliverySuppressed] (notification posting)
     *
     * Both `relayed` and `keepConnected` are sourced from [settingsRepository.data] —
     * a single canonical Flow rather than two redundant inputs. The previous combine
     * shape used [PushNotificationServiceFacade.isPushNotificationsEnabled] as a second
     * input, but that StateFlow lags settings (it is updated asynchronously from inside
     * the facade's own `activate()` collector). The lag fired a transient
     * `(start, stop)` pair on cold start that tripped Android's
     * `ForegroundServiceDidNotStartInTimeException`. Single source = no race.
     *
     * The orchestrator runs on `pushModeScope` (Dispatchers.Default), which is not
     * controlled by `runTest`'s virtual scheduler, so we use MockK's `coVerify(timeout = …)`
     * to wait for the side-effect rather than driving virtual time.
     *
     * `distinctUntilChanged` operates on the whole [ClientApplicationLifecycleService.PushOrchestrationState]
     * data class, so when *either* component changes, BOTH method calls fire (the
     * orchestrator is idempotent per-method, so a no-op call is fine). State C is the
     * regression we fixed: relayed=true + keep=true now correctly keeps FG alive.
     */
    @Test
    fun `orchestrator mirrors truth table when relayed and keep-connected change at runtime`() =
        runTest {
            order.clear()
            val settingsFlow =
                MutableStateFlow(
                    Settings(pushNotificationsEnabled = false, keepConnectedInBackground = false),
                )
            every { settingsRepository.data } returns settingsFlow
            coEvery { notificationController.hasPermission() } returns true
            coEvery { settingsRepository.fetch() } returns
                Settings(pushNotificationsEnabled = false, keepConnectedInBackground = false)

            service.activate()

            // State A: relayed=false → FG ON, NOT suppressed (default mode, local posts).
            // Note: BOTH the bootstrap path (`maybeLaunchForegroundNotificationService`)
            // AND the orchestrator's initial emission fire with the same values on
            // activate(), so each method is called TWICE at State A. The service
            // methods are idempotent so the double-call is a no-op past the first one.
            coVerify(timeout = 2_000, exactly = 2) {
                openTradesNotificationService.setLocalDeliverySuppressed(false)
            }
            coVerify(timeout = 2_000, exactly = 2) {
                openTradesNotificationService.setKeepProcessAlive(true)
            }

            // State B: relayed=true, keep=false → FG OFF, suppressed (pure relayed mode).
            settingsFlow.value = settingsFlow.value.copy(pushNotificationsEnabled = true)
            coVerify(timeout = 2_000) { openTradesNotificationService.setLocalDeliverySuppressed(true) }
            coVerify(timeout = 2_000) { openTradesNotificationService.setKeepProcessAlive(false) }

            // State C: relayed=true, keep=true → FG ON (regression fix — was OFF before
            // bisq-mobile#1450 follow-up). Local delivery stays suppressed because the
            // relay handles user-facing notifications, but the FG service runs to keep
            // the WS alive in background for fast resume and live trade chat.
            // The orchestrator's `distinctUntilChanged` operates on the whole
            // `PushOrchestrationState` data class — when ONE component changes (keep
            // here) BOTH method calls fire, even though setLocalDeliverySuppressed(true)
            // matches state B's value. Cumulative counts from A+B+C:
            //   setLocalDeliverySuppressed(true): 2 (B + C)
            //   setKeepProcessAlive(true):        3 (2× A + C)
            settingsFlow.value = settingsFlow.value.copy(keepConnectedInBackground = true)
            coVerify(timeout = 2_000, exactly = 3) {
                openTradesNotificationService.setKeepProcessAlive(true)
            }
            coVerify(timeout = 2_000, exactly = 2) {
                openTradesNotificationService.setLocalDeliverySuppressed(true)
            }

            // State D: relayed=false, keep=true → FG ON, NOT suppressed (back to default
            // local mode; the keep-connected toggle is hidden in UI when relayed is off
            // but the stored value stays). Same FG/suppression VALUES as State A but
            // distinctUntilChanged fires because the data class differs from State C.
            // Cumulative counts after A+B+C+D:
            //   setLocalDeliverySuppressed(false): 3 (2× A + D)
            //   setKeepProcessAlive(true):        4 (2× A + C + D)
            //   setKeepProcessAlive(false):       1 (B only — sanity-check the
            //                                        "stop FG" path stayed scoped to State B)
            settingsFlow.value = settingsFlow.value.copy(pushNotificationsEnabled = false)
            coVerify(timeout = 2_000, exactly = 3) {
                openTradesNotificationService.setLocalDeliverySuppressed(false)
            }
            coVerify(timeout = 2_000, exactly = 4) {
                openTradesNotificationService.setKeepProcessAlive(true)
            }
            coVerify(timeout = 1_000, exactly = 1) {
                openTradesNotificationService.setKeepProcessAlive(false)
            }
        }

    @Test
    fun `orchestrator stops FG and suppresses local delivery when OS notification permission is revoked`() =
        runTest {
            order.clear()
            val settingsFlow =
                MutableStateFlow(
                    Settings(pushNotificationsEnabled = false, keepConnectedInBackground = false),
                )
            every { settingsRepository.data } returns settingsFlow
            // Permission denied: there's nothing useful to deliver locally; suppress AND
            // shut FG down regardless of the relayed/keep-connected combo.
            coEvery { notificationController.hasPermission() } returns false

            service.activate()

            coVerify(timeout = 2_000) { openTradesNotificationService.setLocalDeliverySuppressed(true) }
            coVerify(timeout = 2_000) { openTradesNotificationService.setKeepProcessAlive(false) }
        }

    @Test
    fun `deactivate stops notification service and deactivates dependencies in reverse order`() =
        runTest {
            service.deactivate()

            assertEquals(
                listOf(
                    "notification.stop",
                    "push.deactivate",
                    "messageDelivery.deactivate",
                    "userProfile.deactivate",
                    "tradeRestrictingAlert.deactivate",
                    "alert.deactivate",
                    "reputation.deactivate",
                    "mediation.deactivate",
                    "explorer.deactivate",
                    "fiat.deactivate",
                    "language.deactivate",
                    "tradeChat.deactivate",
                    "trades.deactivate",
                    "marketPrice.deactivate",
                    "offers.deactivate",
                    "connectivity.deactivate",
                    "settings.deactivate",
                    "network.deactivate",
                    "bootstrap.deactivate",
                    "apiAccess.deactivate",
                ),
                order,
            )
        }

    @Test
    fun `deactivate continues when notification service stop throws`() =
        runTest {
            order.clear()
            io.mockk.coEvery {
                openTradesNotificationService.stopNotificationService()
            } answers {
                order += "notification.stop"
                throw IllegalStateException("boom")
            }

            service.deactivate()

            assertEquals(
                listOf(
                    "notification.stop",
                    "push.deactivate",
                    "messageDelivery.deactivate",
                    "userProfile.deactivate",
                    "tradeRestrictingAlert.deactivate",
                    "alert.deactivate",
                    "reputation.deactivate",
                    "mediation.deactivate",
                    "explorer.deactivate",
                    "fiat.deactivate",
                    "language.deactivate",
                    "tradeChat.deactivate",
                    "trades.deactivate",
                    "marketPrice.deactivate",
                    "offers.deactivate",
                    "connectivity.deactivate",
                    "settings.deactivate",
                    "network.deactivate",
                    "bootstrap.deactivate",
                    "apiAccess.deactivate",
                ),
                order,
            )
        }

    private fun configureActivationTracking() {
        // The bootstrap path now calls `setKeepProcessAlive(true)` directly (see
        // `ClientApplicationLifecycleService.maybeLaunchForegroundNotificationService`)
        // instead of the legacy `startService()` alias. Track that call. Only the
        // `true` argument is recorded — `false` is the "skip" case and is asserted
        // via `order.contains("notification.start") == false`.
        io.mockk.every { openTradesNotificationService.setKeepProcessAlive(true) } answers { order += "notification.start" }
        io.mockk.every { openTradesNotificationService.startService() } answers { order += "notification.start" }
        coEvery { apiAccessService.activate() } answers { order += "apiAccess.activate" }
        coEvery { applicationBootstrapFacade.activate() } answers { order += "bootstrap.activate" }
        coEvery { networkServiceFacade.activate() } answers { order += "network.activate" }
        coEvery { settingsServiceFacade.activate() } answers { order += "settings.activate" }
        coEvery { connectivityService.activate() } answers { order += "connectivity.activate" }
        coEvery { offersServiceFacade.activate() } answers { order += "offers.activate" }
        coEvery { marketPriceServiceFacade.activate() } answers { order += "marketPrice.activate" }
        coEvery { tradesServiceFacade.activate() } answers { order += "trades.activate" }
        coEvery { tradeChatMessagesServiceFacade.activate() } answers { order += "tradeChat.activate" }
        coEvery { languageServiceFacade.activate() } answers { order += "language.activate" }
        coEvery { userDefinedAccountsServiceFacade.activate() } answers { order += "fiat.activate" }
        coEvery { explorerServiceFacade.activate() } answers { order += "explorer.activate" }
        coEvery { mediationServiceFacade.activate() } answers { order += "mediation.activate" }
        coEvery { reputationServiceFacade.activate() } answers { order += "reputation.activate" }
        coEvery { alertNotificationsServiceFacade.activate() } answers { order += "alert.activate" }
        coEvery { tradeRestrictingAlertServiceFacade.activate() } answers { order += "tradeRestrictingAlert.activate" }
        coEvery { userProfileServiceFacade.activate() } answers { order += "userProfile.activate" }
        coEvery { messageDeliveryServiceFacade.activate() } answers { order += "messageDelivery.activate" }
        coEvery { pushNotificationServiceFacade.activate() } answers { order += "push.activate" }
    }

    private fun configureDeactivationTracking() {
        io.mockk.coEvery { openTradesNotificationService.stopNotificationService() } answers { order += "notification.stop" }
        coEvery { pushNotificationServiceFacade.deactivate() } answers { order += "push.deactivate" }
        coEvery { messageDeliveryServiceFacade.deactivate() } answers { order += "messageDelivery.deactivate" }
        coEvery { userProfileServiceFacade.deactivate() } answers { order += "userProfile.deactivate" }
        coEvery { tradeRestrictingAlertServiceFacade.deactivate() } answers { order += "tradeRestrictingAlert.deactivate" }
        coEvery { alertNotificationsServiceFacade.deactivate() } answers { order += "alert.deactivate" }
        coEvery { reputationServiceFacade.deactivate() } answers { order += "reputation.deactivate" }
        coEvery { mediationServiceFacade.deactivate() } answers { order += "mediation.deactivate" }
        coEvery { explorerServiceFacade.deactivate() } answers { order += "explorer.deactivate" }
        coEvery { userDefinedAccountsServiceFacade.deactivate() } answers { order += "fiat.deactivate" }
        coEvery { languageServiceFacade.deactivate() } answers { order += "language.deactivate" }
        coEvery { tradeChatMessagesServiceFacade.deactivate() } answers { order += "tradeChat.deactivate" }
        coEvery { tradesServiceFacade.deactivate() } answers { order += "trades.deactivate" }
        coEvery { marketPriceServiceFacade.deactivate() } answers { order += "marketPrice.deactivate" }
        coEvery { offersServiceFacade.deactivate() } answers { order += "offers.deactivate" }
        coEvery { connectivityService.deactivate() } answers { order += "connectivity.deactivate" }
        coEvery { settingsServiceFacade.deactivate() } answers { order += "settings.deactivate" }
        coEvery { networkServiceFacade.deactivate() } answers { order += "network.deactivate" }
        coEvery { applicationBootstrapFacade.deactivate() } answers { order += "bootstrap.deactivate" }
        coEvery { apiAccessService.deactivate() } answers { order += "apiAccess.deactivate" }
    }
}
