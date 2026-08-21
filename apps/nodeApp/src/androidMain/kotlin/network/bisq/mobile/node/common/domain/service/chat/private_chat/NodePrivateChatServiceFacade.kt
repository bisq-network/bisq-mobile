package network.bisq.mobile.node.common.domain.service.chat.private_chat

import bisq.chat.ChatChannelDomain
import bisq.chat.ChatService
import bisq.chat.notifications.ChatNotificationService
import bisq.chat.priv.LeavePrivateChatManager
import bisq.chat.two_party.TwoPartyPrivateChatChannelService
import bisq.common.observable.Pin
import bisq.common.observable.collection.CollectionObserver
import bisq.user.banned.BannedUserService
import bisq.user.identity.UserIdentityService
import bisq.user.profile.UserProfile
import bisq.user.profile.UserProfileService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import network.bisq.mobile.data.replicated.chat.Citation
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatChannel
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatMessageReaction
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.data.service.chat.private_chat.PrivateChatServiceFacade
import network.bisq.mobile.node.common.domain.mapping.Mappings
import network.bisq.mobile.node.common.domain.mapping.chat.toDomain
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull
import bisq.chat.two_party.TwoPartyPrivateChatChannel as Bisq2TwoPartyPrivateChatChannel
import bisq.chat.two_party.TwoPartyPrivateChatMessage as Bisq2TwoPartyPrivateChatMessage

/**
 * Private chat (DM) backed by Bisq 2's `TwoPartyPrivateChatChannelService`.
 *
 * Mirrors what Bisq 2 desktop does, which is not always what the trade chat facade does:
 *  - channels are created through [ChatService.createAndSelectTwoPartyPrivateChatChannel], not the
 *    channel service directly, because the "select" half binds the user identity the next DM will use
 *  - leaving goes through [LeavePrivateChatManager], which also clears notifications and selection
 *  - unread state comes from Bisq 2's persisted [ChatNotificationService] rather than a local count
 */
class NodePrivateChatServiceFacade(
    applicationService: AndroidApplicationService.Provider,
) : ServiceFacade(),
    PrivateChatServiceFacade {
    // Dependencies
    private val chatService: ChatService by lazy { applicationService.chatService.get() }

    /** Bisq 2 registers exactly one two-party service, for [ChatChannelDomain.DISCUSSION]. */
    private val channelService: TwoPartyPrivateChatChannelService by lazy { chatService.twoPartyPrivateChatChannelService }
    private val leavePrivateChatManager: LeavePrivateChatManager by lazy { chatService.leavePrivateChatManager }
    private val chatNotificationService: ChatNotificationService by lazy { chatService.chatNotificationService }
    private val userProfileService: UserProfileService by lazy { applicationService.userService.get().userProfileService }
    private val userIdentityService: UserIdentityService by lazy { applicationService.userService.get().userIdentityService }
    private val bannedUserService: BannedUserService by lazy { applicationService.userService.get().bannedUserService }

    override val isSupported: Flow<Boolean> = flowOf(true)

    private val _channels = MutableStateFlow<List<TwoPartyPrivateChatChannel>>(emptyList())
    override val channels: StateFlow<List<TwoPartyPrivateChatChannel>> = _channels.asStateFlow()

    // Pins, all scoped per channel so that removing one channel never disturbs another's observers.
    private var channelsPin: Pin? = null
    private var notificationsPin: Pin? = null

    // Concurrent, for the same reason _channels is updated atomically below: Bisq2 runs the observers
    // that write these on whichever thread mutated the collection, and deactivate() drains them from a
    // third. A plain HashMap here loses a pin — an observer left bound to a channel we no longer hold —
    // or throws out of a Bisq2 callback.
    private val messagePinsByChannelId: MutableMap<String, MutableSet<Pin>> = ConcurrentHashMap()
    private val reactionPinsByChannelId: MutableMap<String, MutableMap<String, Pin>> = ConcurrentHashMap()

    override suspend fun activate() {
        super<ServiceFacade>.activate()

        channelsPin =
            channelService.channels.addObserver(
                object : CollectionObserver<Bisq2TwoPartyPrivateChatChannel> {
                    override fun onAdded(channel: Bisq2TwoPartyPrivateChatChannel) {
                        handleChannelAdded(channel)
                    }

                    override fun onRemoved(element: Any) {
                        if (element is Bisq2TwoPartyPrivateChatChannel) {
                            handleChannelRemoved(element)
                        }
                    }

                    override fun onCleared() {
                        handleChannelsCleared()
                    }
                },
            )

        // Bisq2 deliberately emits null here to force observers to re-evaluate (it excludes
        // isConsumed from equals), so a null must not be treated as an event payload.
        notificationsPin = chatNotificationService.changedNotification.addObserver { refreshUnreadCounts() }
    }

    override suspend fun deactivate() {
        channelsPin?.unbind()
        channelsPin = null
        notificationsPin?.unbind()
        notificationsPin = null

        messagePinsByChannelId.values.forEach { pins -> pins.forEach { it.unbind() } }
        messagePinsByChannelId.clear()
        reactionPinsByChannelId.values.forEach { pins -> pins.values.forEach { it.unbind() } }
        reactionPinsByChannelId.clear()
        _channels.value = emptyList()

        super<ServiceFacade>.deactivate()
    }

    override suspend fun findOrCreateChannel(peerProfileId: String): Result<String> =
        withContext(Dispatchers.Default) {
            runCatching {
                // No id in any message below: these become the `Result.failure` cause, and
                // `BasePresenter.handleError` logs `exception.message` verbatim. A two-party channel
                // id is derived from both profile ids, so logging one records who is talking to whom,
                // and device logs travel in bug reports.
                val peer =
                    userProfileService.findUserProfile(peerProfileId).getOrNull()
                        ?: error("No user profile found for the requested peer")
                // Desktop always goes through this wrapper rather than findOrCreateChannel: it also
                // selects the channel, which switches the globally selected identity to the one this
                // channel is bound to. Skipping that misbinds the *next* DM for multi-identity users.
                val channel =
                    chatService.createAndSelectTwoPartyPrivateChatChannel(ChatChannelDomain.DISCUSSION, peer).getOrNull()
                        ?: error("Could not create a private chat channel with the requested peer")
                channel.id
            }
        }

    override suspend fun sendChatMessage(
        channelId: String,
        text: String,
        citation: Citation?,
    ): Result<Unit> =
        withContext(Dispatchers.Default) {
            // The onFailure below is not decoration: this is the only runCatching in the file wrapping
            // a real suspension point, so the only one that can catch a cancellation. Leaving the
            // screen mid-send would otherwise come back as an ordinary Result.failure and raise a
            // "could not send" snackbar for a send nobody is waiting on any more.
            //
            // ensureActive rather than rethrowing every CancellationException, because two different
            // things arrive as one type here: `await` also throws it when the *future* is cancelled
            // while this coroutine is perfectly alive, and rethrowing that would cancel the caller
            // silently instead of reporting a send that did fail. Same distinction, and the same
            // reasoning, as WebSocketApiClient.kt:159.
            runCatching {
                val channel = requireChannel(channelId)
                val bisq2Citation = Optional.ofNullable(citation?.let { Mappings.CitationMapping.toBisq2Model(it) })
                // Awaited, mirroring `NodeTradeChatMessagesServiceFacade.sendChatMessage` — the two
                // chats make this call the same way, and neither is the place to change it alone.
                //
                // Worth knowing what is being awaited, because the name suggests less than it does:
                // `PrivateChatChannelService.sendMessage` adds the message to the channel first and
                // only then returns `networkService.confidentialSend(...)`, so this future is about
                // DELIVERY, not local acceptance. Bisq 2's own REST API does not await it —
                // `PrivateChatRestApi.sendTextMessage` fires and answers 204, and its OpenAPI text says
                // outright that "a 204 confirms local acceptance rather than delivery to the peer" — so
                // Bisq Connect never reports a delivery failure for a send, and the node flavour is the
                // stricter of the two. Awaiting costs a Tor round trip behind the send, which is why
                // the two flavours differ at all.
                //
                // See addOrRemoveChatMessageReaction, which does NOT await the same future, and why.
                channelService.sendTextMessage(text, bisq2Citation, channel).await()
                Unit
            }.onFailure { currentCoroutineContext().ensureActive() }
        }

    override suspend fun addChatMessageReaction(
        channelId: String,
        messageId: String,
        reactionEnum: ReactionEnum,
    ): Result<Unit> = addOrRemoveChatMessageReaction(channelId, messageId, reactionEnum, isRemoved = false)

    override suspend fun removeChatMessageReaction(
        channelId: String,
        messageId: String,
        reaction: TwoPartyPrivateChatMessageReaction,
    ): Result<Boolean> =
        withContext(Dispatchers.Default) {
            // Resolved here rather than inline at the call below: as an argument it would be evaluated
            // outside addOrRemoveChatMessageReaction's runCatching, so a reaction id this build does
            // not know would throw out of the facade instead of returning a failure.
            val reactionEnum =
                ReactionEnum.entries.getOrNull(reaction.reactionId)
                    ?: return@withContext Result.failure(
                        IllegalArgumentException("Unsupported reactionId: ${reaction.reactionId}"),
                    )
            if (userIdentityService.findUserIdentity(reaction.userProfileId).isPresent) {
                addOrRemoveChatMessageReaction(channelId, messageId, reactionEnum, isRemoved = true).map { true }
            } else {
                // Not our reaction, so we cannot remove it.
                Result.success(false)
            }
        }

    override suspend fun leaveChannel(channelId: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                val channel = requireChannel(channelId)
                // Not channelService.leaveChannel: the manager additionally re-selects the next
                // channel and consumes the departed channel's notifications, as desktop does.
                leavePrivateChatManager.leaveChannel(channel)
            }
        }

    override suspend fun consumeNotifications(channelId: String) {
        withContext(Dispatchers.Default) {
            findChannel(channelId)?.let { chatNotificationService.consume(it) }
        }
    }

    // Private

    private fun findChannel(channelId: String): Bisq2TwoPartyPrivateChatChannel? = channelService.findChannel(channelId).getOrNull()

    /** Call from inside a `runCatching`, where the failure becomes a `Result.failure`. */
    private fun requireChannel(channelId: String): Bisq2TwoPartyPrivateChatChannel = findChannel(channelId) ?: error("No private chat channel found")

    private fun handleChannelAdded(channel: Bisq2TwoPartyPrivateChatChannel) {
        val channelId = channel.id
        unbindChannelPins(channelId)

        val model = toModel(channel)
        // update, not `value = value...`: Bisq2 runs these observers on whichever thread mutated the
        // collection — a P2P handler or the REST thread — so two channels arriving at once would
        // otherwise lose one to the read-modify-write.
        _channels.update { current -> current.filterNot { it.id == channelId } + model }
        refreshUnreadCount(channel, model)

        val pins = ConcurrentHashMap.newKeySet<Pin>()
        messagePinsByChannelId[channelId] = pins
        // The membership re-check below closes what the concurrent map does not. Storing the set and
        // registering the observer are two steps, so a handleChannelRemoved landing between them
        // removes the set while it is still empty, and the pin added a moment later ends up in a set
        // nothing holds — an observer nobody can ever unbind. Same failure mode as the one described
        // at the channel observer above; the map keeps entries from being lost, not sequences from
        // interleaving.
        pins +=
            channel.chatMessages.addObserver(
                // onAllAdded is not overridden: CollectionObserver already defines it as
                // values.forEach(this::onAdded), which is exactly what we want.
                object : CollectionObserver<Bisq2TwoPartyPrivateChatMessage> {
                    override fun onAdded(message: Bisq2TwoPartyPrivateChatMessage) {
                        addMessageToModel(channel, model, message)
                    }

                    override fun onRemoved(element: Any) {
                        // Private messages cannot be removed
                    }

                    override fun onCleared() {
                        // Private messages cannot be removed
                    }
                },
            )
        if (messagePinsByChannelId[channelId] !== pins) {
            pins.forEach { it.unbind() }
            // The reaction pins go too, because addObserver above replays the messages already in the
            // channel synchronously: each one reaches observeReactions, which recreates this channel's
            // reaction map with computeIfAbsent — after unbindChannelPins had just dropped it. Only
            // when nothing owns the channel again, though: a failed check also means a later
            // handleChannelAdded won the map, and those pins are its, not ours.
            if (!messagePinsByChannelId.containsKey(channelId)) {
                reactionPinsByChannelId.remove(channelId)?.values?.forEach { it.unbind() }
            }
        }
    }

    private fun addMessageToModel(
        channel: Bisq2TwoPartyPrivateChatChannel,
        model: TwoPartyPrivateChatChannel,
        message: Bisq2TwoPartyPrivateChatMessage,
    ) {
        // Bisq2 already rejects banned senders on the inbound path, so this only covers a peer
        // banned *after* their messages arrived — which is why desktop re-checks it too.
        if (bannedUserService.isUserProfileBanned(message.senderUserProfile)) {
            return
        }

        observeReactions(channel, model, message)

        val citationAuthorUserProfile: UserProfile? =
            message.citation
                .flatMap { citation -> userProfileService.findUserProfile(citation.authorUserProfileId) }
                .orElse(null)

        // The identity is per channel, not the globally selected one: a DM is bound to whichever
        // identity created or received it.
        val myUserProfile = channel.myUserIdentity.userProfile
        model.addChatMessage(
            message.toDomain(
                citationAuthorUserProfile,
                myUserProfile,
            ),
        )
    }

    private fun observeReactions(
        channel: Bisq2TwoPartyPrivateChatChannel,
        model: TwoPartyPrivateChatChannel,
        message: Bisq2TwoPartyPrivateChatMessage,
    ) {
        val channelId = channel.id
        val messageId = message.id
        // computeIfAbsent throughout, not getOrPut / containsKey-then-put: two messages of the same
        // channel can arrive on different threads, and the loser of either race leaves behind an
        // observer that nothing ever unbinds — getOrPut on a ConcurrentHashMap is still get-then-put.
        val pinsForChannel = reactionPinsByChannelId.computeIfAbsent(channelId) { ConcurrentHashMap() }
        pinsForChannel.computeIfAbsent(messageId) {
            message.chatMessageReactions.addObserver {
                model.chatMessages.value
                    .find { it.id == messageId }
                    ?.setReactions(
                        message.chatMessageReactions
                            .filter { !it.isRemoved }
                            .map { it.toDomain() },
                    )
            }
        }
    }

    private fun handleChannelRemoved(channel: Bisq2TwoPartyPrivateChatChannel) {
        unbindChannelPins(channel.id)
        _channels.update { current -> current.filterNot { it.id == channel.id } }
    }

    private fun handleChannelsCleared() {
        // The union of both maps, not just the message one: a channel can hold reaction pins without
        // holding message pins — see the replay described in handleChannelAdded — and iterating one
        // map would walk past it.
        (messagePinsByChannelId.keys + reactionPinsByChannelId.keys).toList().forEach { unbindChannelPins(it) }
        _channels.value = emptyList()
    }

    /** Unbinds only [channelId]'s observers — never another channel's. */
    private fun unbindChannelPins(channelId: String) {
        messagePinsByChannelId.remove(channelId)?.forEach { it.unbind() }
        reactionPinsByChannelId.remove(channelId)?.values?.forEach { it.unbind() }
    }

    private fun toModel(channel: Bisq2TwoPartyPrivateChatChannel): TwoPartyPrivateChatChannel {
        // Resolved against the network store rather than read off the persisted channel, as desktop
        // does (PrivateChatsController): that picks up the peer's editable fields — publishDate, terms,
        // statement — and falls back to the embedded copy when the profile has been pruned.
        val peer = userProfileService.getManagedUserProfile(channel.peer)
        return channel.toDomain(peer)
    }

    private fun refreshUnreadCounts() {
        _channels.value.forEach { model ->
            findChannel(model.id)?.let { refreshUnreadCount(it, model) }
        }
    }

    private fun refreshUnreadCount(
        channel: Bisq2TwoPartyPrivateChatChannel,
        model: TwoPartyPrivateChatChannel,
    ) {
        model.setUnreadCount(chatNotificationService.getNumNotifications(channel))
    }

    private suspend fun addOrRemoveChatMessageReaction(
        channelId: String,
        messageId: String,
        reactionEnum: ReactionEnum,
        isRemoved: Boolean,
    ): Result<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                val channel = requireChannel(channelId)
                val message =
                    channel.chatMessages.find { it.id == messageId }
                        ?: error("No message found in the private chat channel")
                // Deliberately NOT awaited, unlike sendChatMessage above. The asymmetry is inherited,
                // not invented here: `NodeTradeChatMessagesServiceFacade` awaits its `sendTextMessage`
                // and drops the future of its `sendTextMessageReaction` in exactly the same way.
                //
                // It also holds up on its own. `sendMessageReaction` is structurally identical to
                // `sendMessage` — adds the reaction locally, then returns the `confidentialSend`
                // future — so awaiting it would put a Tor round trip behind every emoji tap, to buy a
                // signal Bisq Connect cannot produce anyway, since `PrivateChatRestApi` fires this one
                // and answers 204 exactly as it does for a message.
                //
                // What still reaches the caller: a missing channel or message, both raised above inside
                // this runCatching, and on Connect the whole REST failure including the 403 for a
                // withheld permission. What does not: a banned sender or peer, which
                // `PrivateChatChannelService` reports as an already-failed future rather than a throw.
                channelService.sendTextMessageReaction(
                    message,
                    channel,
                    Mappings.ReactionMapping.toBisq2Model(reactionEnum),
                    isRemoved,
                )
                Unit
            }
        }
}
