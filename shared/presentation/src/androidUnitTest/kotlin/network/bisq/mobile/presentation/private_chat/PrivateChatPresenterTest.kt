package network.bisq.mobile.presentation.private_chat

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatChannel
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatMessageReaction
import network.bisq.mobile.data.replicated.chat.two_party.createMockTwoPartyPrivateChatMessage
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.chat.private_chat.PrivateChatNotPermittedException
import network.bisq.mobile.data.service.chat.private_chat.PrivateChatServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PrivateChatPresenterTest : PresentationKoinTestBase() {
    private companion object {
        const val CHANNEL_ID = "discussion.a-b"

        val PEER_REPUTATION = ReputationScoreVO(totalScore = 12_400L, fiveSystemScore = 4.5, ranking = 7)
    }

    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val privateChatServiceFacade: PrivateChatServiceFacade = mockk(relaxed = true)
    private val userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true)
    private val reputationServiceFacade: ReputationServiceFacade = mockk(relaxed = true)
    private val notificationController: NotificationController = mockk(relaxed = true)
    private val settingsRepository = SettingsRepositoryMock()

    private val me: UserProfileVO = createMockUserProfile("me")
    private val peer: UserProfileVO = createMockUserProfile("peer")
    private val ignoredPeer: UserProfileVO = createMockUserProfile("ignoredPeer")

    private val channels = MutableStateFlow<List<TwoPartyPrivateChatChannel>>(emptyList())
    private val ignoredProfileIds = MutableStateFlow<Set<String>>(emptySet())

    /**
     * Never left to the relaxed mock: `loadReputation` reads it to tell an unresolved score apart
     * from a real zero, and empty is the "nothing has loaded yet" case the default here wants.
     */
    private val reputationScores = MutableStateFlow<Map<String, Long>>(emptyMap())

    private lateinit var presenter: PrivateChatPresenter

    override fun onKoinReady() {
        I18nSupport.initialize("en")
        every { privateChatServiceFacade.channels } returns channels
        every { userProfileServiceFacade.ignoredProfileIds } returns ignoredProfileIds
        // Mirrors production, where consuming drives the channel's unread count to zero — on the node
        // flavour synchronously. A relaxed no-op here would let a presenter that reads the count
        // *after* consuming still pass, which is exactly the bug this couples the tests to.
        coEvery { privateChatServiceFacade.consumeNotifications(any()) } answers {
            channels.value.find { it.id == firstArg<String>() }?.setUnreadCount(0)
            Unit
        }
        every { reputationServiceFacade.scoreByUserProfileId } returns reputationScores
        coEvery { reputationServiceFacade.getReputation(any()) } returns Result.failure(IllegalStateException("none"))

        presenter =
            PrivateChatPresenter(
                mainPresenter,
                privateChatServiceFacade,
                userProfileServiceFacade,
                reputationServiceFacade,
                notificationController,
                settingsRepository,
            )
    }

    @Test
    fun `a channel that only arrives after navigation is still picked up`() =
        runTest {
            presenter.initialize(CHANNEL_ID)
            advanceTimeBy(1_000)

            assertFalse(presenter.uiState.value.isChannelNotFound, "must not give up before the subscription lands")

            channels.value = listOf(channel())
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.isChannelNotFound)
            assertEquals("peer", presenter.uiState.value.peerName)
        }

    @Test
    fun `a channel that never arrives reports not found`() =
        runTest {
            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()

            assertTrue(presenter.uiState.value.isChannelNotFound)
            assertFalse(presenter.uiState.value.isLoading)
        }

    @Test
    fun `read count counts only displayed messages, so ignored senders cannot push it past the list`() =
        runTest {
            ignoredProfileIds.value = setOf(ignoredPeer.id)
            val channel = channel()
            // Newest first: the two newest are unread, and one of those is from an ignored sender.
            channel.setAllChatMessages(
                setOf(
                    message("m1", peer, date = 1L),
                    message("m2", peer, date = 2L),
                    message("m3", ignoredPeer, date = 3L),
                    message("m4", peer, date = 4L),
                ),
            )
            channel.setUnreadCount(2)
            channels.value = listOf(channel)

            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()

            val state = presenter.uiState.value
            assertEquals(3, state.messages.size, "the ignored sender's message is not displayed")
            // m4 is unread and displayed; m3 is unread but hidden; m1 and m2 are read.
            assertEquals(2, state.readCount)
            assertTrue(state.readCount <= state.messages.size, "would make ChatMessageList compute a negative unread")
        }

    @Test
    fun `a burst of scroll updates consumes notifications once`() =
        runTest {
            channels.value = listOf(channel())
            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()

            repeat(10) { presenter.onAction(PrivateChatUiAction.OnUpdateReadCount(it)) }
            advanceUntilIdle()

            // Once for opening the thread, once for the debounced burst.
            coVerify(exactly = 2) { privateChatServiceFacade.consumeNotifications(CHANNEL_ID) }
        }

    @Test
    fun `sending a message clears the quote on success`() =
        runTest {
            channels.value = listOf(channel())
            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()
            val quoted = message("m1", peer, date = 1L)
            presenter.onAction(PrivateChatUiAction.OnReply(quoted))
            coEvery { privateChatServiceFacade.sendChatMessage(any(), any(), any()) } returns Result.success(Unit)

            presenter.onAction(PrivateChatUiAction.OnSendMessage("hello"))
            advanceUntilIdle()

            assertNull(presenter.uiState.value.quotedMessage)
            coVerify { privateChatServiceFacade.sendChatMessage(CHANNEL_ID, "hello", any()) }
        }

    /**
     * `ChatInputField` clears its text as soon as it hands the message over, so a send that fails on a
     * dropped connection loses what the user typed. Without a snackbar the only trace is a log line,
     * and the message looks sent.
     */
    @Test
    fun `a send that fails tells the user instead of only logging`() =
        runTest {
            channels.value = listOf(channel())
            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()
            val quoted = message("m1", peer, date = 1L)
            presenter.onAction(PrivateChatUiAction.OnReply(quoted))
            coEvery {
                privateChatServiceFacade.sendChatMessage(any(), any(), any())
            } returns Result.failure(IllegalStateException("connection dropped"))

            presenter.onAction(PrivateChatUiAction.OnSendMessage("hello"))
            advanceUntilIdle()

            verify { globalUiManager.showSnackbar(any(), any(), any(), any()) }
            // The quote survives, so replying again does not mean re-picking the message it answers.
            assertEquals(quoted, presenter.uiState.value.quotedMessage)
        }

    @Test
    fun `a failed report keeps the draft so reopening restores it`() =
        runTest {
            channels.value = listOf(channel())
            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()

            presenter.onAction(PrivateChatUiAction.OnReportUserClick(message("m1", peer, date = 1L)))
            presenter.onAction(PrivateChatUiAction.OnReportFailure("they scammed me"))

            val state = presenter.uiState.value
            assertFalse(state.showReportDialog)
            assertEquals("they scammed me", state.reportDraft)
        }

    @Test
    fun `leaving a channel navigates back`() =
        runTest {
            channels.value = listOf(channel())
            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()
            coEvery { privateChatServiceFacade.leaveChannel(CHANNEL_ID) } returns Result.success(Unit)

            presenter.onAction(PrivateChatUiAction.OnConfirmLeave)
            advanceUntilIdle()

            coVerify { privateChatServiceFacade.leaveChannel(CHANNEL_ID) }
            assertFalse(presenter.uiState.value.showLeaveConfirmDialog)
        }

    @Test
    fun `the unread count survives opening the thread, which consumes it`() =
        runTest {
            val channel = channel()
            channel.setAllChatMessages(
                setOf(
                    message("m1", peer, date = 1L),
                    message("m2", peer, date = 2L),
                    message("m3", peer, date = 3L),
                ),
            )
            channel.setUnreadCount(2)
            channels.value = listOf(channel)

            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()

            // Opening consumed the channel, so channel.unreadCount is now 0. The divider still has to
            // reflect what was unread when the user arrived — otherwise it never renders at all.
            assertEquals(0L, channel.unreadCount.value, "opening must consume")
            assertEquals(1, presenter.uiState.value.readCount, "m1 read; m2 and m3 were unread on open")
        }

    @Test
    fun `once the list reports a read count it wins over the count at open`() =
        runTest {
            val channel = channel()
            channel.setAllChatMessages(
                setOf(
                    message("m1", peer, date = 1L),
                    message("m2", peer, date = 2L),
                    message("m3", peer, date = 3L),
                ),
            )
            channel.setUnreadCount(2)
            channels.value = listOf(channel)

            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()
            assertEquals(1, presenter.uiState.value.readCount)

            // The list is the only side that knows how far the user scrolled.
            presenter.onAction(PrivateChatUiAction.OnUpdateReadCount(3))
            advanceUntilIdle()

            assertEquals(3, presenter.uiState.value.readCount)
        }

    @Test
    fun `a reported read count beyond the displayed messages is clamped`() =
        runTest {
            val channel = channel()
            channel.setAllChatMessages(setOf(message("m1", peer, date = 1L)))
            channels.value = listOf(channel)
            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()

            presenter.onAction(PrivateChatUiAction.OnUpdateReadCount(99))
            advanceUntilIdle()

            // ChatMessageList derives unread as messages.size - readCount; a count past the end would
            // make that negative.
            assertEquals(1, presenter.uiState.value.readCount)
        }

    @Test
    fun `reactions are added and removed on the open channel`() =
        runTest {
            channels.value = listOf(channel())
            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()
            val target = message("m1", peer, date = 1L)
            // Mocked rather than built: the presenter only forwards it, and a real one needs a
            // NetworkIdVO that has nothing to do with what is under test.
            val reaction: TwoPartyPrivateChatMessageReaction = mockk(relaxed = true)

            presenter.onAction(PrivateChatUiAction.OnAddReaction(target, ReactionEnum.THUMBS_UP))
            presenter.onAction(PrivateChatUiAction.OnRemoveReaction(target, reaction))
            advanceUntilIdle()

            coVerify { privateChatServiceFacade.addChatMessageReaction(CHANNEL_ID, "m1", ReactionEnum.THUMBS_UP) }
            coVerify { privateChatServiceFacade.removeChatMessageReaction(CHANNEL_ID, "m1", reaction) }
        }

    @Test
    fun `confirming ignore hides the dialog and ignores the sender`() =
        runTest {
            channels.value = listOf(channel())
            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()

            presenter.onAction(PrivateChatUiAction.OnIgnoreUserClick(peer.id))
            assertEquals(peer.id, presenter.uiState.value.ignoreUserId)

            presenter.onAction(PrivateChatUiAction.OnConfirmIgnore)
            advanceUntilIdle()

            coVerify { userProfileServiceFacade.ignoreUserProfile(peer.id) }
            assertEquals("", presenter.uiState.value.ignoreUserId, "the dialog must close")
        }

    @Test
    fun `confirming undo ignore hides the dialog and un-ignores the sender`() =
        runTest {
            channels.value = listOf(channel())
            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()

            presenter.onAction(PrivateChatUiAction.OnUndoIgnoreUserClick(peer.id))
            presenter.onAction(PrivateChatUiAction.OnConfirmUndoIgnore)
            advanceUntilIdle()

            coVerify { userProfileServiceFacade.undoIgnoreUserProfile(peer.id) }
            assertEquals("", presenter.uiState.value.undoIgnoreUserId)
        }

    @Test
    fun `a leave that fails closes the dialog and tells the user`() =
        runTest {
            channels.value = listOf(channel())
            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()
            coEvery { privateChatServiceFacade.leaveChannel(CHANNEL_ID) } returns Result.failure(IllegalStateException("boom"))

            presenter.onAction(PrivateChatUiAction.OnLeaveChatClick)
            assertTrue(presenter.uiState.value.showLeaveConfirmDialog)

            presenter.onAction(PrivateChatUiAction.OnConfirmLeave)
            advanceUntilIdle()

            // The dialog closes on failure too. Leaving it up would strand the user on a modal whose
            // only action just failed, with nothing on screen saying why.
            assertFalse(presenter.uiState.value.showLeaveConfirmDialog)
            verify { globalUiManager.showSnackbar(any(), any(), any(), any()) }
            verify(exactly = 0) { navigationManager.navigateBack(any()) }
        }

    @Test
    fun `dismissing the leave dialog leaves the channel alone`() =
        runTest {
            channels.value = listOf(channel())
            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()

            presenter.onAction(PrivateChatUiAction.OnLeaveChatClick)
            presenter.onAction(PrivateChatUiAction.OnDismissLeaveDialog)
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.showLeaveConfirmDialog)
            coVerify(exactly = 0) { privateChatServiceFacade.leaveChannel(any()) }
        }

    @Test
    fun `dismissing the not-found dialog leaves the screen`() =
        runTest {
            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()
            assertTrue(presenter.uiState.value.isChannelNotFound)

            presenter.onAction(PrivateChatUiAction.OnChannelNotFoundDialogDismiss)
            advanceUntilIdle()

            // Clearing the flag matters as much as navigating: the dialog is driven by it, so a
            // dismiss that only navigated would re-show it if the screen were ever restored.
            assertFalse(presenter.uiState.value.isChannelNotFound)
            verify { navigationManager.navigateBack(any()) }
        }

    @Test
    fun `the peer header opens the peer profile`() =
        runTest {
            channels.value = listOf(channel())
            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()

            presenter.onAction(PrivateChatUiAction.OnPeerHeaderClick)

            verify { navigationManager.navigate(NavRoute.PeerProfile(peer.id), any(), any()) }
        }

    @Test
    fun `the peer header does nothing until the channel resolves`() =
        runTest {
            presenter.initialize(CHANNEL_ID)

            presenter.onAction(PrivateChatUiAction.OnPeerHeaderClick)

            // No peer yet, so there is no profile to open — tapping must not navigate to a blank one.
            verify(exactly = 0) { navigationManager.navigate(any(), any(), any()) }
        }

    @Test
    fun `dismissing the report dialog discards the draft`() =
        runTest {
            channels.value = listOf(channel())
            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()

            presenter.onAction(PrivateChatUiAction.OnReportUserClick(message("m1", peer, date = 1L)))
            presenter.onAction(PrivateChatUiAction.OnReportFailure("half-typed"))
            presenter.onAction(PrivateChatUiAction.OnDismissReportDialog)

            val state = presenter.uiState.value
            assertFalse(state.showReportDialog)
            // Unlike a failure, an explicit dismiss is the user abandoning the report.
            assertNull(state.reportDraft)
        }

    @Test
    fun `dismissing the chat rules warn box persists the choice`() =
        runTest {
            channels.value = listOf(channel())
            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()
            assertTrue(settingsRepository.mutableData.value.showChatRulesWarnBox, "starts shown")

            presenter.onAction(PrivateChatUiAction.OnDontShowAgainChatRulesWarningBox)
            advanceUntilIdle()

            // Persisted, not just hidden in the UiState: "don't show again" has to outlive the screen.
            assertFalse(settingsRepository.mutableData.value.showChatRulesWarnBox)
        }

    @Test
    fun `getUserName resolves the profile and falls back when it is gone`() =
        runTest {
            coEvery { userProfileServiceFacade.findUserProfile(peer.id) } returns peer
            coEvery { userProfileServiceFacade.findUserProfile("stranger") } returns null

            assertEquals("peer", presenter.getUserName(peer.id))
            assertEquals("data.na".i18n(), presenter.getUserName("stranger"))
        }

    @Test
    fun `an ignore that fails still closes the dialog`() =
        runTest {
            channels.value = listOf(channel())
            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()
            coEvery { userProfileServiceFacade.ignoreUserProfile(peer.id) } throws IllegalStateException("boom")

            presenter.onAction(PrivateChatUiAction.OnIgnoreUserClick(peer.id))
            presenter.onAction(PrivateChatUiAction.OnConfirmIgnore)
            advanceUntilIdle()

            // Same reasoning as the leave failure: a dialog that cannot be dismissed is worse than a
            // silently failed ignore, and the guard flag would stay held otherwise.
            assertEquals("", presenter.uiState.value.ignoreUserId)
        }

    @Test
    fun `an undo ignore that fails still closes the dialog`() =
        runTest {
            channels.value = listOf(channel())
            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()
            coEvery { userProfileServiceFacade.undoIgnoreUserProfile(peer.id) } throws IllegalStateException("boom")

            presenter.onAction(PrivateChatUiAction.OnUndoIgnoreUserClick(peer.id))
            presenter.onAction(PrivateChatUiAction.OnConfirmUndoIgnore)
            advanceUntilIdle()

            assertEquals("", presenter.uiState.value.undoIgnoreUserId)
        }

    /**
     * A refused reaction used to be completely silent: the `Result` was discarded, and on Bisq Connect
     * the emoji only appears once the `PRIVATE_CHAT_REACTIONS` subscription echoes it back, so nothing
     * at all happened on screen.
     */
    @Test
    fun `a reaction that fails tells the user instead of doing nothing`() =
        runTest {
            channels.value = listOf(channel())
            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()
            coEvery {
                privateChatServiceFacade.addChatMessageReaction(any(), any(), any())
            } returns Result.failure(PrivateChatNotPermittedException())

            presenter.onAction(PrivateChatUiAction.OnAddReaction(message("m1", peer, date = 1L), ReactionEnum.THUMBS_UP))
            advanceUntilIdle()

            verify { globalUiManager.showSnackbar("mobile.privateChats.notPermitted".i18n(), any(), any(), any()) }
        }

    /** `false` means "not ours to remove", a documented outcome — it must not raise anything. */
    @Test
    fun `a removal reporting it was not ours stays silent`() =
        runTest {
            channels.value = listOf(channel())
            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()
            coEvery {
                privateChatServiceFacade.removeChatMessageReaction(any(), any(), any())
            } returns Result.success(false)

            presenter.onAction(
                PrivateChatUiAction.OnRemoveReaction(message("m1", peer, date = 1L), mockk(relaxed = true)),
            )
            advanceUntilIdle()

            verify(exactly = 0) { globalUiManager.showSnackbar(any(), any(), any(), any()) }
        }

    /**
     * The screen is reachable without ever calling `findOrCreateChannel` — a notification tap opens a
     * conversation whose DMs keep arriving over the `PRIVATE_CHAT_*` topics, which bisq 2
     * authenticates but never authorises. So the first 403 lands on the send, and `handleError`'s
     * default copy would tell the user to check their connection about a problem only a re-pairing
     * fixes. `PeerProfilePresenter` already says the right thing on the entry-point path.
     */
    @Test
    fun `a send refused for a withheld permission says so instead of blaming the connection`() =
        runTest {
            channels.value = listOf(channel())
            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()
            coEvery {
                privateChatServiceFacade.sendChatMessage(any(), any(), any())
            } returns Result.failure(PrivateChatNotPermittedException())

            presenter.onAction(PrivateChatUiAction.OnSendMessage("hello"))
            advanceUntilIdle()

            verify { globalUiManager.showSnackbar("mobile.privateChats.notPermitted".i18n(), any(), any(), any()) }
            verify(exactly = 0) { globalUiManager.showSnackbar("mobile.error.generic".i18n(), any(), any(), any()) }
        }

    @Test
    fun `a leave refused for a withheld permission says so too`() =
        runTest {
            channels.value = listOf(channel())
            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()
            coEvery { privateChatServiceFacade.leaveChannel(CHANNEL_ID) } returns
                Result.failure(PrivateChatNotPermittedException())

            presenter.onAction(PrivateChatUiAction.OnConfirmLeave)
            advanceUntilIdle()

            verify { globalUiManager.showSnackbar("mobile.privateChats.notPermitted".i18n(), any(), any(), any()) }
        }

    /** Anything that is not a permission failure keeps the generic copy — the handler must not widen. */
    @Test
    fun `an ordinary send failure keeps the generic error`() =
        runTest {
            channels.value = listOf(channel())
            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()
            coEvery {
                privateChatServiceFacade.sendChatMessage(any(), any(), any())
            } returns Result.failure(IllegalStateException("connection dropped"))

            presenter.onAction(PrivateChatUiAction.OnSendMessage("hello"))
            advanceUntilIdle()

            verify { globalUiManager.showSnackbar("mobile.error.generic".i18n(), any(), any(), any()) }
            verify(exactly = 0) { globalUiManager.showSnackbar("mobile.privateChats.notPermitted".i18n(), any(), any(), any()) }
        }

    /**
     * The peer header must not assert a rating it does not have. `?: ZERO_REPUTATION` used to collapse
     * "could not resolve" into a real zero, and on Bisq Connect `getReputation` reads a cache filled
     * asynchronously — so opening a DM before the first payload landed showed no stars for a peer
     * whose offerbook card had just shown 4.5. Mirrors `PeerProfilePresenter`, which the header is one
     * tap away from.
     */
    @Test
    fun `an unresolved reputation is not a zero rating`() =
        runTest {
            channels.value = listOf(channel())

            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()

            assertTrue(presenter.uiState.value.isPeerReputationUnknown, "the cache never filled")
            assertEquals(0.0, presenter.uiState.value.peerStarRating)
        }

    /** The other half: once the cache holds scores, a peer missing from it really is a zero. */
    @Test
    fun `a peer absent from a populated reputation cache rates zero`() =
        runTest {
            reputationScores.value = mapOf("someone-else" to 120L)
            channels.value = listOf(channel())

            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.isPeerReputationUnknown)
            assertEquals(0.0, presenter.uiState.value.peerStarRating)
        }

    /**
     * The header must not stay at "unknown" for as long as the thread is open. On Bisq Connect the
     * score cache is filled by the `REPUTATION` subscription, which can land after this screen is
     * already up — and a single read left the peer with no stars, one tap from a profile that
     * observes the same cache and shows the real rating.
     */
    @Test
    fun `a reputation arriving after the thread is open updates the header`() =
        runTest {
            channels.value = listOf(channel())

            presenter.initialize(CHANNEL_ID)
            advanceUntilIdle()
            assertTrue(presenter.uiState.value.isPeerReputationUnknown, "nothing has arrived yet")

            coEvery { reputationServiceFacade.getReputation(peer.id) } returns Result.success(PEER_REPUTATION)
            reputationScores.value = mapOf(peer.id to PEER_REPUTATION.totalScore)
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.isPeerReputationUnknown)
            assertEquals(4.5, presenter.uiState.value.peerStarRating)
        }

    private fun channel() =
        TwoPartyPrivateChatChannel(
            id = CHANNEL_ID,
            chatChannelDomain = ChatChannelDomainEnum.DISCUSSION,
            peer = peer,
            myUserProfile = me,
        )

    private fun message(
        id: String,
        sender: UserProfileVO,
        date: Long,
    ) = createMockTwoPartyPrivateChatMessage(
        id = id,
        text = "text-$id",
        date = date,
        senderUserProfile = sender,
        myUserProfile = me,
    )
}
