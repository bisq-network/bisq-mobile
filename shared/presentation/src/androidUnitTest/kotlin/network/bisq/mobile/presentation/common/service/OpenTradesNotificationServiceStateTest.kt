package network.bisq.mobile.presentation.common.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradeModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.protocol.BisqEasyTradeStateEnum
import network.bisq.mobile.data.service.ForegroundDetector
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.common.notification.ForegroundServiceController
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.notification.NotificationRedactions
import network.bisq.mobile.presentation.common.notification.model.NotificationBuilder
import network.bisq.mobile.presentation.common.notification.model.NotificationConfig
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class OpenTradesNotificationServiceStateTest : PresentationKoinTestBase() {
    private val notificationController: NotificationController = mockk(relaxed = true)
    private val foregroundServiceController: ForegroundServiceController = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)
    private val userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true)
    private val appForegroundController: ForegroundDetector = mockk(relaxed = true)

    private lateinit var service: OpenTradesNotificationService

    override fun onSetup() {
        every { tradesServiceFacade.openTradeItems } returns MutableStateFlow(emptyList())
        every { userProfileServiceFacade.ignoredProfileIds } returns MutableStateFlow(emptySet())
        every { appForegroundController.isForeground } returns MutableStateFlow(true)

        service =
            OpenTradesNotificationService(
                notificationController = notificationController,
                foregroundServiceController = foregroundServiceController,
                tradesServiceFacade = tradesServiceFacade,
                userProfileServiceFacade = userProfileServiceFacade,
                appForegroundController = appForegroundController,
            )
    }

    private fun mockTrade(
        isMaker: Boolean,
        isTaker: Boolean,
    ): TradeItemPresentationModel {
        val tradeModel = mockk<BisqEasyTradeModel>(relaxed = true)
        every { tradeModel.isMaker } returns isMaker
        every { tradeModel.isTaker } returns isTaker

        val trade = mockk<TradeItemPresentationModel>(relaxed = true)
        every { trade.bisqEasyTradeModel } returns tradeModel
        every { trade.shortTradeId } returns "abc12345"
        every { trade.tradeId } returns "abc12345-full-id"
        every { trade.peersUserName } returns "PeerUser"
        return trade
    }

    @Test
    fun takerSentTakeOfferRequest_doesNotNotifyTaker() =
        runTest {
            val trade = mockTrade(isMaker = false, isTaker = true)

            service.handleTradeStateNotification(
                trade,
                BisqEasyTradeStateEnum.TAKER_SENT_TAKE_OFFER_REQUEST,
            )

            verify(exactly = 0) { notificationController.notify(any<NotificationBuilder.() -> Unit>()) }
        }

    @Test
    fun takerSentTakeOfferRequest_notifiesMaker() =
        runTest {
            val trade = mockTrade(isMaker = true, isTaker = false)

            service.handleTradeStateNotification(
                trade,
                BisqEasyTradeStateEnum.TAKER_SENT_TAKE_OFFER_REQUEST,
            )

            verify(exactly = 1) { notificationController.notify(any<NotificationBuilder.() -> Unit>()) }
        }

    /**
     * Asserts the peer name and the policy together, on purpose. Pinning the policy alone says
     * nothing about why it is the right one, and the wrong policy was held in place for a while by a
     * test that did exactly that — "a state transition does not name a peer" read as true because
     * nobody checked it against the copy.
     */
    @Test
    fun `a trade state notification names the peer, so it is redacted on the lock screen`() =
        runTest {
            var config: NotificationConfig? = null
            every { notificationController.notify(any<NotificationBuilder.() -> Unit>()) } answers {
                config = NotificationBuilder().apply(firstArg<NotificationBuilder.() -> Unit>()).build()
            }

            service.handleTradeStateNotification(
                mockTrade(isMaker = true, isTaker = false),
                BisqEasyTradeStateEnum.TAKER_SENT_TAKE_OFFER_REQUEST,
            )

            val posted = assertNotNull(config, "the service must have posted a notification")
            assertContains(assertNotNull(posted.body, "the notification must have a body"), "PeerUser")
            assertEquals(NotificationRedactions.tradeUpdate(), posted.android?.lockScreen)
            assertEquals(NotificationRedactions.TRADE_UPDATE_CATEGORY, posted.ios?.categoryId, "iOS redacts through the category's placeholder")
        }
}
