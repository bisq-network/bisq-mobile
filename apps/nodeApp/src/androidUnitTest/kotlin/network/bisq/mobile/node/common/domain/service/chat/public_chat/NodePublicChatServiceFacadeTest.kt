package network.bisq.mobile.node.common.domain.service.chat.public_chat

import bisq.chat.ChatChannelDomain
import bisq.chat.ChatMessageType
import bisq.chat.ChatService
import bisq.chat.common.CommonPublicChatChannelService
import bisq.chat.common.CommonPublicChatMessage.COMMON_PUBLIC_CHAT_MESSAGE_TTL
import bisq.chat.common.SubDomain
import bisq.chat.notifications.ChatNotification
import bisq.chat.notifications.ChatNotificationService
import bisq.common.observable.Observable
import bisq.common.observable.collection.CollectionObserver
import bisq.common.observable.collection.ObservableSet
import bisq.network.p2p.services.data.BroadcastResult
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
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.chat.common.CommonPublicChatChannel
import network.bisq.mobile.data.replicated.chat.common.CommonPublicChatMessage
import network.bisq.mobile.data.replicated.chat.reactions.CommonPublicChatMessageReaction
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.data.service.chat.public_chat.PublicChatNotAuthorException
import network.bisq.mobile.data.service.chat.public_chat.PublicChatRemovalRejectedException
import network.bisq.mobile.data.service.chat.public_chat.PublicChatSendRefusedException
import network.bisq.mobile.data.service.chat.public_chat.PublicChatSendRejection
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import network.bisq.mobile.node.common.test_utils.NodeKoinIntegrationTestBase
import network.bisq.mobile.test.coroutines.TestCoroutineJobsManager
import org.junit.Test
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.Optional
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import bisq.chat.common.CommonPublicChatChannel as Bisq2CommonPublicChatChannel
import bisq.chat.common.CommonPublicChatMessage as Bisq2CommonPublicChatMessage
import bisq.chat.reactions.CommonPublicChatMessageReaction as Bisq2CommonPublicChatMessageReaction

/**
 * Drives real bisq2 channels, messages, reactions and `ObservableSet`s — only the services around
 * them are mocks. The migration of a legacy channel id, `isExpired`, and the message equality the
 * P2P store's re-delivery depends on are all properties of those real objects, so stubbing them
 * would be stubbing the behaviour under test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NodePublicChatServiceFacadeTest : NodeKoinIntegrationTestBase() {
    private lateinit var discussionService: CommonPublicChatChannelService
    private lateinit var supportService: CommonPublicChatChannelService
    private lateinit var discussionChannels: ObservableSet<Bisq2CommonPublicChatChannel>
    private lateinit var supportChannels: ObservableSet<Bisq2CommonPublicChatChannel>
    private lateinit var userIdentityService: UserIdentityService
    private lateinit var userProfileService: UserProfileService
    private lateinit var bannedUserService: BannedUserService
    private lateinit var chatNotificationService: ChatNotificationService
    private lateinit var selectedIdentity: UserIdentity
    private lateinit var facade: NodePublicChatServiceFacade

    /** See `NodePrivateChatServiceFacadeTest`: the facade hardcodes `withContext(Dispatchers.Default)`. */
    override fun beforeStartKoin() {
        mockkStatic(Dispatchers::class)
        every { Dispatchers.Default } returns testDispatcher
    }

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
            },
        )

    override fun onSetup() {
        discussionChannels = ObservableSet()
        supportChannels = ObservableSet()
        discussionService = mockk(relaxed = true)
        supportService = mockk(relaxed = true)
        every { discussionService.channels } returns discussionChannels
        every { supportService.channels } returns supportChannels
        stubBroadcasts(discussionService)
        stubBroadcasts(supportService)

        userIdentityService = mockk(relaxed = true)
        userProfileService = mockk(relaxed = true)
        bannedUserService = mockk(relaxed = true)
        chatNotificationService = mockk(relaxed = true)

        // Every author resolves and nobody is banned unless a test says otherwise.
        every { userProfileService.findUserProfile(any()) } answers { Optional.of(profile(firstArg())) }
        every { bannedUserService.isUserProfileBanned(any<String>()) } returns false
        every { bannedUserService.isRateLimitExceeding(any<String>()) } returns false
        selectedIdentity = mockk(relaxed = true) { every { id } returns selectedIdentityId() }
        every { userIdentityService.selectedUserIdentity } returns selectedIdentity
        every { userIdentityService.findUserIdentity(any()) } returns Optional.empty()
        every { userIdentityService.findUserIdentity(selectedIdentityId()) } returns Optional.of(selectedIdentity)

        val chatService = mockk<ChatService>(relaxed = true)
        every { chatService.commonPublicChatChannelServices } returns
            mapOf(
                ChatChannelDomain.DISCUSSION to discussionService,
                ChatChannelDomain.SUPPORT to supportService,
            )
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

        facade = NodePublicChatServiceFacade(provider)
    }

    override fun onTearDown() {
        unmockkStatic(Dispatchers::class)
    }

    @Test
    fun `both domains are exposed, one channel each`() =
        runTest {
            discussionChannels.add(channel(SubDomain.DISCUSSION_BISQ))
            supportChannels.add(channel(SubDomain.SUPPORT_SUPPORT))

            facade.activate()

            assertEquals(
                mapOf(
                    "discussion.bisq" to ChatChannelDomainEnum.DISCUSSION,
                    "support.support" to ChatChannelDomainEnum.SUPPORT,
                ),
                facade.channels.value.associate { it.id to it.chatChannelDomain },
            )
        }

    /**
     * A node upgraded from before v2.1.1 still holds the consolidated channel in its store. Its
     * `getId()` answers the migrated id and its equality is that id, so the store already holds one
     * channel per domain — filtering deprecated sub-domains would drop the only Discussions channel
     * such a node has. The title follows that migrated id rather than the raw field the surviving
     * instance carries, which is what keeps the i18n key resolvable.
     */
    @Test
    fun `a legacy discussion channel is exposed under its migrated id and title`() =
        runTest {
            discussionChannels.add(channel(SubDomain.DISCUSSION_BITCOIN))

            facade.activate()

            val model = facade.channels.value.single { it.chatChannelDomain == ChatChannelDomainEnum.DISCUSSION }
            assertEquals("discussion.bisq", model.id)
            assertEquals("bisq", model.channelTitle)
        }

    /**
     * The case a raw title cannot survive: `EVENTS` migrates to `DISCUSSION` while `getChannelTitle()`
     * stays `conferences`, and `discussion.conferences.title` is in no bundle — the strings for that
     * channel are filed under `events.`. Deriving from the migrated id sidesteps the pairing.
     */
    @Test
    fun `a legacy events channel is exposed as the discussion channel`() =
        runTest {
            discussionChannels.add(channel(SubDomain.EVENTS_CONFERENCES))

            facade.activate()

            val model = facade.channels.value.single { it.chatChannelDomain == ChatChannelDomainEnum.DISCUSSION }
            assertEquals("discussion.bisq", model.id)
            assertEquals("bisq", model.channelTitle)
        }

    @Test
    fun `an expired message is filtered out`() =
        runTest {
            val channel = channel(SubDomain.DISCUSSION_BISQ)
            channel.chatMessages.add(message("live", date = System.currentTimeMillis()))
            channel.chatMessages.add(message("expired", date = expiredDate()))
            discussionChannels.add(channel)

            facade.activate()

            assertEquals(setOf("live"), discussionMessageIds())
        }

    @Test
    fun `a message from a banned author is filtered out`() =
        runTest {
            val bannedAuthor = profileId("banned")
            every { bannedUserService.isUserProfileBanned(bannedAuthor) } returns true
            val channel = channel(SubDomain.DISCUSSION_BISQ)
            channel.chatMessages.add(message("ok"))
            channel.chatMessages.add(message("hidden", authorId = bannedAuthor))
            discussionChannels.add(channel)

            facade.activate()

            assertEquals(setOf("ok"), discussionMessageIds())
        }

    /** The profile store is pruned concurrently, so one lost author must cost one message. */
    @Test
    fun `a message whose author cannot be resolved is skipped without dropping the batch`() =
        runTest {
            val goneAuthor = profileId("gone")
            every { userProfileService.findUserProfile(goneAuthor) } returns Optional.empty()
            val channel = channel(SubDomain.DISCUSSION_BISQ)
            channel.chatMessages.add(message("ok"))
            channel.chatMessages.add(message("orphan", authorId = goneAuthor))
            discussionChannels.add(channel)

            facade.activate()

            assertEquals(setOf("ok"), discussionMessageIds())
        }

    /**
     * The fresh-install race: the initial inventory delivers messages and author profiles as separate
     * data types in arbitrary order, so a message can arrive before its author. Dropping it for good
     * left a brand-new node's Discussions empty until the next restart replayed the persisted store —
     * the same race bisq2's API layer parks and replays for Connect clients.
     */
    @Test
    fun `a message parked for a missing author appears once the profile arrives`() =
        runTest {
            val lateAuthor = profileId("late")
            every { userProfileService.findUserProfile(lateAuthor) } returns Optional.empty()
            val numUserProfiles = Observable(0)
            every { userProfileService.numUserProfiles } returns numUserProfiles
            val channel = channel(SubDomain.DISCUSSION_BISQ)
            discussionChannels.add(channel)
            facade.activate()

            channel.chatMessages.add(message("early-bird", authorId = lateAuthor))
            advanceUntilIdle()
            assertEquals(emptySet(), discussionMessageIds())

            every { userProfileService.findUserProfile(lateAuthor) } answers { Optional.of(profile(lateAuthor)) }
            numUserProfiles.set(1)
            advanceUntilIdle()

            assertEquals(setOf("early-bird"), discussionMessageIds())
        }

    /**
     * The gap the retry collector must already cover: the channel replay inside `activate()` parks
     * a message, and its author lands before the scheduler has run the collector. bisq2's
     * `Observable.addObserver` replays the current value synchronously, so that profile's signal
     * fires inside `activate()` itself, before any dispatch. With `replay = 0` a signal with no
     * subscriber is dropped for good, and nothing retries the park until the next profile happens
     * to land.
     */
    @Test
    fun `a profile landing before the retry collector has run still replays the parked message`() =
        runTest {
            val lateAuthor = profileId("late")
            var profileLanded = false
            every { userProfileService.findUserProfile(lateAuthor) } answers {
                if (profileLanded) Optional.of(profile(lateAuthor)) else Optional.empty()
            }
            // Read after the channel replay parked the message and right before its observer is
            // registered: the profile lands in exactly that gap.
            every { userProfileService.numUserProfiles } answers {
                profileLanded = true
                Observable(1)
            }
            val channel = channel(SubDomain.DISCUSSION_BISQ)
            channel.chatMessages.add(message("early-bird", authorId = lateAuthor))
            discussionChannels.add(channel)

            facade.activate()
            advanceUntilIdle()

            assertEquals(setOf("early-bird"), discussionMessageIds())
        }

    /**
     * The other half of the fresh-install race: facades activate while the user is still in
     * onboarding, so inventory can land before any identity exists. Those messages park until
     * one is selected instead of vanishing.
     */
    @Test
    fun `a message arriving before an identity is selected appears once one is selected`() =
        runTest {
            var currentIdentity: UserIdentity? = null
            every { userIdentityService.selectedUserIdentity } answers { currentIdentity }
            val selectedObservable = Observable<UserIdentity>()
            every { userIdentityService.selectedUserIdentityObservable } returns selectedObservable
            val channel = channel(SubDomain.DISCUSSION_BISQ)
            discussionChannels.add(channel)
            facade.activate()

            channel.chatMessages.add(message("pre-onboarding"))
            advanceUntilIdle()
            assertEquals(emptySet(), discussionMessageIds())

            currentIdentity = selectedIdentity
            selectedObservable.set(selectedIdentity)
            advanceUntilIdle()

            assertEquals(setOf("pre-onboarding"), discussionMessageIds())
        }

    @Test
    fun `a parked message removed before its author arrives never appears`() =
        runTest {
            val lateAuthor = profileId("late")
            every { userProfileService.findUserProfile(lateAuthor) } returns Optional.empty()
            val numUserProfiles = Observable(0)
            every { userProfileService.numUserProfiles } returns numUserProfiles
            val channel = channel(SubDomain.DISCUSSION_BISQ)
            discussionChannels.add(channel)
            facade.activate()

            val parked = message("retracted", authorId = lateAuthor)
            channel.chatMessages.add(parked)
            channel.chatMessages.remove(parked)

            every { userProfileService.findUserProfile(lateAuthor) } answers { Optional.of(profile(lateAuthor)) }
            numUserProfiles.set(1)
            advanceUntilIdle()

            assertEquals(emptySet(), discussionMessageIds())
        }

    /**
     * The re-delivery trap, for a message still parked: the P2P store replaces a message with a
     * fresh equal instance and `onRemoved(old)` can land after `onAdded(new)` parked the successor.
     * Purging the parked entry by id alone would drop the live instance's park; compared by
     * instance, the stale removal is a no-op — same discipline as the reaction bindings.
     */
    @Test
    fun `a parked message re-delivered as an equal instance survives the old instance's removal`() =
        runTest {
            val lateAuthor = profileId("late")
            every { userProfileService.findUserProfile(lateAuthor) } returns Optional.empty()
            val numUserProfiles = Observable(0)
            every { userProfileService.numUserProfiles } returns numUserProfiles
            val channel = channel(SubDomain.DISCUSSION_BISQ)
            val date = System.currentTimeMillis()
            val original = message("survivor", authorId = lateAuthor, date = date)
            val redelivered = message("survivor", authorId = lateAuthor, date = date)
            assertEquals(original, redelivered)
            assertNotSame(original, redelivered)
            discussionChannels.add(channel)
            // Registered before the facade's observer, so its onRemoved re-adds the equal instance
            // from inside the removal — the facade parks the successor BEFORE its own onRemoved runs.
            channel.chatMessages.addObserver(
                object : CollectionObserver<Bisq2CommonPublicChatMessage> {
                    override fun onAdded(element: Bisq2CommonPublicChatMessage) = Unit

                    override fun onRemoved(element: Any) {
                        channel.chatMessages.add(redelivered)
                    }

                    override fun onCleared() = Unit
                },
            )
            facade.activate()
            channel.chatMessages.add(original)
            channel.chatMessages.remove(original)

            every { userProfileService.findUserProfile(lateAuthor) } answers { Optional.of(profile(lateAuthor)) }
            numUserProfiles.set(1)
            advanceUntilIdle()

            assertEquals(setOf("survivor"), discussionMessageIds())
        }

    /** A ban is a permanent verdict — parking it would just replay a message that must never show. */
    @Test
    fun `a banned author's message is dropped for good, not parked`() =
        runTest {
            val bannedAuthor = profileId("banned")
            every { userProfileService.findUserProfile(bannedAuthor) } returns Optional.empty()
            every { bannedUserService.isUserProfileBanned(bannedAuthor) } returns true
            val numUserProfiles = Observable(0)
            every { userProfileService.numUserProfiles } returns numUserProfiles
            val channel = channel(SubDomain.DISCUSSION_BISQ)
            discussionChannels.add(channel)
            facade.activate()

            channel.chatMessages.add(message("never", authorId = bannedAuthor))
            every { userProfileService.findUserProfile(bannedAuthor) } answers { Optional.of(profile(bannedAuthor)) }
            numUserProfiles.set(1)
            advanceUntilIdle()

            assertEquals(emptySet(), discussionMessageIds())
        }

    /**
     * A message shown before its author was banned still has to be taken back: gating the removal on
     * the same visibility filter that admitted it would leave it on screen for good.
     */
    @Test
    fun `a removal is applied even when the message would now fail the visibility filter`() =
        runTest {
            val author = profileId("author")
            val channel = channel(SubDomain.DISCUSSION_BISQ)
            val message = message("m1", authorId = author)
            channel.chatMessages.add(message)
            discussionChannels.add(channel)
            facade.activate()

            every { bannedUserService.isUserProfileBanned(author) } returns true
            channel.chatMessages.remove(message)

            assertEquals(emptySet(), discussionMessageIds())
        }

    /** bisq2 checks a reaction at both levels; only the reaction's own sender decides this one. */
    @Test
    fun `a reaction from a banned sender is filtered out even when the message author is fine`() =
        runTest {
            val bannedReactor = profileId("reactor-banned")
            every { bannedUserService.isUserProfileBanned(bannedReactor) } returns true
            val channel = channel(SubDomain.DISCUSSION_BISQ)
            val message = message("m1")
            message.chatMessageReactions.add(reaction("r-ok", message, profileId("reactor-ok")))
            message.chatMessageReactions.add(reaction("r-banned", message, bannedReactor))
            channel.chatMessages.add(message)
            discussionChannels.add(channel)

            facade.activate()

            assertEquals(listOf("r-ok"), discussionMessage("m1").chatReactions.value.map { it.id })
        }

    /**
     * bisq2 authorizes edit and delete against ANY of my identities, not only the selected one, which
     * is what gates the Edit and Delete menu items.
     */
    @Test
    fun `isMyMessage is true for a message authored by any of my identities`() =
        runTest {
            val myOtherIdentity = profileId("my-other")
            every { userIdentityService.findUserIdentity(myOtherIdentity) } returns
                Optional.of(mockk<UserIdentity>(relaxed = true))
            val channel = channel(SubDomain.DISCUSSION_BISQ)
            channel.chatMessages.add(message("mine", authorId = myOtherIdentity))
            channel.chatMessages.add(message("theirs"))
            discussionChannels.add(channel)

            facade.activate()

            assertTrue(discussionMessage("mine").isMyMessage)
            assertFalse(discussionMessage("theirs").isMyMessage)
        }

    /**
     * The P2P store re-delivers a message as a fresh instance that is `equals` to the one it replaced,
     * and `ObservableCollection#remove` drops the element before notifying — so `onRemoved(old)` can
     * land after `onAdded(new)` has bound its observer. Unbinding by id would kill the successor's.
     *
     * Reproduced exactly: this observer is registered first, so its `onRemoved` runs before the
     * facade's and re-adds the equal instance from inside it.
     */
    @Test
    fun `a message re-delivered as an equal instance keeps the live instance observable`() =
        runTest {
            val channel = channel(SubDomain.DISCUSSION_BISQ)
            // One date for both: every field but the transient reaction set takes part in bisq2's
            // equality, so two calls to System.currentTimeMillis() would make them different messages
            // and the set would hold both — the opposite of the case under test.
            val date = System.currentTimeMillis()
            val original = message("m1", date = date)
            val redelivered = message("m1", date = date)
            assertEquals(original, redelivered)
            assertNotSame(original, redelivered)
            discussionChannels.add(channel)
            channel.chatMessages.addObserver(
                object : CollectionObserver<Bisq2CommonPublicChatMessage> {
                    override fun onAdded(element: Bisq2CommonPublicChatMessage) = Unit

                    override fun onRemoved(element: Any) {
                        channel.chatMessages.add(redelivered)
                    }

                    override fun onCleared() = Unit
                },
            )
            facade.activate()
            channel.chatMessages.add(original)

            channel.chatMessages.remove(original)
            redelivered.chatMessageReactions.add(reaction("r1", redelivered, profileId("reactor")))

            assertEquals(setOf("m1"), discussionMessageIds())
            assertEquals(listOf("r1"), discussionMessage("m1").chatReactions.value.map { it.id })
        }

    @Test
    fun `clearing the channel messages clears the model`() =
        runTest {
            val channel = activateWithDiscussionChannel(message("m1"), message("m2"))

            channel.chatMessages.clear()

            assertTrue(discussionChannelModel().chatMessages.value.isEmpty())
        }

    /**
     * The lifecycle restarts in-process — a Tor bootstrap retry deactivates then activates — so an
     * observer left bound would write into a model nobody reads for the rest of the process.
     */
    @Test
    fun `deactivate empties the channel list and unbinds the message observer`() =
        runTest {
            val channel = activateWithDiscussionChannel(message("m1"))
            val model = discussionChannelModel()

            facade.deactivate()
            channel.chatMessages.add(message("m2"))

            assertTrue(facade.channels.value.isEmpty())
            assertEquals(
                setOf("m1"),
                model.chatMessages.value
                    .map { it.id }
                    .toSet(),
            )
        }

    /**
     * The other half of that restart: the same facade instance is activated again — a Tor bootstrap
     * retry does exactly this — and has to rebind rather than come back deaf.
     *
     * The badge half is asserted too, because it rebinds by a different mechanism than the messages:
     * the recount collector goes through `serviceScope`, which `deactivate` disposes, so it only
     * survives as long as that stays a getter onto the current scope rather than a cached field.
     * Cache it and messages would keep arriving while the badge silently stopped moving.
     */
    @Test
    fun `activating again after a deactivate rebinds the channel`() =
        runTest {
            val notifications = Observable<ChatNotification>()
            every { chatNotificationService.changedNotification } returns notifications
            val channel = activateWithDiscussionChannel(message("m1"))

            facade.deactivate()
            facade.activate()
            advanceUntilIdle()
            channel.chatMessages.add(message("m2"))
            every { chatNotificationService.getNumNotifications(channel) } returns 7
            notifications.set(mockk(relaxed = true))
            advanceUntilIdle()

            assertEquals(setOf("m1", "m2"), discussionMessageIds())
            assertEquals(7, discussionChannelModel().unreadCount.value)
        }

    @Test
    fun `deactivate unbinds the per-message reaction observer`() =
        runTest {
            val message = message("m1")
            activateWithDiscussionChannel(message)
            val model = discussionChannelModel()

            facade.deactivate()
            message.chatMessageReactions.add(reaction("r1", message, profileId("reactor")))

            assertTrue(
                model.chatMessages.value
                    .single()
                    .chatReactions.value
                    .isEmpty(),
            )
        }

    // The write path

    @Test
    fun `a mutation goes to the service that owns the channel`() =
        runTest {
            discussionChannels.add(channel(SubDomain.DISCUSSION_BISQ))
            supportChannels.add(channel(SubDomain.SUPPORT_SUPPORT))
            facade.activate()

            assertTrue(facade.sendChatMessage("support.support", "hello", citation = null).isSuccess)

            verify(exactly = 1) { supportService.publishChatMessage("hello", any(), any(), any()) }
            verify(exactly = 0) { discussionService.publishChatMessage(any<String>(), any(), any(), any()) }
        }

    /**
     * `CommonPublicChatMessage` verifies its text on construction and the edit path constructs *after*
     * removing the original, so an oversized text has to be refused before the domain sees it. Blank is
     * refused by no message class at all.
     */
    @Test
    fun `blank text is refused before the domain is called`() =
        runTest {
            activateWithDiscussionChannel()

            val result = facade.sendChatMessage("discussion.bisq", "   ", citation = null)

            assertTrue(result.isFailure)
            verify(exactly = 0) { discussionService.publishChatMessage(any<String>(), any(), any(), any()) }
        }

    @Test
    fun `text over the maximum length is refused before the domain is called`() =
        runTest {
            activateWithDiscussionChannel()

            val result = facade.sendChatMessage("discussion.bisq", "x".repeat(10_001), citation = null)

            assertTrue(result.isFailure)
            verify(exactly = 0) { discussionService.publishChatMessage(any<String>(), any(), any(), any()) }
        }

    /**
     * The edit path validates the same way, and has more to lose: the domain removes the original
     * before it constructs the replacement, so a text `CommonPublicChatMessage` refuses on
     * construction would take the original down with it.
     */
    @Test
    fun `an edit with invalid text is refused before the domain is called`() =
        runTest {
            val channel = activateWithDiscussionChannel(mine("m1"))

            assertTrue(facade.editChatMessage(channel.id, "m1", "   ").isFailure)
            assertTrue(facade.editChatMessage(channel.id, "m1", "x".repeat(10_001)).isFailure)

            verify(exactly = 0) { discussionService.publishEditedChatMessage(any(), any(), any()) }
        }

    @Test
    fun `a send from a banned profile is refused before the domain is called`() =
        runTest {
            activateWithDiscussionChannel()
            banSelectedIdentity()

            val result = facade.sendChatMessage("discussion.bisq", "hello", citation = null)

            assertEquals(PublicChatSendRejection.MY_PROFILE_BANNED, rejectionOf(result))
            verify(exactly = 0) { discussionService.publishChatMessage(any<String>(), any(), any(), any()) }
        }

    @Test
    fun `a send from a rate-limited profile is refused before the domain is called`() =
        runTest {
            activateWithDiscussionChannel()
            every { bannedUserService.isRateLimitExceeding(selectedIdentityId()) } returns true

            val result = facade.sendChatMessage("discussion.bisq", "hello", citation = null)

            assertEquals(PublicChatSendRejection.RATE_LIMIT_EXCEEDED, rejectionOf(result))
            verify(exactly = 0) { discussionService.publishChatMessage(any<String>(), any(), any(), any()) }
        }

    /** The domain removes the original and only *then* runs its own check, so a refusal loses the message. */
    @Test
    fun `an edit from a banned profile is refused before the domain is called`() =
        runTest {
            val channel = activateWithDiscussionChannel(mine("m1"))
            banSelectedIdentity()

            val result = facade.editChatMessage(channel.id, "m1", "edited")

            assertEquals(PublicChatSendRejection.MY_PROFILE_BANNED, rejectionOf(result))
            verify(exactly = 0) { discussionService.publishEditedChatMessage(any(), any(), any()) }
        }

    /**
     * The domain has no check on the delete itself, but its removal listener drops the local removal
     * for a banned author while the network removal proceeds — leaving the message stuck here for good.
     */
    @Test
    fun `a delete from a banned profile is refused before the domain is called`() =
        runTest {
            val channel = activateWithDiscussionChannel(mine("m1"))
            banSelectedIdentity()

            val result = facade.deleteChatMessage(channel.id, "m1")

            assertEquals(PublicChatSendRejection.MY_PROFILE_BANNED, rejectionOf(result))
            verify(exactly = 0) { discussionService.deleteChatMessage(any(), any()) }
        }

    /** The domain answers a banned reactor with a future failed on a bare RuntimeException. */
    @Test
    fun `a reaction from a banned profile is refused before the domain is called`() =
        runTest {
            val channel = activateWithDiscussionChannel(message("m1"))
            banSelectedIdentity()

            val result = facade.addChatMessageReaction(channel.id, "m1", ReactionEnum.THUMBS_UP)

            assertEquals(PublicChatSendRejection.MY_PROFILE_BANNED, rejectionOf(result))
            verify(exactly = 0) { discussionService.publishChatMessageReaction(any(), any(), any()) }
        }

    @Test
    fun `editing a message that is not mine fails without calling the domain`() =
        runTest {
            val channel = activateWithDiscussionChannel(message("m1"))

            val result = facade.editChatMessage(channel.id, "m1", "edited")

            assertIs<PublicChatNotAuthorException>(result.exceptionOrNull())
            verify(exactly = 0) { discussionService.publishEditedChatMessage(any(), any(), any()) }
        }

    /**
     * The store answers a rejected removal with a successful, empty result and no event, so a completed
     * future does not mean the removal happened. Without the re-check the edit would report success and
     * the original would stay next to its replacement.
     */
    @Test
    fun `an edit whose original is still in the channel reports failure`() =
        runTest {
            val channel = activateWithDiscussionChannel(mine("m1"))
            every { discussionService.publishEditedChatMessage(any(), any(), any()) } returns broadcast()

            val result = facade.editChatMessage(channel.id, "m1", "edited")

            assertIs<PublicChatRemovalRejectedException>(result.exceptionOrNull())
        }

    @Test
    fun `a delete whose message is still in the channel reports failure`() =
        runTest {
            val channel = activateWithDiscussionChannel(mine("m1"))
            every { discussionService.deleteChatMessage(any(), any()) } returns broadcast()

            val result = facade.deleteChatMessage(channel.id, "m1")

            assertIs<PublicChatRemovalRejectedException>(result.exceptionOrNull())
        }

    @Test
    fun `a delete the store accepts reports success and drops the message from the model`() =
        runTest {
            val channel = activateWithDiscussionChannel(mine("m1"))

            val result = facade.deleteChatMessage(channel.id, "m1")

            assertTrue(result.isSuccess)
            assertEquals(emptySet(), discussionMessageIds())
        }

    /** The local add inside the domain call is synchronous, so a retry finds the reaction and no-ops. */
    @Test
    fun `adding a reaction that is already there does not publish it again`() =
        runTest {
            val message = message("m1")
            message.chatMessageReactions.add(reaction("r1", message, selectedIdentityId()))
            activateWithDiscussionChannel(message)

            val result = facade.addChatMessageReaction("discussion.bisq", "m1", ReactionEnum.THUMBS_UP)

            assertTrue(result.isSuccess)
            verify(exactly = 0) { discussionService.publishChatMessageReaction(any(), any(), any()) }
        }

    @Test
    fun `removing a reaction that is not mine fails without calling the domain`() =
        runTest {
            val message = message("m1")
            message.chatMessageReactions.add(reaction("r1", message, profileId("someone-else")))
            activateWithDiscussionChannel(message)

            val result = facade.removeChatMessageReaction("discussion.bisq", "m1", discussionMessage("m1").chatReactions.value.single())

            assertIs<PublicChatNotAuthorException>(result.exceptionOrNull())
            verify(exactly = 0) { discussionService.deleteChatMessageReaction(any(), any()) }
        }

    @Test
    fun `removing a reaction the message no longer carries succeeds`() =
        runTest {
            val message = message("m1")
            val bisq2Reaction = reaction("r1", message, selectedIdentityId())
            message.chatMessageReactions.add(bisq2Reaction)
            activateWithDiscussionChannel(message)
            val model = discussionMessage("m1").chatReactions.value.single()
            message.chatMessageReactions.remove(bisq2Reaction)

            val result = facade.removeChatMessageReaction("discussion.bisq", "m1", model)

            assertTrue(result.isSuccess)
            verify(exactly = 0) { discussionService.deleteChatMessageReaction(any(), any()) }
        }

    @Test
    fun `an unknown channel id fails cleanly for every method`() =
        runTest {
            activateWithDiscussionChannel(mine("m1"))

            assertTrue(facade.sendChatMessage("nope", "hello", citation = null).isFailure)
            assertTrue(facade.editChatMessage("nope", "m1", "edited").isFailure)
            assertTrue(facade.deleteChatMessage("nope", "m1").isFailure)
            assertTrue(facade.addChatMessageReaction("nope", "m1", ReactionEnum.THUMBS_UP).isFailure)
            assertTrue(
                facade
                    .removeChatMessageReaction("nope", "m1", discussionMessage("m1").let { anyModelReaction() })
                    .isFailure,
            )
        }

    @Test
    fun `an unknown message id fails cleanly`() =
        runTest {
            val channel = activateWithDiscussionChannel(mine("m1"))

            assertTrue(facade.editChatMessage(channel.id, "nope", "edited").isFailure)
            assertTrue(facade.deleteChatMessage(channel.id, "nope").isFailure)
            assertTrue(facade.addChatMessageReaction(channel.id, "nope", ReactionEnum.THUMBS_UP).isFailure)
        }

    /**
     * The badge's node-side producer: bisq2 reports a notification change and the facade recounts every
     * channel it exposes. `changedNotification` carries no channel, so the recount is unconditional.
     */
    @Test
    fun `a notification change recounts the unread badge`() =
        runTest {
            val notifications = Observable<ChatNotification>()
            every { chatNotificationService.changedNotification } returns notifications
            val channel = activateWithDiscussionChannel()
            // The signal is conflated with no replay, so the collector has to be subscribed before it
            // fires — as it is in the app, where activation and the first notification are far apart.
            advanceUntilIdle()
            every { chatNotificationService.getNumNotifications(channel) } returns 3

            notifications.set(mockk(relaxed = true))
            advanceUntilIdle()

            assertEquals(3, discussionChannelModel().unreadCount.value)
        }

    /**
     * The recount is guarded because a throw would cancel the collector for the rest of the process,
     * and a badge frozen that way is indistinguishable on screen from having nothing new.
     */
    @Test
    fun `a recount that throws does not kill the collector`() =
        runTest {
            val notifications = Observable<ChatNotification>()
            every { chatNotificationService.changedNotification } returns notifications
            val channel = activateWithDiscussionChannel()
            advanceUntilIdle()
            var recounts = 0
            every { chatNotificationService.getNumNotifications(channel) } answers {
                recounts++
                if (recounts == 1) throw IllegalStateException("boom") else 5
            }

            notifications.set(mockk(relaxed = true))
            advanceUntilIdle()
            notifications.set(mockk(relaxed = true))
            advanceUntilIdle()

            assertEquals(5, discussionChannelModel().unreadCount.value)
        }

    @Test
    fun `consuming notifications consumes the channel`() =
        runTest {
            val channel = activateWithDiscussionChannel()

            facade.consumeNotifications(channel.id)

            verify(exactly = 1) { chatNotificationService.consume(channel) }
        }

    // Helpers

    private suspend fun activateWithDiscussionChannel(vararg messages: Bisq2CommonPublicChatMessage): Bisq2CommonPublicChatChannel {
        val channel = channel(SubDomain.DISCUSSION_BISQ)
        messages.forEach { channel.chatMessages.add(it) }
        discussionChannels.add(channel)
        facade.activate()
        return channel
    }

    /** A message authored by the node's selected identity, so edit and delete are authorized. */
    private fun mine(id: String) = message(id, authorId = selectedIdentityId())

    private fun banSelectedIdentity() {
        every { bannedUserService.isUserProfileBanned(selectedIdentityId()) } returns true
    }

    private fun selectedIdentityId() = profileId("me")

    private fun rejectionOf(result: Result<Unit>): PublicChatSendRejection {
        val exception = result.exceptionOrNull()
        assertIs<PublicChatSendRefusedException>(exception)
        return exception.rejection
    }

    private fun anyModelReaction() =
        CommonPublicChatMessageReaction(
            id = "r1",
            userProfileId = selectedIdentityId(),
            chatChannelId = "discussion.bisq",
            chatChannelDomain = ChatChannelDomainEnum.DISCUSSION,
            chatMessageId = "m1",
            reactionId = 0,
            date = 1L,
        )

    /**
     * The removals actually remove, so the facade's "did the store really take it back?" re-check sees
     * the accepting store. The two tests that pin the rejected-removal path stub a bare completed
     * future over this.
     */
    private fun stubBroadcasts(service: CommonPublicChatChannelService) {
        every { service.publishChatMessage(any<String>(), any(), any(), any()) } returns broadcast()
        every { service.publishChatMessageReaction(any(), any(), any()) } returns broadcast()
        every { service.publishEditedChatMessage(any(), any(), any()) } answers {
            removeFromItsChannel(firstArg())
            broadcast()
        }
        every { service.deleteChatMessage(any(), any()) } answers {
            removeFromItsChannel(firstArg())
            broadcast()
        }
        every { service.deleteChatMessageReaction(any(), any()) } answers {
            val reaction = firstArg<Bisq2CommonPublicChatMessageReaction>()
            allMessages().forEach { it.chatMessageReactions.remove(reaction) }
            broadcast()
        }
    }

    private fun removeFromItsChannel(message: Bisq2CommonPublicChatMessage) {
        allChannels().forEach { it.chatMessages.remove(message) }
    }

    private fun allChannels() = discussionChannels + supportChannels

    private fun allMessages() = allChannels().flatMap { it.chatMessages }

    private fun broadcast() = CompletableFuture.completedFuture(mockk<BroadcastResult>(relaxed = true))

    private fun discussionChannelModel(): CommonPublicChatChannel = facade.channels.value.single { it.chatChannelDomain == ChatChannelDomainEnum.DISCUSSION }

    private fun discussionMessageIds(): Set<String> =
        discussionChannelModel()
            .chatMessages.value
            .map { it.id }
            .toSet()

    private fun discussionMessage(id: String): CommonPublicChatMessage = discussionChannelModel().chatMessages.value.single { it.id == id }

    private fun channel(subDomain: SubDomain) = Bisq2CommonPublicChatChannel(subDomain.chatChannelDomain, subDomain)

    private fun message(
        id: String,
        authorId: String = profileId("author"),
        date: Long = System.currentTimeMillis(),
        text: String = "text of $id",
    ) = Bisq2CommonPublicChatMessage(
        id,
        ChatChannelDomain.DISCUSSION,
        SubDomain.DISCUSSION_BISQ.channelId,
        authorId,
        Optional.of(text),
        Optional.empty(),
        date,
        false,
        ChatMessageType.TEXT,
    )

    private fun reaction(
        id: String,
        message: Bisq2CommonPublicChatMessage,
        userProfileId: String,
    ) = Bisq2CommonPublicChatMessageReaction(
        id,
        userProfileId,
        message.channelId,
        message.chatChannelDomain,
        message.id,
        0,
        System.currentTimeMillis(),
    )

    private fun profile(id: String): UserProfile = mockk(relaxed = true) { every { this@mockk.id } returns id }

    /** `NetworkDataValidation.validateProfileId` requires exactly 40 characters. */
    private fun profileId(name: String) = name.padEnd(40, '0')

    private fun expiredDate() = System.currentTimeMillis() - COMMON_PUBLIC_CHAT_MESSAGE_TTL - 1_000L
}
