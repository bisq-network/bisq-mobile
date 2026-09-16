package network.bisq.mobile.presentation.trade.trade_chat

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.data.model.TradeReadStateMap
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage
import network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.createMockBisqEasyOpenTradeMessage
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.user.identity.UserIdentityVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.data.service.message_delivery.MessageDeliveryServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.repository.TradeReadStateRepository
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TradeChatPresenterTest : PresentationKoinTestBase() {
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val tradesServiceFacade: TradesServiceFacade = mockk(relaxed = true)
    private val tradeChatMessagesServiceFacade: TradeChatMessagesServiceFacade = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val tradeReadStateRepository: TradeReadStateRepository = mockk(relaxed = true)
    private val userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true)
    private val notificationController: NotificationController = mockk(relaxed = true)
    private val messageDeliveryServiceFacade: MessageDeliveryServiceFacade = mockk(relaxed = true)
    private lateinit var presenter: TradeChatPresenter

    /** Drive whether the node has delivered the trade chat messages, or cannot deliver them at all. */
    private val chatMessagesSynced = MutableStateFlow(false)
    private val chatMessagesSyncFailed = MutableStateFlow(false)

    override fun beforeStartKoin() {
        super.beforeStartKoin()
        globalUiManager = spyk(GlobalUiManager(testDispatcher))
    }

    override fun onKoinReady() {
        every { tradesServiceFacade.selectedTrade } returns MutableStateFlow(null)
        every { tradesServiceFacade.openTradesSynced } returns MutableStateFlow(true)
        every { tradesServiceFacade.openTradesSyncFailed } returns MutableStateFlow(false)
        every { tradeChatMessagesServiceFacade.chatMessagesSynced } returns chatMessagesSynced
        every { tradeChatMessagesServiceFacade.chatMessagesSyncFailed } returns chatMessagesSyncFailed
        every { userProfileServiceFacade.ignoredProfileIds } returns MutableStateFlow(emptySet())
        every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(emptyList())
        every { settingsRepository.data } returns MutableStateFlow(Settings())
        every { tradeReadStateRepository.data } returns MutableStateFlow(TradeReadStateMap())

        presenter =
            TradeChatPresenter(
                mainPresenter,
                tradesServiceFacade,
                tradeChatMessagesServiceFacade,
                settingsRepository,
                tradeReadStateRepository,
                userProfileServiceFacade,
                notificationController,
                messageDeliveryServiceFacade,
            )
    }

    @Test
    fun `rapid double-tap on sendChatMessage triggers send only once`() =
        runTest {
            coEvery { tradeChatMessagesServiceFacade.sendChatMessage(any(), any()) } coAnswers {
                kotlinx.coroutines.delay(Long.MAX_VALUE)
                Result.success(Unit)
            }

            presenter.onAction(TradeChatUiAction.OnSendMessage("hello"))
            presenter.onAction(TradeChatUiAction.OnSendMessage("hello"))
            advanceUntilIdle()

            coVerify { tradeChatMessagesServiceFacade.sendChatMessage("hello", null) }
            assertFalse(presenter.isSendChatMessageEnabled.value)
        }

    @Test
    fun `sendChatMessage success clears quoted message`() =
        runTest {
            val quoted = mockk<BisqEasyOpenTradeMessage>(relaxed = true)
            every { quoted.text } returns "quoted"
            every { quoted.id } returns "q1"
            every { quoted.senderUserProfileId } returns "sender"
            presenter.onAction(TradeChatUiAction.OnReply(quoted))
            coEvery { tradeChatMessagesServiceFacade.sendChatMessage(any(), any()) } returns
                Result.success(Unit)

            presenter.onAction(TradeChatUiAction.OnSendMessage("hello"))
            advanceUntilIdle()

            assertNull(presenter.uiState.value.quotedMessage)
        }

    @Test
    fun `loading holds while the trade chat messages have not arrived`() =
        runTest {
            val messages = givenTradeWithMessages()

            presenter.initialize("tid")
            runCurrent()

            assertTrue(presenter.uiState.value.isLoading, "Messages have not arrived yet")

            messages.value = setOf(mockk<BisqEasyOpenTradeMessage>(relaxed = true))
            runCurrent()

            assertFalse(presenter.uiState.value.isLoading)
        }

    @Test
    fun `loading stops on an empty chat once the messages have synced`() =
        runTest {
            givenTradeWithMessages()

            presenter.initialize("tid")
            runCurrent()

            assertTrue(presenter.uiState.value.isLoading, "Nothing has been delivered yet")

            chatMessagesSynced.value = true
            runCurrent()

            assertFalse(presenter.uiState.value.isLoading)
        }

    /** On the client a subscribe that fails once is only retried on the next reconnect. */
    @Test
    fun `loading stops when the chat messages are not coming because their subscription failed`() =
        runTest {
            givenTradeWithMessages()

            presenter.initialize("tid")
            runCurrent()
            assertTrue(presenter.uiState.value.isLoading, "Nothing has been delivered yet")

            chatMessagesSyncFailed.value = true
            runCurrent()

            assertFalse(presenter.uiState.value.isLoading)
        }

    @Test
    fun `loading stops when the trade is not found so the dialog is not hidden behind the spinner`() =
        runTest {
            every { tradesServiceFacade.openTradeItems } returns MutableStateFlow(emptyList())
            every { tradesServiceFacade.selectedTrade } returns MutableStateFlow(null)

            presenter.initialize("tid")
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.isLoading)
            assertTrue(presenter.uiState.value.isTradeNotFound)
        }

    @Test
    fun `confirmed ignore user calls ignoreUserProfile`() =
        runTest {
            coEvery { userProfileServiceFacade.ignoreUserProfile("peer-1") } returns Unit

            presenter.onAction(TradeChatUiAction.OnIgnoreUserClick("peer-1"))
            presenter.onAction(TradeChatUiAction.OnConfirmIgnore)
            advanceUntilIdle()

            coVerify { userProfileServiceFacade.ignoreUserProfile("peer-1") }
            assertNull(presenter.uiState.value.ignoreTargetProfileId)
        }

    @Test
    fun `when the ignore call is cancelled then no error is surfaced to the user`() =
        runTest {
            coEvery { userProfileServiceFacade.ignoreUserProfile("peer-1") } throws
                CancellationException("navigated away")

            presenter.onAction(TradeChatUiAction.OnIgnoreUserClick("peer-1"))
            presenter.onAction(TradeChatUiAction.OnConfirmIgnore)
            advanceUntilIdle()

            verify(exactly = 0) { globalUiManager.showSnackbar(any(), any(), any(), any()) }
        }

    @Test
    fun `confirmed undo ignore user calls undoIgnoreUserProfile`() =
        runTest {
            coEvery { userProfileServiceFacade.undoIgnoreUserProfile("peer-2") } returns Unit

            presenter.onAction(TradeChatUiAction.OnUndoIgnoreUserClick("peer-2"))
            presenter.onAction(TradeChatUiAction.OnConfirmUndoIgnore)
            advanceUntilIdle()

            coVerify { userProfileServiceFacade.undoIgnoreUserProfile("peer-2") }
            assertNull(presenter.uiState.value.undoIgnoreTargetProfileId)
        }

    @Test
    fun `myProfiles are the owned profiles the highlighter matches against`() =
        runTest {
            val me = createMockUserProfile("me")
            val userProfiles = MutableStateFlow(listOf(me))
            every { userProfileServiceFacade.userProfiles } returns userProfiles
            givenTradeWithMessages()

            presenter.initialize("tid")
            runCurrent()

            assertEquals(listOf(me), presenter.uiState.value.myProfiles)

            val work = createMockUserProfile("work")
            userProfiles.value = listOf(me, work)
            runCurrent()

            assertEquals(listOf(me, work), presenter.uiState.value.myProfiles)
        }

    @Test
    fun `mention candidates are scoped to the trade own identity`() =
        runTest {
            val me = createMockUserProfile("me")
            val myOther = createMockUserProfile("myOther")
            val peer = createMockUserProfile("peer")
            val mediator = createMockUserProfile("mediator")
            val author = createMockUserProfile("author")
            every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(listOf(me, myOther))

            val messages =
                MutableStateFlow(
                    setOf(
                        createMockBisqEasyOpenTradeMessage(
                            id = "m1",
                            text = "hello",
                            senderUserProfile = author,
                            myUserProfile = me,
                        ),
                    ),
                )
            givenTradeWithMessages(messages, traders = setOf(peer), mediator = mediator, myProfile = me)

            presenter.initialize("tid")
            runCurrent()

            // Raw author, peer, mediator, and the identity this trade runs with — an unrelated
            // owned profile is not mentionable in a trade chat.
            assertEquals(
                listOf(author.id, peer.id, mediator.id, me.id),
                presenter.uiState.value.mentionCandidates
                    .map { it.id },
            )
        }

    @Test
    fun `a failed report draft is restored only for the same accused profile`() {
        val accused = createMockUserProfile("accused")
        val other = createMockUserProfile("other")
        val fromAccused = createMockBisqEasyOpenTradeMessage(id = "a1", senderUserProfile = accused)
        val fromOther = createMockBisqEasyOpenTradeMessage(id = "b1", senderUserProfile = other)

        presenter.onAction(TradeChatUiAction.OnReportUserClick(fromAccused))
        presenter.onAction(TradeChatUiAction.OnReportFailure("the typed report"))

        assertNull(presenter.uiState.value.reportTargetMessage)
        assertEquals("the typed report", presenter.uiState.value.reportDraft)
        assertEquals(accused.id, presenter.uiState.value.reportDraftProfileId)

        presenter.onAction(TradeChatUiAction.OnReportUserClick(fromOther))

        assertEquals(fromOther, presenter.uiState.value.reportTargetMessage)
        assertNull(presenter.uiState.value.reportDraft)
        assertNull(presenter.uiState.value.reportDraftProfileId)
    }

    @Test
    fun `reopening a failed report for the same profile restores the draft`() {
        val accused = createMockUserProfile("accused")
        val first = createMockBisqEasyOpenTradeMessage(id = "a1", senderUserProfile = accused)
        val retry = createMockBisqEasyOpenTradeMessage(id = "a2", senderUserProfile = accused)

        presenter.onAction(TradeChatUiAction.OnReportUserClick(first))
        presenter.onAction(TradeChatUiAction.OnReportFailure("the typed report"))
        presenter.onAction(TradeChatUiAction.OnReportUserClick(retry))

        assertEquals(retry, presenter.uiState.value.reportTargetMessage)
        assertEquals("the typed report", presenter.uiState.value.reportDraft)
        assertEquals(accused.id, presenter.uiState.value.reportDraftProfileId)
    }

    @Test
    fun `dismissing a report clears the draft and its owner`() {
        val accused = createMockUserProfile("accused")
        val message = createMockBisqEasyOpenTradeMessage(id = "a1", senderUserProfile = accused)

        presenter.onAction(TradeChatUiAction.OnReportUserClick(message))
        presenter.onAction(TradeChatUiAction.OnReportFailure("the typed report"))
        presenter.onAction(TradeChatUiAction.OnReportUserClick(message))
        presenter.onAction(TradeChatUiAction.OnDismissReportDialog)

        assertNull(presenter.uiState.value.reportTargetMessage)
        assertNull(presenter.uiState.value.reportDraft)
        assertNull(presenter.uiState.value.reportDraftProfileId)
    }

    @Test
    fun `a report failure with no open target does not retain an unowned draft`() {
        presenter.onAction(TradeChatUiAction.OnReportFailure("stale draft"))

        assertNull(presenter.uiState.value.reportTargetMessage)
        assertNull(presenter.uiState.value.reportDraft)
        assertNull(presenter.uiState.value.reportDraftProfileId)
    }

    /** A trade the facade can resolve, with a channel whose messages the caller drives. */
    private fun givenTradeWithMessages(
        messages: MutableStateFlow<Set<BisqEasyOpenTradeMessage>> = MutableStateFlow(emptySet()),
        traders: Set<UserProfileVO> = emptySet(),
        mediator: UserProfileVO? = null,
        myProfile: UserProfileVO = createMockUserProfile("me"),
    ): MutableStateFlow<Set<BisqEasyOpenTradeMessage>> {
        val myIdentity = mockk<UserIdentityVO>()
        every { myIdentity.userProfile } returns myProfile

        val channel = mockk<BisqEasyOpenTradeChannel>(relaxed = true)
        every { channel.chatMessages } returns messages
        every { channel.traders } returns traders
        every { channel.mediator } returns mediator
        every { channel.myUserIdentity } returns myIdentity

        val trade = mockk<TradeItemPresentationModel>(relaxed = true)
        every { trade.tradeId } returns "tid"
        every { trade.bisqEasyOpenTradeChannelModel } returns channel

        every { tradesServiceFacade.openTradeItems } returns MutableStateFlow(listOf(trade))
        every { tradesServiceFacade.openTradesSynced } returns MutableStateFlow(true)
        return messages
    }
}
