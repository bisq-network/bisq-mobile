package network.bisq.mobile.presentation.community.messages

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.chat.Citation
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatChannel
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatMessageReaction
import network.bisq.mobile.data.replicated.chat.two_party.createMockTwoPartyPrivateChatMessage
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.chat.private_chat.PrivateChatServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The Messages tab (#1825): the list renders from [PrivateChatServiceFacade.channels] — never a
 * navigation-time snapshot — sorted newest activity first, with each row's preview, unread count
 * and reputation kept live.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MessagesPresenterTest : PresentationKoinTestBase() {
    private lateinit var mainPresenter: MainPresenter
    private lateinit var facade: FakePrivateChatServiceFacade
    private lateinit var reputationServiceFacade: ReputationServiceFacade

    private val myProfile = createMockUserProfile("Me")

    override fun onKoinReady() {
        mainPresenter = mockk(relaxed = true)
        facade = FakePrivateChatServiceFacade()
        reputationServiceFacade = mockk(relaxed = true)
        every { reputationServiceFacade.scoreByUserProfileId } returns MutableStateFlow(emptyMap())
        coEvery { reputationServiceFacade.getReputation(any()) } returns
            Result.success(ReputationScoreVO(totalScore = 900, fiveSystemScore = 4.5, ranking = 1))
    }

    private fun presenter(): MessagesPresenter = MessagesPresenter(mainPresenter, facade, mockk<UserProfileServiceFacade>(relaxed = true), reputationServiceFacade)

    private fun attachedPresenter(): MessagesPresenter = presenter().also { it.onViewAttached() }

    private fun channel(
        id: String,
        peerName: String,
    ): TwoPartyPrivateChatChannel =
        TwoPartyPrivateChatChannel(
            id = id,
            chatChannelDomain = ChatChannelDomainEnum.DISCUSSION,
            peer = createMockUserProfile(peerName),
            myUserProfile = myProfile,
        )

    private fun TwoPartyPrivateChatChannel.withMessage(
        text: String,
        date: Long,
        fromMe: Boolean = false,
    ): TwoPartyPrivateChatChannel {
        addChatMessage(
            createMockTwoPartyPrivateChatMessage(
                text = text,
                date = date,
                senderUserProfile = if (fromMe) myUserProfile else peer,
                myUserProfile = myUserProfile,
            ),
        )
        return this
    }

    @Test
    fun `renders conversations newest activity first with preview, unread and reputation`() =
        runTest {
            val older = channel("discussion.a-b", "Alice").withMessage("first message", date = 1_000L)
            val newer = channel("discussion.a-c", "Carol").withMessage("second message", date = 2_000L)
            newer.setUnreadCount(3)
            facade.backing.value = listOf(older, newer)

            val presenter = attachedPresenter()
            advanceUntilIdle()

            val items = presenter.uiState.value.conversations
            assertEquals(listOf("discussion.a-c", "discussion.a-b"), items.map { it.channelId })
            assertEquals("second message", items[0].lastMessagePreview)
            assertEquals(3L, items[0].unreadCount)
            assertEquals(4.5, items[0].starRating)
            assertEquals("Carol", items[0].peerProfile.userName)
        }

    @Test
    fun `my own last message is previewed with the You prefix`() =
        runTest {
            facade.backing.value =
                listOf(channel("discussion.a-b", "Alice").withMessage("thanks!", date = 1_000L, fromMe = true))

            val presenter = attachedPresenter()
            advanceUntilIdle()

            assertEquals(
                "mobile.privateChats.list.previewMine".i18n("thanks!"),
                presenter.uiState.value.conversations
                    .first()
                    .lastMessagePreview,
            )
        }

    @Test
    fun `a new message re-sorts the list without a reload`() =
        runTest {
            val a = channel("discussion.a-b", "Alice").withMessage("hi", date = 2_000L)
            val b = channel("discussion.a-c", "Carol").withMessage("hello", date = 1_000L)
            facade.backing.value = listOf(a, b)
            val presenter = attachedPresenter()
            advanceUntilIdle()
            assertEquals(
                "discussion.a-b",
                presenter.uiState.value.conversations[0]
                    .channelId,
            )

            b.withMessage("newest", date = 3_000L)
            advanceUntilIdle()

            assertEquals(
                "discussion.a-c",
                presenter.uiState.value.conversations[0]
                    .channelId,
            )
            assertEquals(
                "newest",
                presenter.uiState.value.conversations[0]
                    .lastMessagePreview,
            )
        }

    @Test
    fun `starts loading when the facade has no snapshot yet and clears on the first emission`() =
        runTest {
            val presenter = attachedPresenter()
            assertTrue(presenter.uiState.value.isLoading)

            advanceUntilIdle()

            assertFalse(presenter.uiState.value.isLoading)
        }

    @Test
    fun `seeds synchronously from an already-loaded facade without a loading flash`() =
        runTest {
            facade.backing.value = listOf(channel("discussion.a-b", "Alice").withMessage("hi", date = 1_000L))

            val presenter = presenter()

            assertEquals(1, presenter.uiState.value.conversations.size)
            assertFalse(presenter.uiState.value.isLoading)
        }

    @Test
    fun `a conversation click opens the private chat`() =
        runTest {
            val presenter = attachedPresenter()

            presenter.onAction(PrivateChatListUiAction.OnConversationClick("discussion.a-b"))
            advanceUntilIdle()

            verify { navigationManager.navigate(NavRoute.PrivateChat("discussion.a-b"), any(), any()) }
        }

    @Test
    fun `browse offerbook switches to the offerbook tab`() =
        runTest {
            val presenter = attachedPresenter()

            presenter.onAction(PrivateChatListUiAction.OnBrowseOfferbookClick)
            advanceUntilIdle()

            verify { navigationManager.navigateToTab(NavRoute.TabOfferbookMarket, any(), any(), any()) }
        }

    /** Only [channels] is read; the mutations exist because the interface has them. */
    private class FakePrivateChatServiceFacade : PrivateChatServiceFacade {
        val backing = MutableStateFlow<List<TwoPartyPrivateChatChannel>>(emptyList())
        override val channels: StateFlow<List<TwoPartyPrivateChatChannel>> = backing.asStateFlow()

        override val isSupported: Flow<Boolean> = flowOf(true)

        override suspend fun findOrCreateChannel(peerProfileId: String) = Result.success("discussion.a-b")

        override suspend fun sendChatMessage(
            channelId: String,
            text: String,
            citation: Citation?,
        ) = Result.success(Unit)

        override suspend fun addChatMessageReaction(
            channelId: String,
            messageId: String,
            reactionEnum: ReactionEnum,
        ) = Result.success(Unit)

        override suspend fun removeChatMessageReaction(
            channelId: String,
            messageId: String,
            reaction: TwoPartyPrivateChatMessageReaction,
        ) = Result.success(true)

        override suspend fun leaveChannel(channelId: String) = Result.success(Unit)

        override suspend fun consumeNotifications(channelId: String) = Unit
    }
}
