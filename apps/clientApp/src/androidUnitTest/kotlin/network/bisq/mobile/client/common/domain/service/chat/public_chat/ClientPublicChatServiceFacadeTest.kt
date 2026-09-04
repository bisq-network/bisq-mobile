package network.bisq.mobile.client.common.domain.service.chat.public_chat

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.loggerConfigInit
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.client.common.di.clientJson
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketRestApiException
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.common.domain.websocket.subscription.ModificationType
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.data.replicated.chat.common.CommonPublicChatMessage
import network.bisq.mobile.data.replicated.chat.reactions.CommonPublicChatMessageReaction
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.chat.public_chat.PublicChatNotAuthorException
import network.bisq.mobile.data.service.chat.public_chat.PublicChatRemovalRejectedException
import network.bisq.mobile.data.service.chat.public_chat.PublicChatSendRefusedException
import network.bisq.mobile.data.service.chat.public_chat.PublicChatSendRejection
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.service.capabilities.BackendCapabilities
import network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService
import network.bisq.mobile.domain.service.capabilities.Feature
import network.bisq.mobile.domain.service.community.CommunityHubService
import network.bisq.mobile.domain.service.community.CommunitySegment
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClientPublicChatServiceFacadeTest : ClientKoinIntegrationTestBase() {
    private val apiGateway: PublicChatApiGateway = mockk(relaxed = true)
    private val globalUiManager: GlobalUiManager = mockk(relaxed = true)
    private val capabilities = MutableStateFlow(BackendCapabilities(setOf(Feature.PUBLIC_CHAT.key)))
    private val backendCapabilitiesService: BackendCapabilitiesService =
        mockk { every { this@mockk.capabilities } returns this@ClientPublicChatServiceFacadeTest.capabilities }
    private val json = clientJson

    private val me: UserProfileVO = createMockUserProfile("me")
    private val myOtherIdentity: UserProfileVO = createMockUserProfile("me-too")
    private val peer: UserProfileVO = createMockUserProfile("peer")

    private val selectedUserProfile = MutableStateFlow<UserProfileVO?>(me)
    private val userProfileServiceFacade: UserProfileServiceFacade =
        mockk {
            every { this@mockk.selectedUserProfile } returns this@ClientPublicChatServiceFacadeTest.selectedUserProfile
            coEvery { getUserIdentityIds() } returns listOf(me.id, myOtherIdentity.id)
        }

    private var channelsObserver = WebSocketEventObserver()
    private var messagesObserver = WebSocketEventObserver()
    private var reactionsObserver = WebSocketEventObserver()

    private var channelSequence = 0
    private var messageSequence = 0
    private var reactionSequence = 0

    private lateinit var facade: ClientPublicChatServiceFacade

    private val capturedWarnings = mutableListOf<Pair<String, Throwable?>>()
    private val capturedErrors = mutableListOf<Pair<String, Throwable?>>()

    /**
     * Swaps the facade's own logger for a capturing one, set on the instance rather than through
     * `Logger.setLogWriters`: under a Gradle invocation without "debug" in its task names (CI runs
     * `clean test`) `getLogger()` hands out release loggers whose immutable StaticConfig drops
     * warnings and ignores the global writer list, so a global swap captures nothing there.
     * Captures the throwable as well as the message: what leaks the node's response body is the
     * cause chain, not the wording.
     */
    private fun captureLogs() {
        capturedWarnings.clear()
        capturedErrors.clear()
        facade.log =
            Logger(
                loggerConfigInit(
                    object : LogWriter() {
                        override fun log(
                            severity: Severity,
                            message: String,
                            tag: String,
                            throwable: Throwable?,
                        ) {
                            when (severity) {
                                Severity.Warn -> capturedWarnings.add(message to throwable)
                                Severity.Error -> capturedErrors.add(message to throwable)
                                else -> Unit
                            }
                        }
                    },
                ),
                tag = "ClientPublicChatServiceFacade",
            )
    }

    override fun onSetup() {
        ApplicationBootstrapFacade.isDemo = false
        coEvery { apiGateway.subscribeChannels() } returns channelsObserver
        coEvery { apiGateway.subscribeMessages() } returns messagesObserver
        coEvery { apiGateway.subscribeReactions() } returns reactionsObserver
        facade = createFacade()
    }

    /** [enabledSegments] is the Community-Hub rollout config; the shipped Connect value is empty. */
    private fun createFacade(
        enabledSegments: Set<CommunitySegment> = setOf(CommunitySegment.DISCUSSIONS),
    ) = ClientPublicChatServiceFacade(
        apiGateway,
        backendCapabilitiesService,
        userProfileServiceFacade,
        CommunityHubService(
            backendCapabilitiesService = backendCapabilitiesService,
            enabledSegments = enabledSegments,
            requiredFeatures = mapOf(CommunitySegment.DISCUSSIONS to Feature.PUBLIC_CHAT),
            dispatcher = testDispatcher,
        ),
        json,
        globalUiManager,
        testDispatcher,
    )

    override fun onTearDown() {
        try {
            ApplicationBootstrapFacade.isDemo = false
        } finally {
            super.onTearDown()
        }
    }

    // Activation preconditions

    @Test
    fun `is unsupported and subscribes to nothing when the node does not advertise the feature`() =
        runTest {
            capabilities.value = BackendCapabilities(setOf(Feature.CLOSED_TRADES.key))

            assertFalse(facade.isSupported.first())

            facade.activate()
            advanceUntilIdle()

            coVerify(exactly = 0) { apiGateway.subscribeChannels() }
            coVerify(exactly = 0) { apiGateway.subscribeMessages() }
            coVerify(exactly = 0) { apiGateway.subscribeReactions() }
            assertTrue(facade.channels.value.isEmpty())
        }

    /**
     * `ClientApplicationLifecycleService` activates this facade before `ConfigServiceFacade`, which is
     * what fetches the capability manifest — so at activation the set is still the legacy baseline.
     * Reading it once would leave the feature dead for the whole session.
     */
    @Test
    fun `subscribes once the node advertises the feature, even though it was unsupported at activate`() =
        runTest {
            capabilities.value = BackendCapabilities(setOf(Feature.CLOSED_TRADES.key))

            facade.activate()
            advanceUntilIdle()
            coVerify(exactly = 0) { apiGateway.subscribeChannels() }

            capabilities.value = BackendCapabilities(setOf(Feature.PUBLIC_CHAT.key))
            advanceUntilIdle()

            coVerify(exactly = 1) { apiGateway.subscribeChannels() }
            coVerify(exactly = 1) { apiGateway.subscribeMessages() }
            coVerify(exactly = 1) { apiGateway.subscribeReactions() }
        }

    /**
     * The rollout is a precondition too, not only the node's capability. `feature.communityHubSegments.client`
     * ships empty, and the hub's Discussions segment is the only route to a public chat thread —
     * TabContainerPresenter hides the Community tab entirely while no segment is live. Subscribing
     * anyway would pull both channels' full history over Tor for a screen the user cannot open.
     */
    @Test
    fun `subscribes to nothing while the Discussions segment is not live`() =
        runTest {
            val facade = createFacade(enabledSegments = emptySet())

            facade.activate()
            advanceUntilIdle()

            coVerify(exactly = 0) { apiGateway.subscribeChannels() }
            coVerify(exactly = 0) { apiGateway.subscribeMessages() }
            coVerify(exactly = 0) { apiGateway.subscribeReactions() }
            assertTrue(facade.channels.value.isEmpty())
        }

    /** Discussions specifically, not "some segment": Contacts is live in configs that Discussions is not. */
    @Test
    fun `subscribes to nothing when another segment is live but Discussions is not`() =
        runTest {
            val facade = createFacade(enabledSegments = setOf(CommunitySegment.CONTACTS))

            facade.activate()
            advanceUntilIdle()

            coVerify(exactly = 0) { apiGateway.subscribeChannels() }
        }

    /**
     * The other precondition, and the one the node flavour does not have: it reads the selected
     * identity from memory, while `ClientUserProfileServiceFacade` fetches it over the same WebSocket
     * this subscription is racing. Subscribing first would deliver the snapshot — which arrives once
     * per subscribe and is not resent until a reconnect — with nothing to build a message from.
     */
    @Test
    fun `waits for the selected profile before subscribing`() =
        runTest {
            selectedUserProfile.value = null

            facade.activate()
            advanceUntilIdle()
            coVerify(exactly = 0) { apiGateway.subscribeMessages() }

            selectedUserProfile.value = me
            advanceUntilIdle()

            coVerify(exactly = 1) { apiGateway.subscribeMessages() }
        }

    /**
     * A REST call, so it can fail — and left to throw it would kill the activation coroutine, taking
     * the subscriptions with it and leaving the screen on a channel that never arrives.
     */
    @Test
    fun `an identity-load failure falls back to the selected profile and still subscribes`() =
        runTest {
            coEvery { userProfileServiceFacade.getUserIdentityIds() } throws IllegalStateException("boom")

            activateAndSettle()
            emitChannel()
            emitMessage(messageId = "m1", author = me)
            advanceUntilIdle()
            emitMessage(messageId = "m2", author = myOtherIdentity)
            advanceUntilIdle()

            coVerify(exactly = 1) { apiGateway.subscribeMessages() }
            val messages =
                facade.channels.value
                    .single()
                    .chatMessages.value
                    .associateBy { it.id }
            assertTrue(messages.getValue("m1").isMyMessage, "the selected profile is the fallback for what is mine")
            assertFalse(messages.getValue("m2").isMyMessage, "the fallback narrows to the selected profile only")
        }

    /**
     * `getUserIdentityIds()` rethrows the REST exception whose message is the node's response body
     * verbatim, and a throwable passed to the logger prints it. The same body `asDomainFailure` strips
     * from every mutation failure must not come back in through the fallback's log line.
     */
    @Test
    fun `the identity-load failure does not log the node's response body`() =
        runTest {
            captureLogs()
            coEvery { userProfileServiceFacade.getUserIdentityIds() } throws
                IllegalStateException(NODE_RESPONSE_BODY)

            activateAndSettle()

            val warning = capturedWarnings.single { "identity ids" in it.first }
            assertNull(warning.second, "the cause chain would carry the response body into the log")
            assertFalse(NODE_RESPONSE_BODY in warning.first)
        }

    /**
     * The complement of the fallback above: `deactivate()` cancels the coroutine parked inside this
     * REST call, and swallowing that cancellation would log a failure that never happened and consult
     * the fallback on a facade that is going away. Mirrors the node twin's
     * `onFailure { currentCoroutineContext().ensureActive() }`.
     */
    @Test
    fun `deactivating during the identity load cancels it instead of falling back`() =
        runTest {
            coEvery { userProfileServiceFacade.getUserIdentityIds() } coAnswers { awaitCancellation() }
            facade.activate()
            advanceUntilIdle()

            facade.deactivate()
            advanceUntilIdle()

            // Parked inside the call, not never-started — and nothing survived to subscribe.
            coVerify(exactly = 1) { userProfileServiceFacade.getUserIdentityIds() }
            coVerify(exactly = 0) { apiGateway.subscribeChannels() }
            // One read is activate's own wait for the profile; a second one is the fallback running
            // after the call was cancelled.
            verify(exactly = 1) { userProfileServiceFacade.selectedUserProfile }
        }

    // Channels

    @Test
    fun `a channel payload becomes a channel model carrying its unread count`() =
        runTest {
            activateAndSettle()

            emitChannel(unreadCount = 3)
            advanceUntilIdle()

            val channel = facade.channels.value.single()
            assertEquals(CHANNEL_ID, channel.id)
            assertEquals(3L, channel.unreadCount.value)
        }

    /** The raw title mobile builds `"<domain>.<channelTitle>.title"` from, not bisq2's display string. */
    @Test
    fun `the channel title comes from the id rather than from the node's resolved display string`() =
        runTest {
            activateAndSettle()

            emitChannel(title = "Diskuze")
            advanceUntilIdle()

            assertEquals(
                "bisq",
                facade.channels.value
                    .single()
                    .channelTitle,
            )
        }

    @Test
    fun `a re-sent channel updates the existing model rather than replacing it`() =
        runTest {
            activateAndSettle()
            emitChannel(unreadCount = 3)
            emitMessage(messageId = "m1")
            advanceUntilIdle()
            val model = facade.channels.value.single()

            emitChannel(unreadCount = 0)
            advanceUntilIdle()

            assertTrue(model === facade.channels.value.single(), "the channel model must survive an unread-count update")
            assertEquals(0L, model.unreadCount.value)
            assertEquals(1, model.chatMessages.value.size)
        }

    /**
     * The node flavour sorts by domain ordinal so the list a presenter reads never depends on map
     * iteration order; both flavours feed the same `StateFlow` contract, so this one makes the same
     * promise.
     */
    @Test
    fun `channels are published in domain order regardless of arrival order`() =
        runTest {
            activateAndSettle()

            emitChannel(id = "support.support", domain = ChatChannelDomainEnum.SUPPORT)
            advanceUntilIdle()
            emitChannel()
            advanceUntilIdle()

            assertEquals(listOf(CHANNEL_ID, "support.support"), facade.channels.value.map { it.id })
        }

    /**
     * The shape production delivers first — `WebSocketClientImpl.subscribe` synthesises the initial
     * snapshot client-side as REPLACE for every topic — pinned against the mutation that matters: a
     * REPLACE must upsert the live model, not rebuild it, or every reconnect would strand the open
     * screen. Absence — the one thing a REPLACE says that an ADDED cannot — carries no information
     * here, because bisq2 registers one channel per domain at startup and never adds or drops one.
     */
    @Test
    fun `a REPLACE channel snapshot upserts the live model rather than rebuilding it`() =
        runTest {
            activateAndSettle()
            emitChannel(unreadCount = 3)
            emitMessage(messageId = "m1")
            advanceUntilIdle()
            val model = facade.channels.value.single()

            emitChannel(unreadCount = 2, modificationType = ModificationType.REPLACE)
            advanceUntilIdle()

            assertTrue(model === facade.channels.value.single(), "a REPLACE must upsert, not rebuild")
            assertEquals(2L, model.unreadCount.value)
            assertEquals(1, model.chatMessages.value.size)
        }

    @Test
    fun `messages and reactions merge into the channel regardless of arrival order`() =
        runTest {
            activateAndSettle()

            // Reaction first, then the message it names, and only then the channel that holds both.
            emitReaction(messageId = "m1", reactionId = 1)
            emitMessage(messageId = "m1", text = "hi")
            emitChannel()
            advanceUntilIdle()

            val message = singleMessage()
            assertEquals("hi", message.text)
            assertEquals(listOf(1), message.chatReactions.value.map { it.reactionId })
        }

    /**
     * `WebSocketEventPayload.from` logs and rethrows, and the three collectors are children of one
     * activation coroutine — so one undecodable payload, unguarded, would take channels, messages and
     * reactions down together, silently, for the rest of the session. On Connect that is one node
     * round-trip away: a newer bisq2 adding an enum constant is enough. The node twin guards its
     * collector for the same reason.
     */
    @Test
    fun `an undecodable payload does not kill the subscriptions`() =
        runTest {
            activateAndSettle()

            messagesObserver.setEvent(
                WebSocketEvent(
                    topic = Topic.PUBLIC_CHAT_MESSAGES,
                    subscriberId = "messages",
                    deferredPayload = "not json",
                    modificationType = ModificationType.ADDED,
                    sequenceNumber = messageSequence++,
                ),
            )
            advanceUntilIdle()

            emitChannel()
            emitMessage(messageId = "m1")
            advanceUntilIdle()

            assertEquals("m1", singleMessage().id)
        }

    /**
     * An undecodable payload is skipped by [WebSocketEventPayload.from] returning null — its own
     * log line is pinned payload-free by `WebSocketEventPayloadTest` — so the facade must neither
     * log the payload itself nor lose the collector: the next valid event on the topic still lands.
     */
    @Test
    fun `an undecodable event is skipped and the collector keeps collecting`() =
        runTest {
            captureLogs()
            activateAndSettle()

            messagesObserver.setEvent(
                WebSocketEvent(
                    topic = Topic.PUBLIC_CHAT_MESSAGES,
                    subscriberId = "messages",
                    deferredPayload = NODE_RESPONSE_BODY,
                    modificationType = ModificationType.ADDED,
                    sequenceNumber = messageSequence++,
                ),
            )
            advanceUntilIdle()

            emitChannel()
            emitMessage(messageId = "m1")
            advanceUntilIdle()

            assertEquals("m1", singleMessage().id)
            assertTrue(capturedWarnings.none { NODE_RESPONSE_BODY in it.first })
        }

    /**
     * The consume-notifications failure is this facade's one error-severity log line, and a
     * [WebSocketRestApiException]'s message is the node's response body verbatim — logged as a
     * throwable it would print. The class and the HTTP status are what the line keeps.
     */
    @Test
    fun `a failed notification consumption does not log the node's response body`() =
        runTest {
            captureLogs()
            coEvery { apiGateway.consumeNotifications(CHANNEL_ID) } returns
                Result.failure(WebSocketRestApiException(HttpStatusCode.Forbidden, NODE_RESPONSE_BODY))

            facade.consumeNotifications(CHANNEL_ID)

            val error = capturedErrors.single { "consume notifications" in it.first }
            assertNull(error.second, "a logged throwable prints the response body it carries")
            assertFalse(NODE_RESPONSE_BODY in error.first)
            assertTrue("WebSocketRestApiException" in error.first, "the class is what the log keeps")
            assertTrue("HTTP 403" in error.first, "the status is what the log keeps")
        }

    // Removal, which is where this departs from private chat

    @Test
    fun `a removed message leaves the channel`() =
        runTest {
            activateAndSettle()
            emitChannel()
            emitMessage(messageId = "m1")
            advanceUntilIdle()
            assertEquals("m1", singleMessage().id)

            emitMessage(messageId = "m1", modificationType = ModificationType.REMOVED)
            advanceUntilIdle()

            assertTrue(
                facade.channels.value
                    .single()
                    .chatMessages.value
                    .isEmpty(),
            )
        }

    /**
     * bisq2 pushes no `REMOVED` for the reactions of a removed message — the client is expected to drop
     * them with it. Without the cascade they outlive the message in the maps and come back the moment
     * the same id is seen again.
     */
    @Test
    fun `a removed message takes its reactions with it`() =
        runTest {
            activateAndSettle()
            emitChannel()
            emitMessage(messageId = "m1")
            emitReaction(messageId = "m1", reactionId = 1)
            advanceUntilIdle()
            assertEquals(listOf(1), singleMessage().chatReactions.value.map { it.reactionId })

            emitMessage(messageId = "m1", modificationType = ModificationType.REMOVED)
            advanceUntilIdle()
            emitMessage(messageId = "m1")
            advanceUntilIdle()

            assertTrue(singleMessage().chatReactions.value.isEmpty())
        }

    /**
     * An edit is a removal plus a new message, with no field linking the two ids. The date is the
     * ORIGINAL one, which is why `wasEdited` has to carry the fact instead.
     */
    @Test
    fun `an edit replaces the message and keeps the original date`() =
        runTest {
            activateAndSettle()
            emitChannel()
            emitMessage(messageId = "m1", text = "typo", date = 1000L)
            advanceUntilIdle()

            emitMessage(messageId = "m1", modificationType = ModificationType.REMOVED)
            advanceUntilIdle()
            emitMessage(messageId = "m2", text = "fixed", date = 1000L, wasEdited = true)
            advanceUntilIdle()

            val message = singleMessage()
            assertEquals("fixed", message.text)
            assertEquals(1000L, message.date)
            assertTrue(message.wasEdited)
        }

    /** Public reactions have no `isRemoved`: the removal carries the original, and it is deleted by id. */
    @Test
    fun `a removed reaction is matched by id`() =
        runTest {
            activateAndSettle()
            emitChannel()
            emitMessage(messageId = "m1")
            emitReaction(messageId = "m1", reactionId = 1, id = "r1")
            advanceUntilIdle()
            assertEquals(listOf(1), singleMessage().chatReactions.value.map { it.reactionId })

            emitReaction(messageId = "m1", reactionId = 1, id = "r1", modificationType = ModificationType.REMOVED)
            advanceUntilIdle()

            assertTrue(singleMessage().chatReactions.value.isEmpty())
        }

    /**
     * bisq2 pushes a removal for a reaction it may never have pushed — one whose sender was banned
     * after the fact — and says so in `PublicChatReactionsWebSocketService`: it counts on the client
     * deleting by id and moving on.
     */
    @Test
    fun `a removal for a reaction that was never received is a no-op`() =
        runTest {
            activateAndSettle()
            emitChannel()
            emitMessage(messageId = "m1")
            emitReaction(messageId = "m1", reactionId = 1, id = "r1")
            advanceUntilIdle()

            emitReaction(messageId = "m1", reactionId = 2, id = "never-seen", modificationType = ModificationType.REMOVED)
            advanceUntilIdle()

            assertEquals(listOf(1), singleMessage().chatReactions.value.map { it.reactionId })
        }

    /**
     * The message twin of the reaction no-op above: TTL pruning arrives in bursts, and a client that
     * subscribed after a message expired still gets its REMOVED.
     */
    @Test
    fun `a removal for a message that was never received is a no-op`() =
        runTest {
            activateAndSettle()
            emitChannel()
            emitMessage(messageId = "m1")
            advanceUntilIdle()

            emitMessage(messageId = "never-seen", modificationType = ModificationType.REMOVED)
            advanceUntilIdle()

            assertEquals("m1", singleMessage().id)
        }

    /**
     * The reason this facade has a message REPLACE branch where private chat has none. bisq2 documents
     * three windows in which a `REMOVED` is never pushed at all — an author banned or rate limited when
     * the removal is offered, a profile pruned before the DTO can be built — and the resubscribe
     * snapshot is the only thing that repairs them.
     *
     * It reaches the collector at all because `WebSocketEventObserver` starts its sequence at -1 and
     * `resetSequence()` returns it there: the snapshot arrives as sequence 0, which has to be strictly
     * greater. Were that -1 ever a 0, the repair would go quiet with nothing else to notice.
     */
    @Test
    fun `a resubscribe snapshot drops a message whose removal was never pushed`() =
        runTest {
            activateAndSettle()
            emitChannel()
            emitMessage(messageId = "m1")
            advanceUntilIdle()
            emitMessage(messageId = "m2")
            advanceUntilIdle()
            assertEquals(
                2,
                facade.channels.value
                    .single()
                    .chatMessages.value.size,
            )

            emitMessageSnapshot(listOf("m2"))
            advanceUntilIdle()

            assertEquals("m2", singleMessage().id, "the snapshot is authoritative about absence")
        }

    @Test
    fun `a resubscribe snapshot drops a reaction whose removal was never pushed`() =
        runTest {
            activateAndSettle()
            emitChannel()
            emitMessage(messageId = "m1")
            emitReaction(messageId = "m1", reactionId = 1)
            advanceUntilIdle()
            assertEquals(1, singleMessage().chatReactions.value.size)

            emitReactionSnapshot()
            advanceUntilIdle()

            assertTrue(singleMessage().chatReactions.value.isEmpty())
        }

    /**
     * The two topics snapshot independently, and nothing orders them. Clearing the reactions from the
     * messages branch wiped a reactions snapshot that had already landed, and neither comes back —
     * each arrives once per subscribe.
     */
    @Test
    fun `a message snapshot keeps the reactions a reaction snapshot already delivered`() =
        runTest {
            activateAndSettle()
            emitChannel()
            emitMessage(messageId = "m1")
            advanceUntilIdle()

            emitReactionSnapshot(listOf("m1" to 1))
            advanceUntilIdle()
            emitMessageSnapshot(listOf("m1"))
            advanceUntilIdle()

            assertEquals(listOf(1), singleMessage().chatReactions.value.map { it.reactionId })
        }

    /** The other half: a reaction whose message the snapshot dropped has nothing left to hang on. */
    @Test
    fun `a message snapshot drops the reactions of a message it no longer lists`() =
        runTest {
            activateAndSettle()
            emitChannel()
            emitMessage(messageId = "m1")
            advanceUntilIdle()
            emitReaction(messageId = "m1", reactionId = 1)
            advanceUntilIdle()
            assertEquals(listOf(1), singleMessage().chatReactions.value.map { it.reactionId })

            emitMessageSnapshot(emptyList())
            advanceUntilIdle()
            emitMessage(messageId = "m1")
            advanceUntilIdle()

            assertTrue(singleMessage().chatReactions.value.isEmpty())
        }

    // Authorship

    /**
     * bisq2 authorises an edit or a delete against ANY of my identities, not only the selected one, and
     * `isMyMessage` is what gates those two menu items. The DTO carries no such flag — the node
     * computes it from its `UserIdentityService`, and here it comes from the cached identity ids.
     */
    @Test
    fun `a message written by an identity of mine that is not selected is still mine`() =
        runTest {
            activateAndSettle()
            emitChannel()
            emitMessage(messageId = "m1", author = myOtherIdentity)
            advanceUntilIdle()

            assertTrue(singleMessage().isMyMessage)
        }

    @Test
    fun `a message written by someone else is not mine`() =
        runTest {
            activateAndSettle()
            emitChannel()
            emitMessage(messageId = "m1", author = peer)
            advanceUntilIdle()

            assertFalse(singleMessage().isMyMessage)
        }

    /**
     * A switch moves both inputs a message is built with: which messages are mine and whose reaction
     * is whose. The identity list itself can change with it — an identity created while another
     * profile was selected only becomes visible through the reload.
     */
    @Test
    fun `a profile switch reloads the identities and rebuilds authorship`() =
        runTest {
            coEvery { userProfileServiceFacade.getUserIdentityIds() } returns listOf(me.id)
            activateAndSettle()
            emitChannel()
            emitMessage(messageId = "m1", author = myOtherIdentity)
            advanceUntilIdle()
            assertFalse(singleMessage().isMyMessage)

            coEvery { userProfileServiceFacade.getUserIdentityIds() } returns listOf(me.id, myOtherIdentity.id)
            selectedUserProfile.value = myOtherIdentity
            advanceUntilIdle()

            assertTrue(singleMessage().isMyMessage, "the switch must reload the identity ids and rebuild")
        }

    // Mutations

    /**
     * `sendChatMessage` and `addChatMessageReaction` name the selected identity explicitly, so the
     * mutation goes out as the profile the user is looking at even if the node's own selection moved
     * underneath; a reaction removal names the reaction's own owner, which bisq2 requires.
     */
    @Test
    fun `a mutation names the selected identity, a reaction removal its owner`() =
        runTest {
            coEvery { apiGateway.sendTextMessage(any(), any(), any(), any()) } returns Result.success(Unit)
            coEvery { apiGateway.addChatMessageReaction(any(), any(), any(), any()) } returns Result.success(Unit)
            coEvery { apiGateway.removeChatMessageReaction(any(), any(), any()) } returns Result.success(Unit)
            activateAndSettle()

            assertTrue(facade.sendChatMessage(CHANNEL_ID, "hello", null).isSuccess)
            assertTrue(facade.addChatMessageReaction(CHANNEL_ID, "m1", ReactionEnum.THUMBS_UP).isSuccess)
            assertTrue(facade.removeChatMessageReaction(CHANNEL_ID, "m1", reaction(ownerId = myOtherIdentity.id)).isSuccess)

            coVerify(exactly = 1) { apiGateway.sendTextMessage(CHANNEL_ID, "hello", null, me.id) }
            coVerify(exactly = 1) { apiGateway.addChatMessageReaction(CHANNEL_ID, "m1", ReactionEnum.THUMBS_UP, me.id) }
            coVerify(exactly = 1) {
                apiGateway.removeChatMessageReaction(CHANNEL_ID, "m1", match { it.userProfileId == myOtherIdentity.id })
            }
        }

    /**
     * The one Unit-returning mutation: a failure is invisible to the caller by construction, logged
     * and swallowed, so what there is to pin is that the delegation happens and a failure does not
     * throw.
     */
    @Test
    fun `consuming notifications delegates to the gateway and survives a failure`() =
        runTest {
            coEvery { apiGateway.consumeNotifications(CHANNEL_ID) } returns
                Result.failure(WebSocketRestApiException(HttpStatusCode.InternalServerError, "boom"))
            activateAndSettle()

            facade.consumeNotifications(CHANNEL_ID)

            coVerify(exactly = 1) { apiGateway.consumeNotifications(CHANNEL_ID) }
        }

    // Failure translation

    /**
     * Three different 403s share one status and are told apart only by the body. `WebSocketApiClient`
     * keeps the value of an `{"error": …}` envelope's key, so bisq2's permission filter arrives as the
     * bare token while the endpoint's own refusal arrives as prose.
     */
    @Test
    fun `a withheld permission is not reported as someone else's message`() =
        runTest {
            coEvery { apiGateway.deleteMessage(any(), any()) } returnsMany
                listOf(
                    Result.failure(WebSocketRestApiException(HttpStatusCode.Forbidden, "permission_not_granted")),
                    Result.failure(WebSocketRestApiException(HttpStatusCode.Forbidden, "")),
                )

            repeat(2) {
                val exception = facade.deleteChatMessage(CHANNEL_ID, "m1").exceptionOrNull()
                assertFalse(exception is PublicChatNotAuthorException, "the permission filter is not an authorship refusal")
                assertTrue(exception is IllegalStateException)
            }
        }

    @Test
    fun `editing someone else's message is reported as an authorship refusal`() =
        runTest {
            coEvery { apiGateway.editMessage(any(), any(), any()) } returns
                Result.failure(WebSocketRestApiException(HttpStatusCode.Forbidden, "Only the author can edit a message."))

            val exception = facade.editChatMessage(CHANNEL_ID, "m1", "text").exceptionOrNull()

            assertTrue(exception is PublicChatNotAuthorException)
        }

    @Test
    fun `a banned profile is reported as a refusal naming the ban`() =
        runTest {
            coEvery { apiGateway.sendTextMessage(any(), any(), any(), any()) } returns
                Result.failure(
                    WebSocketRestApiException(
                        HttpStatusCode.Conflict,
                        """{"rejection":"MY_PROFILE_BANNED","message":"Your user profile is banned."}""",
                    ),
                )

            val exception = facade.sendChatMessage(CHANNEL_ID, "hello", null).exceptionOrNull()

            assertEquals(PublicChatSendRejection.MY_PROFILE_BANNED, (exception as PublicChatSendRefusedException).rejection)
        }

    /**
     * The rate limit is the status and nothing else: unlike the 409 it carries no `SendRefusedResponse`
     * to read the reason out of.
     */
    @Test
    fun `a rate limit is reported as a refusal rather than a generic failure`() =
        runTest {
            coEvery { apiGateway.sendTextMessage(any(), any(), any(), any()) } returns
                Result.failure(WebSocketRestApiException(HttpStatusCode.TooManyRequests, "Rate limit exceeded. Wait before sending again."))

            val exception = facade.sendChatMessage(CHANNEL_ID, "hello", null).exceptionOrNull()

            assertEquals(PublicChatSendRejection.RATE_LIMIT_EXCEEDED, (exception as PublicChatSendRefusedException).rejection)
        }

    /**
     * bisq2 shares `SendRefusedResponse` with private chat, so it can name a rejection a public channel
     * has no room for. That, and a body this build cannot decode, is UNKNOWN rather than a guess.
     */
    @Test
    fun `a refusal without a rejection this build knows is still a refusal`() =
        runTest {
            coEvery { apiGateway.sendTextMessage(any(), any(), any(), any()) } returnsMany
                listOf(
                    Result.failure(WebSocketRestApiException(HttpStatusCode.Conflict, """{"rejection":"PEER_BANNED","message":"x"}""")),
                    Result.failure(WebSocketRestApiException(HttpStatusCode.Conflict, "Your user profile is banned.")),
                )

            repeat(2) {
                val exception = facade.sendChatMessage(CHANNEL_ID, "hello", null).exceptionOrNull()
                assertEquals(PublicChatSendRejection.UNKNOWN, (exception as PublicChatSendRefusedException).rejection)
            }
        }

    /**
     * The removal went out to the network but the local store refused it, so the message is gone
     * everywhere except here. bisq2 answers 500 for an unexpected error too, which is why the message
     * is matched and not only the status.
     */
    @Test
    fun `a removal the node's store refused is reported as such`() =
        runTest {
            coEvery { apiGateway.deleteMessage(any(), any()) } returns
                Result.failure(
                    WebSocketRestApiException(HttpStatusCode.InternalServerError, "The message could not be removed locally."),
                )

            val exception = facade.deleteChatMessage(CHANNEL_ID, "m1").exceptionOrNull()

            assertTrue(exception is PublicChatRemovalRejectedException)
        }

    @Test
    fun `an unexpected server error is not mistaken for a refused removal`() =
        runTest {
            coEvery { apiGateway.deleteMessage(any(), any()) } returns
                Result.failure(WebSocketRestApiException(HttpStatusCode.InternalServerError, "Unexpected error"))

            val exception = facade.deleteChatMessage(CHANNEL_ID, "m1").exceptionOrNull()

            assertFalse(exception is PublicChatRemovalRejectedException)
        }

    /**
     * The same body on a send must not become one either: `removalTarget` is a parameter precisely
     * because a 500 means different things per call site, and a send removes nothing.
     */
    @Test
    fun `a removal-flavoured 500 on a send is not a refused removal`() =
        runTest {
            coEvery { apiGateway.sendTextMessage(any(), any(), any(), any()) } returns
                Result.failure(
                    WebSocketRestApiException(HttpStatusCode.InternalServerError, "The message could not be removed locally."),
                )

            val exception = facade.sendChatMessage(CHANNEL_ID, "hello", null).exceptionOrNull()

            assertFalse(exception is PublicChatRemovalRejectedException)
        }

    /** An edit is a removal plus a publish, and its rejection names what was lost: the original. */
    @Test
    fun `an edit whose removal was refused names the original message`() =
        runTest {
            coEvery { apiGateway.editMessage(any(), any(), any()) } returns
                Result.failure(
                    WebSocketRestApiException(HttpStatusCode.InternalServerError, "The message could not be removed locally."),
                )

            val exception = facade.editChatMessage(CHANNEL_ID, "m1", "text").exceptionOrNull()

            assertTrue(exception is PublicChatRemovalRejectedException)
            assertTrue(exception.message.orEmpty().contains("original message"))
        }

    /** A transport failure is not an API answer; it passes through untranslated. */
    @Test
    fun `a non-API failure passes through untranslated`() =
        runTest {
            val transport = RuntimeException("socket closed")
            coEvery { apiGateway.sendTextMessage(any(), any(), any(), any()) } returns Result.failure(transport)

            assertSame(transport, facade.sendChatMessage(CHANNEL_ID, "hello", null).exceptionOrNull())
        }

    /**
     * A public channel id is safe to log — `discussion.bisq` names a room, not two people — but a 400
     * quotes back the profile id it could not resolve, and `handleError` logs `message`.
     */
    @Test
    fun `other failures keep the status but not the node's body`() =
        runTest {
            // A literal, unmistakable id: `me.id` is the two-letter "me", whose absence from an
            // English sentence proves nothing.
            val profileId = "profile-9f2c41"
            coEvery { apiGateway.sendTextMessage(any(), any(), any(), any()) } returns
                Result.failure(
                    WebSocketRestApiException(HttpStatusCode.BadRequest, "No user identity found for senderUserProfileId: $profileId"),
                )

            val exception = facade.sendChatMessage(CHANNEL_ID, "hello", null).exceptionOrNull()

            assertNotNull(exception)
            assertFalse(exception.message.orEmpty().contains(profileId), "the node's body names a profile")
            assertTrue(exception.message.orEmpty().contains("400"))
            assertNull(exception.cause, "the cause chain would carry the body into the log")
        }

    // Demo mode

    @Test
    fun `no mutation reaches the node in demo mode`() =
        runTest {
            ApplicationBootstrapFacade.isDemo = true

            assertTrue(facade.sendChatMessage(CHANNEL_ID, "hello", null).isSuccess)
            assertTrue(facade.editChatMessage(CHANNEL_ID, "m1", "text").isSuccess)
            assertTrue(facade.deleteChatMessage(CHANNEL_ID, "m1").isSuccess)
            assertTrue(facade.addChatMessageReaction(CHANNEL_ID, "m1", ReactionEnum.THUMBS_UP).isSuccess)
            assertTrue(facade.removeChatMessageReaction(CHANNEL_ID, "m1", reaction()).isSuccess)
            facade.consumeNotifications(CHANNEL_ID)

            coVerify(exactly = 0) { apiGateway.sendTextMessage(any(), any(), any(), any()) }
            coVerify(exactly = 0) { apiGateway.editMessage(any(), any(), any()) }
            coVerify(exactly = 0) { apiGateway.deleteMessage(any(), any()) }
            coVerify(exactly = 0) { apiGateway.addChatMessageReaction(any(), any(), any(), any()) }
            coVerify(exactly = 0) { apiGateway.removeChatMessageReaction(any(), any(), any()) }
            coVerify(exactly = 0) { apiGateway.consumeNotifications(any()) }
            verify(exactly = 6) { globalUiManager.showSnackbar(any(), any(), any(), any()) }
        }

    // Teardown

    @Test
    fun `deactivate clears the channels it was serving`() =
        runTest {
            activateAndSettle()
            emitChannel()
            emitMessage(messageId = "m1")
            advanceUntilIdle()
            assertEquals(1, facade.channels.value.size)

            facade.deactivate()
            advanceUntilIdle()

            assertTrue(facade.channels.value.isEmpty())
        }

    /**
     * Background/foreground really does deactivate + activate this facade through the lifecycle
     * service. Fresh observers with fresh sequence gates, as a real resubscribe hands out — and the
     * empty message set doubles as proof that deactivate cleared the internal DTO maps, not only the
     * published list: a zombie `m1` would resurface through the rebuild here.
     */
    @Test
    fun `activating again after a deactivate resubscribes and starts clean`() =
        runTest {
            activateAndSettle()
            emitChannel()
            emitMessage(messageId = "m1")
            advanceUntilIdle()
            facade.deactivate()
            advanceUntilIdle()

            channelsObserver = WebSocketEventObserver()
            messagesObserver = WebSocketEventObserver()
            reactionsObserver = WebSocketEventObserver()
            channelSequence = 0
            messageSequence = 0
            reactionSequence = 0
            coEvery { apiGateway.subscribeChannels() } returns channelsObserver
            coEvery { apiGateway.subscribeMessages() } returns messagesObserver
            coEvery { apiGateway.subscribeReactions() } returns reactionsObserver

            activateAndSettle()
            emitChannel()
            advanceUntilIdle()

            val channel = facade.channels.value.single()
            assertTrue(channel.chatMessages.value.isEmpty(), "a message from the previous activation must not resurface")
        }

    // Helpers. Settle (`advanceUntilIdle`) between two emissions on the SAME observer:
    // `WebSocketEventObserver` publishes into a `MutableStateFlow`, which conflates, so a second
    // event set before the collector runs replaces the first — and the sequence gate has already
    // moved past it, so it is gone for good. Emissions on different observers can be batched.

    private fun singleMessage(): CommonPublicChatMessage =
        facade.channels.value
            .single()
            .chatMessages.value
            .single()

    private fun reaction(ownerId: String = me.id) =
        CommonPublicChatMessageReaction(
            id = "r1",
            userProfileId = ownerId,
            chatChannelId = CHANNEL_ID,
            chatChannelDomain = ChatChannelDomainEnum.DISCUSSION,
            chatMessageId = "m1",
            reactionId = ReactionEnum.THUMBS_UP.ordinal,
            date = 1L,
        )

    private suspend fun TestScope.activateAndSettle() {
        facade.activate()
        advanceUntilIdle()
    }

    private fun channelDto(
        unreadCount: Long,
        title: String,
        id: String = CHANNEL_ID,
        domain: ChatChannelDomainEnum = ChatChannelDomainEnum.DISCUSSION,
    ) = CommonPublicChatChannelDto(
        id = id,
        chatChannelDomain = domain,
        title = title,
        description = "Public channel for discussions",
        unreadCount = unreadCount,
    )

    private fun messageDto(
        messageId: String,
        text: String,
        author: UserProfileVO,
        date: Long,
        wasEdited: Boolean,
    ) = CommonPublicChatMessageDto(
        messageId = messageId,
        channelId = CHANNEL_ID,
        authorUserProfileId = author.id,
        authorUserProfile = author,
        text = text,
        citation = null,
        citationAuthorUserProfile = null,
        date = date,
        chatMessageType = ChatMessageTypeEnum.TEXT,
        wasEdited = wasEdited,
        chatMessageReactions = emptySet(),
    )

    private fun reactionDto(
        messageId: String,
        reactionId: Int,
        id: String,
    ) = CommonPublicChatMessageReactionDto(
        id = id,
        senderUserProfileId = peer.id,
        senderUserProfile = peer,
        chatChannelId = CHANNEL_ID,
        chatChannelDomain = ChatChannelDomainEnum.DISCUSSION,
        chatMessageId = messageId,
        reactionId = reactionId,
        date = 1L,
    )

    private suspend fun emitChannel(
        unreadCount: Long = 0,
        title: String = "bisq",
        id: String = CHANNEL_ID,
        domain: ChatChannelDomainEnum = ChatChannelDomainEnum.DISCUSSION,
        modificationType: ModificationType = ModificationType.ADDED,
    ) = channelsObserver.setEvent(
        WebSocketEvent(
            topic = Topic.PUBLIC_CHAT_CHANNELS,
            subscriberId = "channels",
            deferredPayload = json.encodeToString(listOf(channelDto(unreadCount, title, id, domain))),
            modificationType = modificationType,
            sequenceNumber = channelSequence++,
        ),
    )

    private suspend fun emitMessage(
        messageId: String,
        text: String = "hi",
        author: UserProfileVO = peer,
        date: Long = 1234L,
        wasEdited: Boolean = false,
        modificationType: ModificationType = ModificationType.ADDED,
    ) = messagesObserver.setEvent(
        WebSocketEvent(
            topic = Topic.PUBLIC_CHAT_MESSAGES,
            subscriberId = "messages",
            deferredPayload = json.encodeToString(listOf(messageDto(messageId, text, author, date, wasEdited))),
            modificationType = modificationType,
            sequenceNumber = messageSequence++,
        ),
    )

    private suspend fun emitMessageSnapshot(messageIds: List<String>) =
        messagesObserver.setEvent(
            WebSocketEvent(
                topic = Topic.PUBLIC_CHAT_MESSAGES,
                subscriberId = "messages",
                deferredPayload = json.encodeToString(messageIds.map { messageDto(it, "hi", peer, 1234L, false) }),
                modificationType = ModificationType.REPLACE,
                sequenceNumber = messageSequence++,
            ),
        )

    private suspend fun emitReaction(
        messageId: String,
        reactionId: Int,
        id: String = "reaction-$reactionId",
        modificationType: ModificationType = ModificationType.ADDED,
    ) = reactionsObserver.setEvent(
        WebSocketEvent(
            topic = Topic.PUBLIC_CHAT_REACTIONS,
            subscriberId = "reactions",
            deferredPayload = json.encodeToString(listOf(reactionDto(messageId, reactionId, id))),
            modificationType = modificationType,
            sequenceNumber = reactionSequence++,
        ),
    )

    private suspend fun emitReactionSnapshot(reactions: List<Pair<String, Int>> = emptyList()) =
        reactionsObserver.setEvent(
            WebSocketEvent(
                topic = Topic.PUBLIC_CHAT_REACTIONS,
                subscriberId = "reactions",
                deferredPayload =
                    json.encodeToString(
                        reactions.map { (messageId, reactionId) -> reactionDto(messageId, reactionId, "reaction-$reactionId") },
                    ),
                modificationType = ModificationType.REPLACE,
                sequenceNumber = reactionSequence++,
            ),
        )

    private companion object {
        const val CHANNEL_ID = "discussion.bisq"

        /** Stands in for whatever the node echoes back; recognisable if it reaches a log line. */
        const val NODE_RESPONSE_BODY = "profileId=super-secret-identity"
    }
}
