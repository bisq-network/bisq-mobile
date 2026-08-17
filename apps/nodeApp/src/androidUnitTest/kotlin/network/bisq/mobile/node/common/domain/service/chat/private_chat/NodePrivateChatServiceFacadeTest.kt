package network.bisq.mobile.node.common.domain.service.chat.private_chat

import bisq.chat.ChatChannelDomain
import bisq.chat.ChatMessageType
import bisq.chat.ChatService
import bisq.chat.notifications.ChatNotificationService
import bisq.chat.two_party.TwoPartyPrivateChatChannelService
import bisq.common.observable.collection.ObservableSet
import bisq.network.SendMessageResult
import bisq.user.UserService
import bisq.user.banned.BannedUserService
import bisq.user.identity.UserIdentity
import bisq.user.identity.UserIdentityService
import bisq.user.profile.UserProfile
import bisq.user.profile.UserProfileService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatMessageReaction
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import network.bisq.mobile.test.coroutines.TestCoroutineJobsManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.util.Optional
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import bisq.chat.two_party.TwoPartyPrivateChatChannel as Bisq2TwoPartyPrivateChatChannel
import bisq.chat.two_party.TwoPartyPrivateChatMessage as Bisq2TwoPartyPrivateChatMessage

/**
 * Covers `removeChatMessageReaction`'s guards and the per-channel observer lifecycle.
 *
 * The lifecycle half drives real `ObservableSet`s — the same class Bisq 2 hands the facade — so that
 * `addObserver`'s replay and the add/remove/clear dispatch are exercised rather than stubbed. Only the
 * channels and messages hanging off them are mocks.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NodePrivateChatServiceFacadeTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var channelService: TwoPartyPrivateChatChannelService
    private lateinit var userIdentityService: UserIdentityService
    private lateinit var userProfileService: UserProfileService
    private lateinit var bannedUserService: BannedUserService
    private lateinit var chatNotificationService: ChatNotificationService
    private lateinit var channels: ObservableSet<Bisq2TwoPartyPrivateChatChannel>
    private lateinit var facade: NodePrivateChatServiceFacade

    private val me: UserProfileVO = createMockUserProfile("me")

    @Before
    fun setUp() {
        mockkStatic(Dispatchers::class)
        every { Dispatchers.Default } returns testDispatcher
        Dispatchers.setMain(testDispatcher)

        startKoin {
            modules(
                module {
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                },
            )
        }

        channelService = mockk(relaxed = true)
        userIdentityService = mockk(relaxed = true)
        userProfileService = mockk(relaxed = true)
        bannedUserService = mockk(relaxed = true)
        chatNotificationService = mockk(relaxed = true)
        channels = ObservableSet()

        every { channelService.channels } returns channels
        // Bisq 2 returns the network-store copy; the identity of the instance is all these tests need.
        every { userProfileService.getManagedUserProfile(any()) } answers { firstArg() }

        val chatService = mockk<ChatService>(relaxed = true)
        every { chatService.twoPartyPrivateChatChannelService } returns channelService
        every { chatService.chatNotificationService } returns chatNotificationService

        val userService = mockk<UserService>(relaxed = true)
        every { userService.userIdentityService } returns userIdentityService
        every { userService.userProfileService } returns userProfileService
        every { userService.bannedUserService } returns bannedUserService

        val applicationService = mockk<AndroidApplicationService>(relaxed = true)
        every { applicationService.chatService } returns chatService
        every { applicationService.userService } returns userService

        val provider = AndroidApplicationService.Provider()
        provider.applicationService = applicationService

        facade = NodePrivateChatServiceFacade(provider)
    }

    @After
    fun tearDown() {
        unmockkStatic(Dispatchers::class)
        Dispatchers.resetMain()
        stopKoin()
    }

    /**
     * A reaction id Bisq 2 knows and this build does not must come back as a failure. Resolving the
     * enum as a call argument put the lookup outside the facade's `runCatching`, so it threw out of
     * the suspend function instead.
     */
    @Test
    fun `an unknown reaction id fails instead of throwing`() =
        runTest(testDispatcher) {
            every { userIdentityService.findUserIdentity(any()) } returns Optional.of(mockk<UserIdentity>(relaxed = true))

            val result = facade.removeChatMessageReaction("channel-1", "message-1", reaction(reactionId = 99))

            assertTrue(result.isFailure)
            verify(exactly = 0) { channelService.sendTextMessageReaction(any(), any(), any(), any()) }
        }

    @Test
    fun `a reaction that is not ours reports false without calling the node`() =
        runTest(testDispatcher) {
            every { userIdentityService.findUserIdentity(any()) } returns Optional.empty()

            val result = facade.removeChatMessageReaction("channel-1", "message-1", reaction(reactionId = 0))

            assertFalse(result.getOrThrow())
            verify(exactly = 0) { channelService.sendTextMessageReaction(any(), any(), any(), any()) }
        }

    /** The known-id path still reaches the channel lookup — here it misses, so it fails cleanly. */
    @Test
    fun `a known reaction id on a missing channel fails rather than throwing`() =
        runTest(testDispatcher) {
            every { userIdentityService.findUserIdentity(any()) } returns Optional.of(mockk<UserIdentity>(relaxed = true))
            // findChannel is overloaded (String / ChatMessage), so both sides need to be explicit.
            every { channelService.findChannel(any<String>()) } returns Optional.empty<Bisq2TwoPartyPrivateChatChannel>()

            val result = facade.removeChatMessageReaction("channel-1", "message-1", reaction(ReactionEnum.THUMBS_UP.ordinal))

            assertTrue(result.isFailure)
        }

    /**
     * `ObservableCollection.addAll` hands `onAllAdded` the whole argument, not just the elements that
     * were new, so a channel already present is handled a second time with no `onCleared` in between.
     * `addObserver` replays the collection too, which is how the first handling happens here.
     */
    @Test
    fun `a channel handled twice does not appear twice`() =
        runTest(testDispatcher) {
            val kept = channel("c1")
            channels.add(kept)
            facade.activate()

            channels.addAll(listOf(kept, channel("c2")))

            assertEquals(
                listOf("c1", "c2"),
                facade.channels.value
                    .map { it.id }
                    .sorted(),
            )
        }

    /**
     * `unbindChannelPins` removes by channel id. Unbinding everything instead would leave the surviving
     * channel with a model that silently stops receiving messages — invisible until a DM goes missing.
     */
    @Test
    fun `removing a channel leaves the other channel's message observer bound`() =
        runTest(testDispatcher) {
            val kept = channel("c1")
            val removed = channel("c2")
            channels.add(kept)
            channels.add(removed)
            facade.activate()

            channels.remove(removed)
            kept.chatMessages.add(message("m1"))

            assertEquals(listOf("c1"), facade.channels.value.map { it.id })
            assertEquals(
                listOf("m1"),
                facade.channels.value
                    .single()
                    .chatMessages.value
                    .map { it.id },
            )
        }

    /**
     * Bisq 2 rejects banned senders on the inbound path, so this only covers a peer banned *after*
     * their messages arrived — the same re-check desktop does.
     */
    @Test
    fun `a message from a banned sender does not reach the model`() =
        runTest(testDispatcher) {
            val channel = channel("c1")
            channels.add(channel)
            facade.activate()

            every { bannedUserService.isUserProfileBanned(any<UserProfile>()) } returns true
            channel.chatMessages.add(message("banned"))
            every { bannedUserService.isUserProfileBanned(any<UserProfile>()) } returns false
            channel.chatMessages.add(message("ok"))

            assertEquals(
                listOf("ok"),
                facade.channels.value
                    .single()
                    .chatMessages.value
                    .map { it.id },
            )
        }

    /**
     * Bisq 2 dispatches asynchronously, so the failure only shows up in the returned future. Without
     * awaiting it the facade returns `Result.success` for a message that never left, and
     * `PrivateChatPresenter` — which clears the quoted message on success only — would drop the
     * citation the user is still waiting to send and show no error.
     */
    @Test
    fun `a send whose dispatch fails is reported as a failure`() =
        runTest(testDispatcher) {
            val channel = channel("channel-1")
            every { channelService.findChannel(any<String>()) } returns Optional.of(channel)
            every { channelService.sendTextMessage(any(), any(), any()) } returns
                CompletableFuture.failedFuture(IllegalStateException("no peer reachable"))

            val result = facade.sendChatMessage("channel-1", "hello", citation = null)

            assertTrue(result.isFailure)
        }

    @Test
    fun `a send whose dispatch succeeds is reported as a success`() =
        runTest(testDispatcher) {
            val channel = channel("channel-1")
            every { channelService.findChannel(any<String>()) } returns Optional.of(channel)
            every { channelService.sendTextMessage(any(), any(), any()) } returns
                CompletableFuture.completedFuture(mockk<SendMessageResult>(relaxed = true))

            val result = facade.sendChatMessage("channel-1", "hello", citation = null)

            assertTrue(result.isSuccess)
        }

    private fun channel(id: String): Bisq2TwoPartyPrivateChatChannel =
        mockk<Bisq2TwoPartyPrivateChatChannel>(relaxed = true) {
            every { this@mockk.id } returns id
            every { chatChannelDomain } returns ChatChannelDomain.DISCUSSION
            every { chatMessages } returns ObservableSet()
        }

    private fun message(id: String): Bisq2TwoPartyPrivateChatMessage =
        mockk<Bisq2TwoPartyPrivateChatMessage>(relaxed = true) {
            every { this@mockk.id } returns id
            every { chatMessageType } returns ChatMessageType.TEXT
            every { text } returns Optional.of("text of $id")
            every { citation } returns Optional.empty()
            every { chatMessageReactions } returns ObservableSet()
        }

    private fun reaction(reactionId: Int) =
        TwoPartyPrivateChatMessageReaction(
            id = "reaction-1",
            senderUserProfile = me,
            receiverUserProfileId = "receiver-1",
            receiverNetworkId = me.networkId,
            chatChannelId = "channel-1",
            chatChannelDomain = ChatChannelDomainEnum.DISCUSSION,
            chatMessageId = "message-1",
            reactionId = reactionId,
            date = 1234L,
            isRemoved = false,
        )
}
