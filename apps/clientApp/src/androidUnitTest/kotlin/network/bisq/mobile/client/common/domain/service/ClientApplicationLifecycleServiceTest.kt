package network.bisq.mobile.client.common.domain.service

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.client.common.domain.access.ApiAccessService
import network.bisq.mobile.data.service.accounts.FiatAccountsServiceFacade
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
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class ClientApplicationLifecycleServiceTest {
    private val order = mutableListOf<String>()

    private val openTradesNotificationService: OpenTradesNotificationService = mockk(relaxed = true)
    private val kmpTorService: KmpTorService = mockk(relaxed = true)
    private val fiatAccountsServiceFacade: FiatAccountsServiceFacade = mockk(relaxed = true)
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

    private lateinit var service: ClientApplicationLifecycleService

    @Before
    fun setUp() {
        configureActivationTracking()
        configureDeactivationTracking()
        service =
            ClientApplicationLifecycleService(
                openTradesNotificationService = openTradesNotificationService,
                kmpTorService = kmpTorService,
                fiatAccountsServiceFacade = fiatAccountsServiceFacade,
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
        coEvery { fiatAccountsServiceFacade.activate() } answers { order += "fiat.activate" }
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
        coEvery { fiatAccountsServiceFacade.deactivate() } answers { order += "fiat.deactivate" }
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
