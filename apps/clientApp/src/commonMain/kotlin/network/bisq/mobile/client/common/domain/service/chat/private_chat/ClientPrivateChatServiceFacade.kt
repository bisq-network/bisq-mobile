package network.bisq.mobile.client.common.domain.service.chat.private_chat

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.util.notifyIfDemoModeRestricted
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketRestApiException
import network.bisq.mobile.client.common.domain.websocket.subscription.ModificationType
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.data.replicated.chat.Citation
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatChannel
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatMessageReaction
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.data.service.chat.private_chat.PrivateChatNotPermittedException
import network.bisq.mobile.data.service.chat.private_chat.PrivateChatServiceFacade
import network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService
import network.bisq.mobile.domain.service.capabilities.Feature
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager

/**
 * Private chat (DM) over the trusted node's API.
 *
 * Gated on [Feature.PRIVATE_CHAT]: a node that does not advertise it has no private-chat topics or
 * endpoints, so [isSupported] is false, callers hide the entry point, and [activate] subscribes to
 * nothing. Subscribing anyway would not fail fast — such a node cannot parse the topic out of the
 * subscription request and simply never answers, costing a full subscribe timeout per topic.
 *
 * Three topics feed this, mirroring trade chat: channels, messages, and reactions. Channels arrive
 * again whenever their unread count changes, so a channel model is updated in place rather than
 * replaced — it owns the message set, the unread count and each message's delivery status as flows.
 */
class ClientPrivateChatServiceFacade(
    private val apiGateway: PrivateChatApiGateway,
    private val backendCapabilitiesService: BackendCapabilitiesService,
    private val json: Json,
    private val globalUiManager: GlobalUiManager,
    // Injectable so tests can drive the subscription collectors on their virtual-time dispatcher.
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ServiceFacade(),
    PrivateChatServiceFacade {
    override val isSupported: Flow<Boolean>
        get() =
            backendCapabilitiesService.capabilities
                .map { it.isSupported(Feature.PRIVATE_CHAT) }
                .distinctUntilChanged()

    private val _channels = MutableStateFlow<List<TwoPartyPrivateChatChannel>>(emptyList())
    override val channels: StateFlow<List<TwoPartyPrivateChatChannel>> = _channels.asStateFlow()

    // All mutable state below is guarded by this: the three subscriptions collect concurrently and
    // every one of them recomputes a channel's message set from all three sources.
    private val stateMutex = Mutex()
    private val channelModelsById: MutableMap<String, TwoPartyPrivateChatChannel> = linkedMapOf()
    private val myUserProfileIdsByChannelId: MutableMap<String, String> = mutableMapOf()
    private val messageDtosById: MutableMap<String, TwoPartyPrivateChatMessageDto> = mutableMapOf()
    private val reactionDtos: MutableSet<TwoPartyPrivateChatMessageReactionDto> = mutableSetOf()

    override suspend fun activate() {
        super<ServiceFacade>.activate()

        serviceScope.launch(defaultDispatcher) {
            // Awaited rather than read as a snapshot: this facade is activated before
            // ConfigServiceFacade, which is what fetches the /config/capabilities manifest, so at this
            // point the capability set is still the legacy baseline and a snapshot would always say
            // "unsupported" — silently leaving the feature dead for the whole session.
            //
            // On a node that never advertises it this simply never resumes, which is the intended
            // "subscribe to nothing": serviceScope is cancelled on deactivate.
            backendCapabilitiesService.capabilities.first { it.isSupported(Feature.PRIVATE_CHAT) }
            log.i { "Private chat is supported by the paired node; subscribing" }

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
            myUserProfileIdsByChannelId.clear()
            messageDtosById.clear()
            reactionDtos.clear()
            _channels.value = emptyList()
        }
    }

    override suspend fun findOrCreateChannel(peerProfileId: String): Result<String> {
        // Unreachable in practice — demo mode never advertises the feature, so the entry point is
        // hidden. Guarded anyway, because there is no channel id to invent if it ever were reached.
        if (globalUiManager.notifyIfDemoModeRestricted()) {
            return Result.failure(UnsupportedOperationException("Private chats are not available in demo mode"))
        }
        return apiGateway
            .findOrCreateChannel(peerProfileId)
            .map { it.channelId }
            .recoverCatching { throw asDomainFailure(it) }
    }

    override suspend fun sendChatMessage(
        channelId: String,
        text: String,
        citation: Citation?,
    ): Result<Unit> {
        if (globalUiManager.notifyIfDemoModeRestricted()) return Result.success(Unit)
        return apiGateway.sendTextMessage(channelId, text, citation).recoverCatching { throw asDomainFailure(it) }
    }

    override suspend fun addChatMessageReaction(
        channelId: String,
        messageId: String,
        reactionEnum: ReactionEnum,
    ): Result<Unit> {
        if (globalUiManager.notifyIfDemoModeRestricted()) return Result.success(Unit)
        return apiGateway
            .addChatMessageReaction(channelId, messageId, reactionEnum)
            .recoverCatching { throw asDomainFailure(it) }
    }

    override suspend fun removeChatMessageReaction(
        channelId: String,
        messageId: String,
        reaction: TwoPartyPrivateChatMessageReaction,
    ): Result<Boolean> {
        // Demo mode never actually removes anything → honour the contract by reporting false.
        if (globalUiManager.notifyIfDemoModeRestricted()) return Result.success(false)
        // Under the lock like every other access to this map: the three subscription collectors write
        // it, and an unguarded read racing a rehash can come back null — which here is indistinguishable
        // from "not our reaction" and would swallow the removal.
        val myUserProfileId = stateMutex.withLock { myUserProfileIdsByChannelId[channelId] }
        if (myUserProfileId == null || reaction.senderUserProfile.id != myUserProfileId) {
            // Not our reaction, so we cannot remove it.
            return Result.success(false)
        }
        return apiGateway
            .removeChatMessageReaction(channelId, messageId, reaction)
            .map { true }
            .recoverCatching { throw asDomainFailure(it) }
    }

    override suspend fun leaveChannel(channelId: String): Result<Unit> {
        if (globalUiManager.notifyIfDemoModeRestricted()) return Result.success(Unit)
        return apiGateway
            .leaveChannel(channelId)
            .onSuccess { removeChannel(channelId) }
            .recoverCatching { throw asDomainFailure(it) }
    }

    override suspend fun consumeNotifications(channelId: String) {
        if (globalUiManager.notifyIfDemoModeRestricted()) return
        apiGateway
            .consumeNotifications(channelId)
            // The id is left out on purpose: a two-party channel id is derived from the two profile
            // ids, so logging it records who is talking to whom, and device logs travel in bug reports.
            .onFailure { log.e(it) { "Failed to consume notifications of a private chat channel" } }
    }

    // Private

    /**
     * Translates a 403 into [PrivateChatNotPermittedException].
     *
     * The node advertises the private-chat capability from `/config/capabilities`, which is public and
     * deliberately not permission-filtered, so [isSupported] can be true for a pairing that was never
     * granted `PRIVATE_CHAT_CHANNELS`. Without this the failure reaches the UI as a generic error and
     * gets reported as a connection problem, which it is not.
     *
     * Applied to every REST call, not just channel creation, because a pairing that lost the
     * permission still reaches this screen: DMs keep arriving over the `PRIVATE_CHAT_*` topics, which
     * bisq2 never authorises (see [PrivateChatServiceFacade.isSupported]). Opening an existing
     * conversation therefore skips [findOrCreateChannel] entirely, and the first send would have been
     * the first 403 — reported as a dropped connection, which sends the user off to retry something
     * only a re-pairing can fix.
     */
    private fun asDomainFailure(cause: Throwable): Throwable =
        if (cause is WebSocketRestApiException && cause.httpStatusCode == HttpStatusCode.Forbidden) {
            PrivateChatNotPermittedException()
        } else {
            cause
        }

    private suspend fun subscribeChannels() {
        val observer = apiGateway.subscribeChannels()
        observer.webSocketEvent.collect { webSocketEvent ->
            if (webSocketEvent?.deferredPayload == null) {
                return@collect
            }
            val payload: WebSocketEventPayload<List<TwoPartyPrivateChatChannelDto>> =
                WebSocketEventPayload.from(json, webSocketEvent)
            stateMutex.withLock {
                when (webSocketEvent.modificationType) {
                    // REMOVED is a channel that was left — here or on another client of the same node.
                    // Treating it as an upsert like the other types would resurrect it.
                    ModificationType.REMOVED -> payload.payload.forEach { forgetChannel(it.id) }

                    // REPLACE never comes from the node, which only ever sends ADDED/REMOVED here: it
                    // is the (re)subscription snapshot the client synthesises from the node's full
                    // channel list, so it is authoritative about absence too. Upserting it kept a
                    // channel that was left on another client while this one was offline alive for the
                    // rest of the session — openable, with every write against it failing.
                    ModificationType.REPLACE -> {
                        val incomingIds = payload.payload.mapTo(mutableSetOf()) { it.id }
                        channelModelsById.keys
                            .filterNot { it in incomingIds }
                            .toList()
                            .forEach { forgetChannel(it) }
                        payload.payload.forEach { upsertChannel(it) }
                    }

                    else -> payload.payload.forEach { upsertChannel(it) }
                }
                publishChannels()
            }
        }
    }

    private suspend fun subscribeMessages() {
        val observer = apiGateway.subscribeMessages()
        observer.webSocketEvent.collect { webSocketEvent ->
            if (webSocketEvent?.deferredPayload == null) {
                return@collect
            }
            val payload: WebSocketEventPayload<List<TwoPartyPrivateChatMessageDto>> =
                WebSocketEventPayload.from(json, webSocketEvent)
            stateMutex.withLock {
                payload.payload.forEach { messageDtosById[it.messageId] = it }
                payload.payload
                    .map { it.channelId }
                    .toSet()
                    .forEach { rebuildMessages(it) }
            }
        }
    }

    private suspend fun subscribeReactions() {
        val observer = apiGateway.subscribeReactions()
        observer.webSocketEvent.collect { webSocketEvent ->
            if (webSocketEvent?.deferredPayload == null) {
                return@collect
            }
            val payload: WebSocketEventPayload<List<TwoPartyPrivateChatMessageReactionDto>> =
                WebSocketEventPayload.from(json, webSocketEvent)
            stateMutex.withLock {
                if (webSocketEvent.modificationType == ModificationType.REPLACE) {
                    // Same reasoning as subscribeChannels: the snapshot carries every live reaction —
                    // the node filters removed ones out of it — so a reaction withdrawn while this
                    // client was offline is simply absent, and merging would leave it on screen.
                    reactionDtos.clear()
                    reactionDtos.addAll(payload.payload.filterNot { it.isRemoved })
                    // Every known channel, not just those named in the payload: the channels that lost
                    // a reaction are exactly the ones the snapshot no longer mentions.
                    channelModelsById.keys.toList().forEach { rebuildMessages(it) }
                } else {
                    payload.payload.forEach { applyReaction(it) }
                    payload.payload
                        .mapNotNull { messageDtosById[it.chatMessageId]?.channelId }
                        .toSet()
                        .forEach { rebuildMessages(it) }
                }
            }
        }
    }

    private fun applyReaction(reaction: TwoPartyPrivateChatMessageReactionDto) {
        if (reaction.isRemoved) {
            // A removal arrives as its own reaction with a fresh id, so the original cannot be looked
            // up by id — match it on what identifies it to a user instead.
            reactionDtos.removeAll { existing ->
                existing.chatMessageId == reaction.chatMessageId &&
                    existing.senderUserProfile.id == reaction.senderUserProfile.id &&
                    existing.reactionId == reaction.reactionId
            }
        } else {
            reactionDtos.add(reaction)
        }
    }

    /**
     * Updates the existing model in place; only a channel we have never seen produces a new one.
     *
     * The node re-resolves the peer on every push, but the model keeps the profile it was built with:
     * nothing the UI reads off it can move (a Bisq 2 nickname is immutable and the avatar comes from
     * the proof of work), and replacing the model would strand the open conversation, which collects
     * on the instance it resolved at `initialize`.
     */
    private fun upsertChannel(dto: TwoPartyPrivateChatChannelDto) {
        val model = channelModelsById.getOrPut(dto.id) { dto.toDomain() }
        myUserProfileIdsByChannelId[dto.id] = dto.myUserProfile.id
        model.setUnreadCount(dto.unreadCount)
        rebuildMessages(dto.id)
    }

    private fun rebuildMessages(channelId: String) {
        val model = channelModelsById[channelId] ?: return
        // Per channel, not the globally selected profile: a DM is bound to whichever identity created
        // or received it, and the node sends that identity's profile with the channel.
        val myUserProfile = model.myUserProfile
        val messages =
            messageDtosById.values
                .asSequence()
                .filter { it.channelId == channelId }
                .map { message ->
                    val reactions =
                        reactionDtos
                            .filter { it.chatMessageId == message.messageId }
                            .map { it.toDomain() }
                    message.toDomain(myUserProfile, reactions)
                }.toSet()
        model.setAllChatMessages(messages)
    }

    private suspend fun removeChannel(channelId: String) {
        stateMutex.withLock {
            forgetChannel(channelId)
            publishChannels()
        }
    }

    private fun forgetChannel(channelId: String) {
        channelModelsById.remove(channelId)
        myUserProfileIdsByChannelId.remove(channelId)
        val messageIds =
            messageDtosById.values
                .filter { it.channelId == channelId }
                .map { it.messageId }
                .toSet()
        messageIds.forEach { messageDtosById.remove(it) }
        reactionDtos.removeAll { it.chatMessageId in messageIds }
    }

    private fun publishChannels() {
        _channels.value = channelModelsById.values.toList()
    }
}
