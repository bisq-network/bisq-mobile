package network.bisq.mobile.client.common.domain.service.chat.public_chat

import co.touchlab.kermit.Logger
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.service.chat.private_chat.SendRefusedResponse
import network.bisq.mobile.client.common.domain.service.chat.private_chat.SendRejectionDto
import network.bisq.mobile.client.common.domain.util.notifyIfDemoModeRestricted
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketRestApiException
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.common.domain.websocket.subscription.ModificationType
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.data.replicated.chat.Citation
import network.bisq.mobile.data.replicated.chat.common.CommonPublicChatChannel
import network.bisq.mobile.data.replicated.chat.reactions.CommonPublicChatMessageReaction
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.data.service.chat.public_chat.PublicChatNotAuthorException
import network.bisq.mobile.data.service.chat.public_chat.PublicChatRemovalRejectedException
import network.bisq.mobile.data.service.chat.public_chat.PublicChatSendRefusedException
import network.bisq.mobile.data.service.chat.public_chat.PublicChatSendRejection
import network.bisq.mobile.data.service.chat.public_chat.PublicChatServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService
import network.bisq.mobile.domain.service.capabilities.Feature
import network.bisq.mobile.domain.service.community.CommunityHubService
import network.bisq.mobile.domain.service.community.CommunitySegment
import network.bisq.mobile.domain.utils.getLogger
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager

/**
 * The Discussions and Support channels over the trusted node's API.
 *
 * Gated on [Feature.PUBLIC_CHAT] exactly like private chat: a node without it serves none of these
 * endpoints, and subscribing anyway costs a subscribe timeout per topic rather than failing fast.
 *
 * Three topics feed this, all subscribed unscoped. Where it departs from the private sibling is
 * removal: a public message really disappears — a delete, the removal half of an edit, or the P2P
 * store pruning its 10-day TTL in bursts — and bisq2 documents three windows where the `REMOVED` is
 * never pushed at all. So the snapshot that arrives on every (re)subscribe is authoritative about
 * absence, and processing it is what repairs those windows. Private chat needs none of that, because
 * a DM is never removed.
 */
class ClientPublicChatServiceFacade(
    private val apiGateway: PublicChatApiGateway,
    private val backendCapabilitiesService: BackendCapabilitiesService,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val communityHubService: CommunityHubService,
    private val json: Json,
    private val globalUiManager: GlobalUiManager,
    // Injectable so tests can drive the subscription collectors on their virtual-time dispatcher.
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ServiceFacade(),
    PublicChatServiceFacade {
    // var so the log-assertion tests can substitute a capturing logger. Under a Gradle invocation
    // without "debug" in its task names (CI runs `clean test`) getLogger() hands out release
    // loggers whose immutable StaticConfig drops warnings and ignores Logger.setLogWriters, so a
    // global writer swap captures nothing there (see Logging.kt).
    override var log: Logger = getLogger("ClientPublicChatServiceFacade")

    override val isSupported: Flow<Boolean>
        get() =
            backendCapabilitiesService.capabilities
                .map { it.isSupported(Feature.PUBLIC_CHAT) }
                .distinctUntilChanged()

    private val _channels = MutableStateFlow<List<CommonPublicChatChannel>>(emptyList())
    override val channels: StateFlow<List<CommonPublicChatChannel>> = _channels.asStateFlow()

    // All mutable state below is guarded by this: the three subscriptions collect concurrently.
    private val stateMutex = Mutex()
    private val channelModelsById: MutableMap<String, CommonPublicChatChannel> = linkedMapOf()
    private val messageDtosById: MutableMap<String, CommonPublicChatMessageDto> = mutableMapOf()

    // Indexed by message rather than kept as one flat set, unlike private chat: a public channel holds
    // thousands of messages against a DM's handful, and a flat set turns every reaction event into a
    // scan of all of them.
    private val reactionDtosByMessageId: MutableMap<String, MutableMap<String, CommonPublicChatMessageReactionDto>> = mutableMapOf()

    /**
     * Every profile id that is mine, which is what authorises Edit and Delete — bisq2 resolves the
     * author against ANY of my identities, not only the selected one.
     *
     * Cached because the place that needs it is not a suspending function, while
     * [UserProfileServiceFacade.getUserIdentityIds] is a REST round trip. Refreshed whenever the
     * selected profile changes, which is the closest signal to "the user touched their identities".
     */
    private var myIdentityIds: Set<String> = emptySet()

    override suspend fun activate() {
        super<ServiceFacade>.activate()

        serviceScope.launch(defaultDispatcher) {
            // Awaited rather than read as a snapshot: this facade is activated before
            // ConfigServiceFacade, which is what fetches the /config/capabilities manifest, so at this
            // point the capability set is still the legacy baseline and a snapshot would always say
            // "unsupported" — silently leaving the feature dead for the whole session.
            backendCapabilitiesService.capabilities.first { it.isSupported(Feature.PUBLIC_CHAT) }

            // The second precondition: the rollout, not just the node. The hub's Discussions segment
            // is the only route to a public chat thread, and TabContainerPresenter hides the Community
            // tab outright while no segment is live — so without this a release build (which ships
            // feature.communityHubSegments.client empty) would subscribe to three topics and hold both
            // channels' full history for a screen the user cannot open. Same argument as
            // CommunityUnreadCountAggregator, one layer down. Kept separate from the capability await
            // above rather than folded into it: liveSegments happens to imply the capability today,
            // through REQUIRED_FEATURES, and that is a mapping this file does not own.
            communityHubService.liveSegments.first { CommunitySegment.DISCUSSIONS in it }

            // The third precondition, and it has to be met BEFORE subscribing. On the node a null
            // selected profile means there is no identity at all, so it drops the message; here it
            // usually means the profile has not arrived yet, because ClientUserProfileServiceFacade
            // fetches it over the same WebSocket this subscription is racing. Dropping on that would
            // discard the whole snapshot — which arrives once, and is not resent until a reconnect.
            userProfileServiceFacade.selectedUserProfile.filterNotNull().first()
            refreshMyIdentityIds()
            log.i { "Public chat is supported by the paired node; subscribing" }

            launch { observeSelectedProfileChanges() }
            launch { subscribeChannels() }
            launch { subscribeMessages() }
            launch { subscribeReactions() }
        }
    }

    override suspend fun deactivate() {
        // Cancelled before the state is cleared: super.deactivate() disposes the scope the three
        // subscription collectors run on, and one of them landing between the clear and the cancel
        // would repopulate everything we just dropped.
        super<ServiceFacade>.deactivate()
        stateMutex.withLock {
            channelModelsById.clear()
            messageDtosById.clear()
            reactionDtosByMessageId.clear()
            myIdentityIds = emptySet()
            _channels.value = emptyList()
        }
    }

    override suspend fun sendChatMessage(
        channelId: String,
        text: String,
        citation: Citation?,
    ): Result<Unit> {
        if (globalUiManager.notifyIfDemoModeRestricted()) return Result.success(Unit)
        // Named rather than left to the node's own selection, so the message goes out as the profile
        // the user is looking at even if the node's selection moved underneath.
        val senderUserProfileId = userProfileServiceFacade.selectedUserProfile.value?.id
        return apiGateway
            .sendTextMessage(channelId, text, citation, senderUserProfileId)
            .recoverCatching { throw asDomainFailure(it) }
    }

    override suspend fun editChatMessage(
        channelId: String,
        messageId: String,
        text: String,
    ): Result<Unit> {
        if (globalUiManager.notifyIfDemoModeRestricted()) return Result.success(Unit)
        return apiGateway
            .editMessage(channelId, messageId, text)
            .recoverCatching { throw asDomainFailure(it, removalTarget = "original message") }
    }

    override suspend fun deleteChatMessage(
        channelId: String,
        messageId: String,
    ): Result<Unit> {
        if (globalUiManager.notifyIfDemoModeRestricted()) return Result.success(Unit)
        return apiGateway
            .deleteMessage(channelId, messageId)
            .recoverCatching { throw asDomainFailure(it, removalTarget = "message") }
    }

    override suspend fun addChatMessageReaction(
        channelId: String,
        messageId: String,
        reactionEnum: ReactionEnum,
    ): Result<Unit> {
        if (globalUiManager.notifyIfDemoModeRestricted()) return Result.success(Unit)
        val senderUserProfileId = userProfileServiceFacade.selectedUserProfile.value?.id
        return apiGateway
            .addChatMessageReaction(channelId, messageId, reactionEnum, senderUserProfileId)
            .recoverCatching { throw asDomainFailure(it) }
    }

    override suspend fun removeChatMessageReaction(
        channelId: String,
        messageId: String,
        reaction: CommonPublicChatMessageReaction,
    ): Result<Unit> {
        if (globalUiManager.notifyIfDemoModeRestricted()) return Result.success(Unit)
        return apiGateway
            .removeChatMessageReaction(channelId, messageId, reaction)
            .recoverCatching { throw asDomainFailure(it, removalTarget = "reaction") }
    }

    override suspend fun consumeNotifications(channelId: String) {
        if (globalUiManager.notifyIfDemoModeRestricted()) return
        apiGateway
            .consumeNotifications(channelId)
            // The channel id is logged, unlike private chat's: "discussion.bisq" names a public room,
            // not the two people in a conversation. The class and status only, never the throwable:
            // a WebSocketRestApiException's message is the node's response body verbatim, and a
            // logged throwable prints it.
            .onFailure { cause ->
                val status = (cause as? WebSocketRestApiException)?.httpStatusCode?.let { ", HTTP ${it.value}" }.orEmpty()
                log.e { "Failed to consume notifications of public chat channel $channelId (${cause::class.simpleName}$status)" }
            }
    }

    // Private

    /**
     * The node's statuses, as the closed set of failures [PublicChatServiceFacade] promises.
     *
     * The three flavours of 403 have to be told apart, and the body is the only thing that does it.
     * `WebSocketApiClient` decodes a body that opens with `{` and keeps the value of its `error` key,
     * so the permission filter's `{"error": "permission_not_granted", …}` arrives as that bare token
     * while the endpoint's own refusal arrives as the prose it sent. A blank body is the filter too —
     * a missing or unknown client id.
     *
     * @param removalTarget names what a 500 failed to remove, passed by the caller rather than parsed:
     *   the call site already knows whether it asked to edit, delete, or take a reaction back. bisq2
     *   answers 500 for an unexpected error as well, which is why the message is still matched.
     *
     * Every other status is reduced to its code, without the body: a public channel id is safe to log
     * but a 400 can quote a profile id back, and `handleError` logs `message`.
     */
    private fun asDomainFailure(
        cause: Throwable,
        removalTarget: String? = null,
    ): Throwable =
        when {
            cause !is WebSocketRestApiException -> cause
            cause.httpStatusCode == HttpStatusCode.Forbidden ->
                if (cause.message.isNullOrBlank() || cause.message == PERMISSION_NOT_GRANTED) {
                    IllegalStateException("The trusted node did not grant this app access to public chat")
                } else {
                    PublicChatNotAuthorException()
                }

            cause.httpStatusCode == HttpStatusCode.Conflict -> PublicChatSendRefusedException(parseRejection(cause))
            cause.httpStatusCode == HttpStatusCode.TooManyRequests ->
                // Unlike the 409 this carries no SendRefusedResponse — the status is the whole answer.
                PublicChatSendRefusedException(PublicChatSendRejection.RATE_LIMIT_EXCEEDED)

            removalTarget != null &&
                cause.httpStatusCode == HttpStatusCode.InternalServerError &&
                cause.message?.contains(COULD_NOT_BE_REMOVED) == true -> PublicChatRemovalRejectedException(removalTarget)

            else -> IllegalStateException("Public chat request failed with HTTP ${cause.httpStatusCode.value}")
        }

    /**
     * The 409 body arrives as [WebSocketRestApiException.message] verbatim: `WebSocketApiClient` only
     * unwraps an `{"error": …}` envelope, and this body is a `SendRefusedResponse` instead. bisq2
     * shares that response with private chat, so it can name `PEER_BANNED` — which a public channel
     * has no room for. That, and anything this build cannot decode, is [PublicChatSendRejection.UNKNOWN]
     * rather than a guess.
     */
    private fun parseRejection(cause: WebSocketRestApiException): PublicChatSendRejection =
        runCatching {
            when (json.decodeFromString<SendRefusedResponse>(cause.message.orEmpty()).rejection) {
                SendRejectionDto.MY_PROFILE_BANNED -> PublicChatSendRejection.MY_PROFILE_BANNED
                SendRejectionDto.PEER_BANNED -> PublicChatSendRejection.UNKNOWN
            }
        }.getOrElse {
            // The exception is not logged: kotlinx quotes the input, i.e. the raw body.
            log.w { "Could not parse the 409 refusal body; reporting UNKNOWN" }
            PublicChatSendRejection.UNKNOWN
        }

    /**
     * Reloads the identities and rebuilds, because a switch moves both things a message is built with:
     * which messages are mine, and whose reaction is whose.
     *
     * `drop(1)` skips the value [activate] already waited for.
     */
    private suspend fun observeSelectedProfileChanges() {
        userProfileServiceFacade.selectedUserProfile
            .filterNotNull()
            .distinctUntilChanged()
            .drop(1)
            .collect {
                refreshMyIdentityIds()
                stateMutex.withLock { channelModelsById.keys.toList().forEach { rebuildMessages(it) } }
            }
    }

    /**
     * A REST call, so it can fail. Left to throw it would take the whole activation down with it and
     * the symptom would be a channel that never arrives; the fallback costs Edit and Delete on messages
     * written by an identity that is not the selected one, which is the smaller loss by far — but an
     * invisible one: nothing on screen says the two menu items are gone, and the log line below is the
     * only trace.
     *
     * `ensureActive` before falling back, because `deactivate()` cancels the coroutine parked inside
     * this very call: swallowing that would log a failure that never happened and write state on a
     * facade that is going away. Same guard, and the same reasoning, as the node twin's mutations.
     */
    private suspend fun refreshMyIdentityIds() {
        val ids =
            runCatching { userProfileServiceFacade.getUserIdentityIds().toSet() }
                .onFailure { currentCoroutineContext().ensureActive() }
                .getOrElse { cause ->
                    // The class only, never the throwable: getUserIdentityIds() rethrows the REST
                    // exception whose message is the node's response body verbatim.
                    log.w {
                        "Could not load the user identity ids (${cause::class.simpleName}); " +
                            "falling back to the selected profile"
                    }
                    setOfNotNull(userProfileServiceFacade.selectedUserProfile.value?.id)
                }
        stateMutex.withLock { myIdentityIds = ids }
    }

    private suspend fun subscribeChannels() {
        apiGateway.subscribeChannels().webSocketEvent.collectGuarded { webSocketEvent ->
            val payload =
                WebSocketEventPayload.from<List<CommonPublicChatChannelDto>>(json, webSocketEvent) ?: return@collectGuarded
            stateMutex.withLock {
                // No branching on the type, unlike messages and reactions. The first event is always
                // the snapshot WebSocketClientImpl synthesises as REPLACE, and after it the topic only
                // pushes ADDED — re-sent whenever the unread count moves, which is the only thing about
                // a public channel that changes. Upserting fits both, because bisq2 registers one
                // channel per domain at startup and never adds or drops one, so absence — the one
                // thing a REPLACE says that an ADDED cannot — carries no information here.
                payload.payload.forEach { upsertChannel(it) }
                publishChannels()
            }
        }
    }

    private suspend fun subscribeMessages() {
        apiGateway.subscribeMessages().webSocketEvent.collectGuarded { webSocketEvent ->
            val payload =
                WebSocketEventPayload.from<List<CommonPublicChatMessageDto>>(json, webSocketEvent) ?: return@collectGuarded
            stateMutex.withLock {
                when (webSocketEvent.modificationType) {
                    // The snapshot, synthesised on every (re)subscribe from the node's full history of
                    // every channel this subscription covers. Authoritative about absence, which is
                    // what repairs the removals bisq2 documents as never pushed: an author banned or
                    // rate limited when the removal was offered, or a profile pruned before the DTO
                    // could be built. Private chat has no branch here at all.
                    ModificationType.REPLACE -> {
                        messageDtosById.clear()
                        payload.payload.forEach { messageDtosById[it.messageId] = it }
                        // Pruned rather than cleared. The two topics snapshot independently and in no
                        // fixed order, so clearing here wipes a reactions snapshot that happened to
                        // land first — and it never comes back, since a snapshot arrives once per
                        // subscribe. Dropping only what has no message left is right either way round.
                        reactionDtosByMessageId.keys.retainAll(messageDtosById.keys)
                        channelModelsById.keys.toList().forEach { rebuildMessages(it) }
                    }

                    ModificationType.REMOVED -> payload.payload.forEach { forgetMessage(it.messageId) }

                    else ->
                        payload.payload.forEach { dto ->
                            messageDtosById[dto.messageId] = dto
                            materializeMessage(dto.messageId)
                        }
                }
            }
        }
    }

    private suspend fun subscribeReactions() {
        apiGateway.subscribeReactions().webSocketEvent.collectGuarded { webSocketEvent ->
            val payload =
                WebSocketEventPayload.from<List<CommonPublicChatMessageReactionDto>>(json, webSocketEvent) ?: return@collectGuarded
            stateMutex.withLock {
                when (webSocketEvent.modificationType) {
                    ModificationType.REPLACE -> {
                        reactionDtosByMessageId.clear()
                        payload.payload.forEach { addReaction(it) }
                        channelModelsById.keys.toList().forEach { rebuildMessages(it) }
                    }

                    ModificationType.REMOVED ->
                        payload.payload.forEach { dto ->
                            // No entry is the normal case, not an inconsistency: bisq2 pushes a removal
                            // for a reaction it may never have pushed — one whose sender was banned
                            // after the fact — and counts on the client deleting by id and moving on.
                            reactionDtosByMessageId[dto.chatMessageId]?.let { reactions ->
                                reactions.remove(dto.id)
                                if (reactions.isEmpty()) {
                                    reactionDtosByMessageId.remove(dto.chatMessageId)
                                }
                            }
                            materializeMessage(dto.chatMessageId)
                        }

                    else ->
                        payload.payload.forEach { dto ->
                            addReaction(dto)
                            materializeMessage(dto.chatMessageId)
                        }
                }
            }
        }
    }

    /**
     * One throw would cancel the collector, and through the shared parent the other two, for the
     * rest of the session with nothing on screen to say so. The event is dropped instead; the next
     * resubscribe snapshot repairs whatever it carried. Same guard, and the same reasoning, as the
     * node twin's unread recount.
     *
     * An undecodable payload — one node round-trip away once a newer bisq2 adds an enum constant —
     * no longer reaches this catch: [WebSocketEventPayload.from] returns null for it (logging topic
     * and cause itself), and the call sites skip. What remains here is a failure in processing a
     * decoded event.
     */
    private suspend fun Flow<WebSocketEvent?>.collectGuarded(block: suspend (WebSocketEvent) -> Unit) =
        collect { webSocketEvent ->
            if (webSocketEvent?.deferredPayload == null) {
                return@collect
            }
            try {
                block(webSocketEvent)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The class only: a decode failure's message quotes the payload it choked on.
                log.w { "Dropped a ${webSocketEvent.topic} event that could not be processed (${e::class.simpleName})" }
            }
        }

    private fun addReaction(dto: CommonPublicChatMessageReactionDto) {
        reactionDtosByMessageId.getOrPut(dto.chatMessageId) { mutableMapOf() }[dto.id] = dto
    }

    /**
     * Updates the existing model in place; only a channel we have never seen produces a new one, and
     * only then is its message set built — replacing the model would strand an open screen, which
     * collects on the instance it resolved at `initialize`.
     */
    private fun upsertChannel(dto: CommonPublicChatChannelDto) {
        val known = channelModelsById.containsKey(dto.id)
        val model = channelModelsById.getOrPut(dto.id) { dto.toDomain() }
        model.setUnreadCount(dto.unreadCount)
        if (!known) {
            // The messages topic can land before the channels one, and its DTOs are kept either way;
            // this is where they become a message set.
            rebuildMessages(dto.id)
        }
    }

    /** One message in, one message out. The full rebuild is for a snapshot, not for a single event. */
    private fun materializeMessage(messageId: String) {
        val dto = messageDtosById[messageId] ?: return
        val model = channelModelsById[dto.channelId] ?: return
        val myUserProfile = userProfileServiceFacade.selectedUserProfile.value ?: return
        model.addChatMessage(toDomain(dto, myUserProfile))
    }

    private fun forgetMessage(messageId: String) {
        val dto = messageDtosById.remove(messageId) ?: return
        // bisq2 pushes no REMOVED for the reactions of a removed message; dropping them here is what
        // keeps them from outliving it in the maps.
        reactionDtosByMessageId.remove(messageId)
        channelModelsById[dto.channelId]?.removeChatMessage(messageId)
    }

    private fun rebuildMessages(channelId: String) {
        val model = channelModelsById[channelId] ?: return
        val myUserProfile = userProfileServiceFacade.selectedUserProfile.value ?: return
        val messages =
            messageDtosById.values
                .asSequence()
                .filter { it.channelId == channelId }
                .map { toDomain(it, myUserProfile) }
                .toSet()
        model.setAllChatMessages(messages)
    }

    private fun toDomain(
        dto: CommonPublicChatMessageDto,
        myUserProfile: UserProfileVO,
    ) = dto.toDomain(
        myUserProfile = myUserProfile,
        // bisq2's own authorisation rule, and what gates the Edit and Delete menu items: ANY of my
        // identities, not only the selected one.
        isMyMessage = dto.authorUserProfileId in myIdentityIds,
        // From the reactions topic rather than the message's embedded set, which is a snapshot from
        // the moment the message was pushed.
        chatReactions = reactionDtosByMessageId[dto.messageId]?.values.orEmpty().map { it.toDomain() },
    )

    private fun publishChannels() {
        // Sorted like the node flavour, so the list a presenter reads never depends on arrival order.
        _channels.value = channelModelsById.values.sortedBy { it.chatChannelDomain.ordinal }
    }

    private companion object {
        const val PERMISSION_NOT_GRANTED = "permission_not_granted"
        const val COULD_NOT_BE_REMOVED = "could not be removed locally"
    }
}
