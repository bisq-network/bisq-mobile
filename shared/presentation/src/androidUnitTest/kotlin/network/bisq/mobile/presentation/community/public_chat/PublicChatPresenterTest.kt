package network.bisq.mobile.presentation.community.public_chat

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.chat.common.CommonPublicChatChannel
import network.bisq.mobile.data.replicated.chat.common.CommonPublicChatMessage
import network.bisq.mobile.data.replicated.chat.common.createMockCommonPublicChatMessage
import network.bisq.mobile.data.replicated.chat.reactions.CommonPublicChatMessageReaction
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.chat.public_chat.PublicChatNotAuthorException
import network.bisq.mobile.data.service.chat.public_chat.PublicChatRemovalRejectedException
import network.bisq.mobile.data.service.chat.public_chat.PublicChatSendRefusedException
import network.bisq.mobile.data.service.chat.public_chat.PublicChatSendRejection
import network.bisq.mobile.data.service.chat.public_chat.PublicChatServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The public thread differs from a DM in the two places a copy-paste from `PrivateChatPresenter`
 * would silently misbehave: the channel is resolved by DOMAIN, because the facade really serves two
 * channels; and ignore, report and profile targets are PER MESSAGE, because a public channel has no
 * fixed peer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PublicChatPresenterTest : PresentationKoinTestBase() {
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val publicChatServiceFacade: PublicChatServiceFacade = mockk(relaxed = true)
    private val userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true)
    private val settingsRepository = SettingsRepositoryMock()

    private val me: UserProfileVO = createMockUserProfile("me")
    private val alice: UserProfileVO = createMockUserProfile("alice")
    private val bob: UserProfileVO = createMockUserProfile("bob")

    private val channels = MutableStateFlow<List<CommonPublicChatChannel>>(emptyList())
    private val ignoredProfileIds = MutableStateFlow<Set<String>>(emptySet())
    private val userProfiles = MutableStateFlow(listOf(me))

    private lateinit var presenter: PublicChatPresenter

    override fun onKoinReady() {
        I18nSupport.initialize("en")
        every { publicChatServiceFacade.channels } returns channels
        every { publicChatServiceFacade.isSupported } returns flowOf(true)
        every { userProfileServiceFacade.ignoredProfileIds } returns ignoredProfileIds
        every { userProfileServiceFacade.userProfiles } returns userProfiles
        // Mirrors production, where consuming drives the channel's unread count to zero — on the node
        // synchronously. A relaxed no-op would let a presenter that reads the count *after* consuming
        // still pass, which is the bug this couples the test to.
        coEvery { publicChatServiceFacade.consumeNotifications(any()) } answers {
            channels.value.find { it.id == firstArg<String>() }?.setUnreadCount(0)
            Unit
        }

        presenter =
            PublicChatPresenter(
                mainPresenter,
                publicChatServiceFacade,
                userProfileServiceFacade,
                settingsRepository,
                ChatChannelDomainEnum.DISCUSSION,
            )
    }

    /** `firstOrNull()` would pick Support in silence, and the literal id would break on a rename. */
    @Test
    fun `the channel is resolved by domain out of the two the facade serves`() =
        runTest {
            channels.value = listOf(supportChannel(), discussionChannel())
            presenter.onViewAttached()
            advanceUntilIdle()

            assertEquals("discussion.bisq", presenter.uiState.value.channelId)
        }

    /**
     * A timeout here is a node-shaped assumption: on Bisq Connect the same code waits for a WebSocket
     * subscribe and its snapshot over Tor, and would routinely tell a healthy user the channel does
     * not exist.
     */
    @Test
    fun `no elapsed time alone reports the channel as missing`() =
        runTest {
            presenter.onViewAttached()
            advanceTimeBy(60_000)

            assertTrue(presenter.uiState.value.isLoading, "waiting must never turn into a terminal state")

            channels.value = listOf(discussionChannel())
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.isLoading)
            assertEquals("discussion.bisq", presenter.uiState.value.channelId)
        }

    /** Consuming zeroes the count synchronously on the node, so reading after would always give 0. */
    @Test
    fun `the unread count is read before the consume that zeroes it`() =
        runTest {
            val channel = discussionChannel(messages = (1..5).map { message("m$it", alice, date = it.toLong()) })
            channel.setUnreadCount(3)
            channels.value = listOf(channel)

            presenter.onViewAttached()
            advanceUntilIdle()

            assertEquals(2, presenter.uiState.value.readCount)
        }

    /** `ChatInputField` clears its text the moment it hands it over, so an early send costs it. */
    @Test
    fun `sending is disabled until the channel resolves`() =
        runTest {
            presenter.onViewAttached()
            advanceUntilIdle()

            assertFalse(presenter.isSendChatMessageEnabled.value)

            channels.value = listOf(discussionChannel())
            advanceUntilIdle()

            assertTrue(presenter.isSendChatMessageEnabled.value)
        }

    /**
     * Many senders share a millisecond on a busy channel, and without a stable secondary key the
     * keyed `LazyColumn` reorders visibly. Same comparator as bisq2's `NEWEST_FIRST`.
     *
     * The arrival order of the two messages sharing a date is load-bearing: "a" arrives first, so a
     * stable sort on the date alone would leave it there and the tie-break is the only thing that can
     * put "b" above it. Sorting them into the fixture alphabetically makes this pass either way.
     */
    @Test
    fun `messages sort newest first with an id tie-break on equal dates`() =
        runTest {
            channels.value =
                listOf(
                    discussionChannel(
                        messages =
                            listOf(
                                message("a", alice, date = 100),
                                message("c", alice, date = 200),
                                message("b", alice, date = 100),
                            ),
                    ),
                )
            presenter.onViewAttached()
            advanceUntilIdle()

            assertEquals(
                listOf("c", "b", "a"),
                presenter.uiState.value.messages
                    .map { it.id },
            )
        }

    /**
     * Filtering has to neutralise the read-count machinery: `ChatMessageList` derives the unread
     * divider and the jump-to-bottom badge from `messages.size - readCount`, so scrolling a filtered
     * list would otherwise mark unread messages read.
     */
    @Test
    fun `a search filters, reports its match count and marks nothing read`() =
        runTest {
            channels.value =
                listOf(
                    discussionChannel(
                        messages =
                            listOf(
                                message("m1", alice, date = 1, text = "when do we settle"),
                                message("m2", bob, date = 2, text = "payment sent"),
                                message("m3", alice, date = 3, text = "settled, thanks"),
                            ),
                    ),
                )
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PublicChatUiAction.OnSearchQueryChange("settle"))
            advanceUntilIdle()

            val state = presenter.uiState.value
            assertEquals(listOf("m3", "m1"), state.messages.map { it.id })
            assertEquals(2, state.searchMatchCount)
            assertEquals(state.messages.size, state.readCount)
        }

    /**
     * The other half of ignoring someone. The action firing is not the outcome the user asked for —
     * what they asked for is to stop reading that person, and the presenter is the only place that
     * drops them: `ChatMessageList` renders whatever list it is handed.
     */
    @Test
    fun `messages from an ignored sender leave the thread`() =
        runTest {
            channels.value =
                listOf(
                    discussionChannel(
                        messages = listOf(message("m1", alice, date = 1), message("m2", bob, date = 2)),
                    ),
                )
            presenter.onViewAttached()
            advanceUntilIdle()

            ignoredProfileIds.value = setOf(bob.id)
            advanceUntilIdle()

            assertEquals(
                listOf("m1"),
                presenter.uiState.value.messages
                    .map { it.id },
            )
        }

    /** A public channel has no fixed peer: the subject is whoever wrote the message the menu was opened on. */
    @Test
    fun `ignoring targets the sender of the message the menu was opened on`() =
        runTest {
            channels.value =
                listOf(
                    discussionChannel(
                        messages = listOf(message("m1", alice, date = 1), message("m2", bob, date = 2)),
                    ),
                )
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PublicChatUiAction.OnIgnoreUserClick(bob.id))
            presenter.onAction(PublicChatUiAction.OnConfirmIgnore)
            advanceUntilIdle()

            coVerify(exactly = 1) { userProfileServiceFacade.ignoreUserProfile(bob.id) }
            coVerify(exactly = 0) { userProfileServiceFacade.ignoreUserProfile(alice.id) }
        }

    @Test
    fun `reporting targets the sender of the message the menu was opened on`() =
        runTest {
            val fromBob = message("m2", bob, date = 2)
            channels.value = listOf(discussionChannel(messages = listOf(message("m1", alice, date = 1), fromBob)))
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PublicChatUiAction.OnReportUserClick(fromBob))

            assertEquals(bob, presenter.uiState.value.reportTargetUserProfile)
        }

    @Test
    fun `a double tap on confirm delete deletes once`() =
        runTest {
            val mine = message("m1", me, date = 1, isMyMessage = true)
            channels.value = listOf(discussionChannel(messages = listOf(mine)))
            coEvery { publicChatServiceFacade.deleteChatMessage(any(), any()) } returns Result.success(Unit)
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PublicChatUiAction.OnDeleteMessageClick(mine))
            presenter.onAction(PublicChatUiAction.OnConfirmDelete)
            presenter.onAction(PublicChatUiAction.OnConfirmDelete)
            advanceUntilIdle()

            coVerify(exactly = 1) { publicChatServiceFacade.deleteChatMessage("discussion.bisq", "m1") }
        }

    /** The composer clears its text as soon as it hands the message over, so a log line is not enough. */
    @Test
    fun `a send that fails tells the user instead of only logging`() =
        runTest {
            channels.value = listOf(discussionChannel())
            coEvery { publicChatServiceFacade.sendChatMessage(any(), any(), any()) } returns
                Result.failure(IllegalStateException("no connection"))
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PublicChatUiAction.OnSendMessage("hello"))
            advanceUntilIdle()

            verify { globalUiManager.showSnackbar(any(), any(), any(), any()) }
        }

    /** An edit is a save, not a send: the composer hands over the same action either way. */
    @Test
    fun `sending while editing saves the edit instead of posting a new message`() =
        runTest {
            val mine = message("m1", me, date = 1, text = "original", isMyMessage = true)
            channels.value = listOf(discussionChannel(messages = listOf(mine)))
            coEvery { publicChatServiceFacade.editChatMessage(any(), any(), any()) } returns Result.success(Unit)
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PublicChatUiAction.OnEditMessage(mine))
            assertEquals("original", presenter.uiState.value.editingInitialText)

            presenter.onAction(PublicChatUiAction.OnSendMessage("edited"))
            advanceUntilIdle()

            coVerify(exactly = 1) { publicChatServiceFacade.editChatMessage("discussion.bisq", "m1", "edited") }
            coVerify(exactly = 0) { publicChatServiceFacade.sendChatMessage(any(), any(), any()) }
            assertEquals(null, presenter.uiState.value.editingMessageId)
        }

    /**
     * The hub mounts this tab with `RememberPresenterLifecycle`, which cancels `presenterScope` on
     * detach and recreates it on re-attach — so the collectors have to be restarted. A guard that
     * only asked whether the job had ever been started would short-circuit here, leaving a tab that
     * renders a frozen list and never sends again.
     */
    @Test
    fun `re-attaching the tab restarts the channel collectors`() =
        runTest {
            channels.value = listOf(discussionChannel(messages = listOf(message("m1", alice, date = 1))))
            presenter.onViewAttached()
            advanceUntilIdle()
            assertEquals(
                listOf("m1"),
                presenter.uiState.value.messages
                    .map { it.id },
            )

            presenter.onViewUnattaching()
            advanceUntilIdle()

            presenter.onViewAttached()
            advanceUntilIdle()

            // A message arriving AFTER the re-attach: asserting on what is already in the state would
            // pass on the stale value a StateFlow keeps, whether or not anything is still collecting.
            channels.value.single().addChatMessage(message("m2", alice, date = 2))
            advanceUntilIdle()

            assertEquals(
                listOf("m2", "m1"),
                presenter.uiState.value.messages
                    .map { it.id },
            )
        }

    /**
     * The four failures the facade can report that `handleError`'s default copy ("something went
     * wrong, try again") would send the user in circles over: none of them is a connection problem and
     * no retry fixes any of them. The whole reason the exception types exist is this copy, so each is
     * asserted on the message rather than on "a snackbar happened".
     */
    @Test
    fun `each refusal the node can report gets its own copy`() =
        runTest {
            val cases =
                listOf(
                    PublicChatSendRefusedException(PublicChatSendRejection.MY_PROFILE_BANNED) to
                        "mobile.community.chat.refused.myProfileBanned",
                    PublicChatSendRefusedException(PublicChatSendRejection.RATE_LIMIT_EXCEEDED) to
                        "mobile.community.chat.refused.rateLimitExceeded",
                    PublicChatNotAuthorException() to "mobile.community.chat.notAuthor",
                    PublicChatRemovalRejectedException("message") to "mobile.community.chat.removalRejected",
                )
            channels.value = listOf(discussionChannel())
            presenter.onViewAttached()
            advanceUntilIdle()

            cases.forEach { (exception, key) ->
                coEvery { publicChatServiceFacade.sendChatMessage(any(), any(), any()) } returns Result.failure(exception)

                presenter.onAction(PublicChatUiAction.OnSendMessage("hello"))
                advanceUntilIdle()

                verify { globalUiManager.showSnackbar(key.i18n(), SnackbarType.ERROR, any(), any()) }
            }
        }

    /** UNKNOWN is the one rejection with nothing specific to say, so it falls back to the generic copy. */
    @Test
    fun `an unknown rejection falls back to the generic copy`() =
        runTest {
            channels.value = listOf(discussionChannel())
            coEvery { publicChatServiceFacade.sendChatMessage(any(), any(), any()) } returns
                Result.failure(PublicChatSendRefusedException(PublicChatSendRejection.UNKNOWN))
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PublicChatUiAction.OnSendMessage("hello"))
            advanceUntilIdle()

            verify { globalUiManager.showSnackbar("mobile.error.generic".i18n(), SnackbarType.ERROR, any(), any()) }
        }

    @Test
    fun `a reaction is added and removed on the message the menu was opened on`() =
        runTest {
            val peers = message("m1", alice, date = 1)
            channels.value = listOf(discussionChannel(messages = listOf(peers)))
            coEvery { publicChatServiceFacade.addChatMessageReaction(any(), any(), any()) } returns Result.success(Unit)
            coEvery { publicChatServiceFacade.removeChatMessageReaction(any(), any(), any()) } returns Result.success(Unit)
            presenter.onViewAttached()
            advanceUntilIdle()
            val reaction = reaction(peers)

            presenter.onAction(PublicChatUiAction.OnAddReaction(peers, ReactionEnum.THUMBS_UP))
            presenter.onAction(PublicChatUiAction.OnRemoveReaction(peers, reaction))
            advanceUntilIdle()

            coVerify(exactly = 1) { publicChatServiceFacade.addChatMessageReaction("discussion.bisq", "m1", ReactionEnum.THUMBS_UP) }
            coVerify(exactly = 1) { publicChatServiceFacade.removeChatMessageReaction("discussion.bisq", "m1", reaction) }
        }

    /** No loading overlay on a reaction, so a failure has only the snackbar left to say it did not land. */
    @Test
    fun `a reaction that fails tells the user`() =
        runTest {
            val peers = message("m1", alice, date = 1)
            channels.value = listOf(discussionChannel(messages = listOf(peers)))
            coEvery { publicChatServiceFacade.addChatMessageReaction(any(), any(), any()) } returns
                Result.failure(PublicChatSendRefusedException(PublicChatSendRejection.RATE_LIMIT_EXCEEDED))
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PublicChatUiAction.OnAddReaction(peers, ReactionEnum.THUMBS_UP))
            advanceUntilIdle()

            verify { globalUiManager.showSnackbar("mobile.community.chat.refused.rateLimitExceeded".i18n(), SnackbarType.ERROR, any(), any()) }
        }

    @Test
    fun `undoing an ignore targets the profile the menu was opened on and closes the dialog`() =
        runTest {
            val peers = message("m1", alice, date = 1)
            channels.value = listOf(discussionChannel(messages = listOf(peers)))
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PublicChatUiAction.OnUndoIgnoreUserClick(alice.id))
            assertEquals(alice.id, presenter.uiState.value.undoIgnoreTargetProfileId)
            presenter.onAction(PublicChatUiAction.OnConfirmUndoIgnore)
            advanceUntilIdle()

            coVerify(exactly = 1) { userProfileServiceFacade.undoIgnoreUserProfile(alice.id) }
            assertEquals(null, presenter.uiState.value.undoIgnoreTargetProfileId)
        }

    /**
     * bisq2 has no partial consume, so the node round trip is debounced while the list reports scrolls.
     * The window is the presenter's own, restated here because its companion is private.
     */
    @Test
    fun `a burst of scrolls consumes the channel once`() =
        runTest {
            channels.value = listOf(discussionChannel(messages = listOf(message("m1", alice, date = 1))))
            presenter.onViewAttached()
            advanceUntilIdle()
            // Opening the thread consumes once on its own; what the scrolling adds is what is under test.
            clearMocks(publicChatServiceFacade, answers = false)

            presenter.onAction(PublicChatUiAction.OnUpdateReadCount(1))
            presenter.onAction(PublicChatUiAction.OnUpdateReadCount(2))
            presenter.onAction(PublicChatUiAction.OnUpdateReadCount(3))
            advanceTimeBy(CONSUME_NOTIFICATIONS_DEBOUNCE_MS * 2)
            advanceUntilIdle()

            coVerify(exactly = 1) { publicChatServiceFacade.consumeNotifications("discussion.bisq") }
        }

    @Test
    fun `myProfiles are the owned profiles the highlighter matches against`() =
        runTest {
            channels.value = listOf(discussionChannel())
            presenter.onViewAttached()
            advanceUntilIdle()

            assertEquals(listOf(me), presenter.uiState.value.myProfiles)

            val work = createMockUserProfile("work")
            userProfiles.value = listOf(me, work)
            advanceUntilIdle()

            assertEquals(listOf(me, work), presenter.uiState.value.myProfiles)
        }

    /** Handed to `ChatMessageList` per row, so a lookup that throws must not take the list down with it. */
    @Test
    fun `an unresolvable author falls back to a placeholder name`() =
        runTest {
            coEvery { userProfileServiceFacade.findUserProfile(any()) } throws IllegalStateException("no profile")

            assertEquals("data.na".i18n(), presenter.getUserName(alice.id))
        }

    // Helpers

    private companion object {
        const val CONSUME_NOTIFICATIONS_DEBOUNCE_MS = 500L
    }

    private fun discussionChannel(messages: List<CommonPublicChatMessage> = emptyList()) =
        CommonPublicChatChannel(
            id = "discussion.bisq",
            chatChannelDomain = ChatChannelDomainEnum.DISCUSSION,
            channelTitle = "bisq",
        ).apply { setAllChatMessages(messages.toSet()) }

    private fun supportChannel() =
        CommonPublicChatChannel(
            id = "support.support",
            chatChannelDomain = ChatChannelDomainEnum.SUPPORT,
            channelTitle = "support",
        )

    private fun reaction(message: CommonPublicChatMessage) =
        CommonPublicChatMessageReaction(
            id = "r-${message.id}",
            userProfileId = me.id,
            chatChannelId = "discussion.bisq",
            chatChannelDomain = ChatChannelDomainEnum.DISCUSSION,
            chatMessageId = message.id,
            reactionId = ReactionEnum.THUMBS_UP.ordinal,
            date = 1,
        )

    private fun message(
        id: String,
        sender: UserProfileVO,
        date: Long,
        text: String = "text of $id",
        isMyMessage: Boolean? = null,
    ) = createMockCommonPublicChatMessage(
        id = id,
        text = text,
        date = date,
        senderUserProfile = sender,
        myUserProfile = me,
        isMyMessage = isMyMessage,
    )
}
