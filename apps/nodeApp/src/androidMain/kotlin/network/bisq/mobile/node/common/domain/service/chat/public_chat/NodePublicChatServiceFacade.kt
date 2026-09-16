package network.bisq.mobile.node.common.domain.service.chat.public_chat

import bisq.chat.ChatChannelDomain
import bisq.chat.ChatService
import bisq.chat.common.CommonPublicChatChannelService
import bisq.chat.notifications.ChatNotificationService
import bisq.common.observable.Pin
import bisq.common.observable.collection.CollectionObserver
import bisq.network.p2p.services.data.BroadcastResult
import bisq.user.banned.BannedUserService
import bisq.user.identity.UserIdentity
import bisq.user.identity.UserIdentityService
import bisq.user.profile.UserProfile
import bisq.user.profile.UserProfileService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.bisq.mobile.data.replicated.chat.Citation
import network.bisq.mobile.data.replicated.chat.common.CommonPublicChatChannel
import network.bisq.mobile.data.replicated.chat.reactions.CommonPublicChatMessageReaction
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.data.service.ServiceFacade
import network.bisq.mobile.data.service.chat.public_chat.PublicChatNotAuthorException
import network.bisq.mobile.data.service.chat.public_chat.PublicChatRemovalRejectedException
import network.bisq.mobile.data.service.chat.public_chat.PublicChatSendRefusedException
import network.bisq.mobile.data.service.chat.public_chat.PublicChatSendRejection
import network.bisq.mobile.data.service.chat.public_chat.PublicChatServiceFacade
import network.bisq.mobile.node.common.domain.mapping.Mappings
import network.bisq.mobile.node.common.domain.mapping.chat.toDomain
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiConsumer
import kotlin.jvm.optionals.getOrNull
import bisq.chat.ChatMessage as Bisq2ChatMessage
import bisq.chat.common.CommonPublicChatChannel as Bisq2CommonPublicChatChannel
import bisq.chat.common.CommonPublicChatMessage as Bisq2CommonPublicChatMessage
import bisq.chat.reactions.CommonPublicChatMessageReaction as Bisq2CommonPublicChatMessageReaction

/**
 * The public chat channels — Discussions and Support — backed by bisq2's per-domain
 * `CommonPublicChatChannelService`.
 *
 * Mirrors bisq2's own REST API (`PublicChatRestApi` and `PublicChatChannels`) rather than the
 * desktop, so both mobile flavours behave the same. What that costs, and why:
 *  - the domain to service map is **copied** on activation: `ChatService.shutdown` empties its own,
 *    so a facade holding it by reference would lose its publishers across a lifecycle restart
 *  - deprecated sub-domains are **not** filtered out. A store upgraded from before v2.1.1 still
 *    holds the consolidated channel, but `CommonPublicChatChannel.getId()` answers the migrated id
 *    and the channel's equality *is* that id, so the set already holds one channel per domain.
 *    Filtering would drop the only Discussions channel such a node has; the surviving instance
 *    decides the title, not the id
 *  - every bisq2 value is read through its getter, never a raw field, because `getId()` and
 *    `getChatChannelDomain()` are where that migration happens
 *  - messages are filtered by bisq2's own `PublicChatDtoFactory.isVisible`, so history and live
 *    stream cannot disagree; removals deliberately bypass that filter
 *
 * Unlike the private facade there is no channel-ownership machinery: the channel set is fixed before
 * the service initialises, and nothing creates, leaves or replaces a channel at runtime.
 */
class NodePublicChatServiceFacade(
    applicationService: AndroidApplicationService.Provider,
) : ServiceFacade(),
    PublicChatServiceFacade {
    // Dependencies
    private val chatService: ChatService by lazy { applicationService.chatService.get() }
    private val chatNotificationService: ChatNotificationService by lazy { chatService.chatNotificationService }
    private val userProfileService: UserProfileService by lazy { applicationService.userService.get().userProfileService }
    private val userIdentityService: UserIdentityService by lazy { applicationService.userService.get().userIdentityService }
    private val bannedUserService: BannedUserService by lazy { applicationService.userService.get().bannedUserService }

    override val isSupported: Flow<Boolean> = flowOf(true)

    private val _channels = MutableStateFlow<List<CommonPublicChatChannel>>(emptyList())
    override val channels: StateFlow<List<CommonPublicChatChannel>> = _channels.asStateFlow()

    /** Copied on activation; see the class KDoc. */
    private val servicesByDomain: MutableMap<ChatChannelDomain, CommonPublicChatChannelService> = ConcurrentHashMap()

    // Concurrent for the same reason the private facade's are: bisq2 runs the observers that write
    // these on whichever thread mutated the collection, and deactivate() drains them from a third.
    private val messagePinsByChannelId: MutableMap<String, MutableSet<Pin>> = ConcurrentHashMap()
    private val reactionBindingsByMessageId: MutableMap<String, ReactionBinding> = ConcurrentHashMap()
    private var notificationsPin: Pin? = null

    /**
     * The reaction observer together with the exact message **instance** it was bound for.
     *
     * The P2P store re-delivers a message as a fresh instance that is `equals` to the one it replaced,
     * and `ObservableCollection#remove` drops the element before notifying — so `onRemoved(old)` can
     * arrive after `onAdded(new)` has bound its observer. Keyed by id alone, that removal would unbind
     * the live message's observer and drop it from the model. Compared by reference, it is a no-op.
     */
    private class ReactionBinding(
        val owner: Bisq2CommonPublicChatMessage,
        val pin: Pin,
    )

    /**
     * A message that passed every permanent check but arrived before a dependency it cannot show
     * without: its author's profile, or a selected identity. On a fresh install the initial
     * inventory delivers messages and profiles as separate data types in arbitrary order — and the
     * facade activates while the user is still in onboarding — so dropping these left a brand-new
     * node's Discussions empty until a restart replayed the persisted store. Parked instead and
     * replayed when a profile lands or an identity is selected: the same push-or-park treatment
     * bisq2's API layer gives Connect clients. Keyed by message id, purged by INSTANCE — the P2P
     * store re-delivers equal instances, same trap [ReactionBinding] documents.
     */
    private class ParkedMessage(
        val channel: Bisq2CommonPublicChatChannel,
        val model: CommonPublicChatChannel,
        val message: Bisq2CommonPublicChatMessage,
    )

    private companion object {
        /** Above bisq2's own TTL'd cap on the message store, so a full fresh-install burst fits. */
        const val PARKED_MESSAGES_CAP = 4096
    }

    private val parkedByMessageId: MutableMap<String, ParkedMessage> = ConcurrentHashMap()
    private var numUserProfilesPin: Pin? = null
    private var selectedIdentityPin: Pin? = null

    /**
     * Conflated like [unreadRefreshSignal] and for the same reason: profile sync arrives in
     * bursts of hundreds, and what matters is that a replay happens after the burst, not once
     * per profile.
     */
    private val parkedRetrySignal =
        MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /**
     * `changedNotification` fires once per message for *every* chat domain, and most of them are trade
     * chat, so a trade burst would otherwise turn into one scan of the notification set per channel per
     * message. Conflated: what matters is that a recount happens after the burst, not how many.
     */
    private val unreadRefreshSignal =
        MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /**
     * Whether the bisq2 observers below may still write into this facade's model. bisq2 calls them
     * back on its own threads, so one already in flight can land after [deactivate] has drained the
     * pin maps — and a pin added after that drain is never unbound.
     */
    @Volatile
    private var isObserving = false

    override suspend fun activate() {
        super<ServiceFacade>.activate()

        // Off Main for the whole activation, not only for the mutations further down. The facades are
        // activated on serviceScope, which is Main.immediate, and `addObserver` replays the channel's
        // entire message set synchronously before it returns — ten days of TTL'd history, each message
        // filtered, mapped and folded into the model. Nothing in here needs Main: bisq2's observer
        // lists are copy-on-write, its collections concurrent, and the models are MutableStateFlows.
        withContext(Dispatchers.Default) {
            isObserving = true
            servicesByDomain.putAll(chatService.commonPublicChatChannelServices)

            // Dispatchers.Default spelled out again because launch takes serviceScope's context rather
            // than this one, and a recount scans bisq2's notification set once per channel. Guarded
            // because one throw would cancel the collector, leaving the hub badge frozen for the rest
            // of the process with nothing on screen to say so. Started UNDISPATCHED so the collector
            // is subscribed before the observers below are registered: bisq2's `addObserver` replays
            // the current value synchronously, and the signals have no replay, so an emit that
            // beats the subscription is dropped for good.
            launchRefreshUnreadCountsJob()

            // Same guarded-collector rationale as the unread recount above.
            launchRetryParkedMessagesJob()

            servicesByDomain.values.forEach { service ->
                service.channels.forEach { observeChannel(it) }
            }

            // bisq2 deliberately emits null here to force observers to re-evaluate (it excludes
            // isConsumed from equals), so a null must not be treated as an event payload.
            notificationsPin = chatNotificationService.changedNotification.addObserver { unreadRefreshSignal.tryEmit(Unit) }
            // The two events that can make a parked message showable: an author profile landing
            // (numUserProfiles moves on every store change) and an identity being selected.
            numUserProfilesPin = userProfileService.numUserProfiles.addObserver { parkedRetrySignal.tryEmit(Unit) }
            selectedIdentityPin = userIdentityService.selectedUserIdentityObservable.addObserver { parkedRetrySignal.tryEmit(Unit) }
        }
    }

    private fun launchRetryParkedMessagesJob() {
        serviceScope.launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
            parkedRetrySignal.collect {
                try {
                    retryParkedMessages()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.w(e) { "Failed to replay parked public chat messages" }
                }
            }
        }
    }

    private fun launchRefreshUnreadCountsJob() {
        serviceScope.launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
            unreadRefreshSignal.collect {
                try {
                    refreshUnreadCounts()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.w(e) { "Failed to refresh public chat unread counts" }
                }
            }
        }
    }

    override suspend fun deactivate() {
        // Off Main for the same reason activate() is: this drains one pin per message that was ever
        // visible across both channels, and bisq2 caps that store in the thousands.
        withContext(Dispatchers.Default) {
            isObserving = false
            notificationsPin?.unbind()
            notificationsPin = null
            numUserProfilesPin?.unbind()
            numUserProfilesPin = null
            selectedIdentityPin?.unbind()
            selectedIdentityPin = null

            messagePinsByChannelId.values.forEach { pins -> pins.forEach { it.unbind() } }
            messagePinsByChannelId.clear()
            reactionBindingsByMessageId.values.forEach { it.pin.unbind() }
            reactionBindingsByMessageId.clear()
            parkedByMessageId.clear()
            servicesByDomain.clear()
            _channels.value = emptyList()
        }

        super<ServiceFacade>.deactivate()
    }

    override suspend fun sendChatMessage(
        channelId: String,
        text: String,
        citation: Citation?,
    ): Result<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                val channel = requireChannel(channelId)
                requireValidText(text)
                // A public channel has no identity of its own, so a send goes out as the selected one.
                val sender = requireSelectedIdentity()
                refuseIfBannedOrRateLimited(sender.id)
                val bisq2Citation = Optional.ofNullable(citation?.let { Mappings.CitationMapping.toBisq2Model(it) })
                // Not awaited, as `PublicChatRestApi` does not await it either: the local add happens
                // synchronously inside the call and only the broadcast is async, so awaiting would put a
                // Tor round trip behind a message the user can already see.
                serviceOf(channel)
                    .publishChatMessage(text, bisq2Citation, channel, sender)
                    .whenComplete(logIfFailed("Publishing a public chat message"))
                Unit
            }.onFailure { currentCoroutineContext().ensureActive() }
        }

    override suspend fun editChatMessage(
        channelId: String,
        messageId: String,
        text: String,
    ): Result<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                val channel = requireChannel(channelId)
                requireValidText(text)
                val message = requireMessage(channel, messageId)
                val author = requireAuthor(message)
                // Not cosmetic: the domain removes the original and only then runs its own ban and rate
                // limit checks, so a refusal inside the domain would lose the message.
                refuseIfBannedOrRateLimited(author.id)
                serviceOf(channel).publishEditedChatMessage(message, text, author).await()
                requireRemoved(channel.chatMessages.contains(message), "original message")
            }.onFailure { currentCoroutineContext().ensureActive() }
        }

    override suspend fun deleteChatMessage(
        channelId: String,
        messageId: String,
    ): Result<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                val channel = requireChannel(channelId)
                val message = requireMessage(channel, messageId)
                val author = requireAuthor(message)
                // The domain has no check on the delete itself, but its removal listener
                // (`processRemovedMessage` -> `isValid`) drops the LOCAL removal for a banned or
                // rate-limited author while the network removal proceeds. The store would still answer
                // success, so a retry finds nothing left to remove and the message is stuck here for good.
                refuseIfBannedOrRateLimited(author.id)
                serviceOf(channel).deleteChatMessage(message, author.networkIdWithKeyPair).await()
                requireRemoved(channel.chatMessages.contains(message), "message")
            }.onFailure { currentCoroutineContext().ensureActive() }
        }

    override suspend fun addChatMessageReaction(
        channelId: String,
        messageId: String,
        reactionEnum: ReactionEnum,
    ): Result<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                val channel = requireChannel(channelId)
                val message = requireMessage(channel, messageId)
                val sender = requireSelectedIdentity()
                // Mandatory: the domain answers a banned sender with a future failed on a bare
                // RuntimeException carrying no message, and the publish below is fire-and-forget.
                refuseIfBannedOrRateLimited(sender.id)
                // Idempotent per identity: the local add inside the domain call is synchronous, so a
                // retry finds the reaction here and no-ops.
                val bisq2Reaction = Mappings.ReactionMapping.toBisq2Model(reactionEnum)
                if (findReaction(message, sender.id, bisq2Reaction.ordinal) == null) {
                    serviceOf(channel)
                        .publishChatMessageReaction(message, bisq2Reaction, sender)
                        .whenComplete(logIfFailed("Publishing a public chat reaction"))
                }
                Unit
            }.onFailure { currentCoroutineContext().ensureActive() }
        }

    override suspend fun removeChatMessageReaction(
        channelId: String,
        messageId: String,
        reaction: CommonPublicChatMessageReaction,
    ): Result<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                val channel = requireChannel(channelId)
                val message = requireMessage(channel, messageId)
                // The reaction names whose it is; a public message carries reactions from many people
                // and I may hold several identities, so falling back to the selected one would guess.
                val owner =
                    userIdentityService.findUserIdentity(reaction.userProfileId).getOrNull()
                        ?: throw PublicChatNotAuthorException()
                // No ban pre-check here, matching bisq2: `removeMessageReaction` has no check to lose
                // the removal to, unlike its adding counterpart.
                val bisq2Reaction = findReaction(message, reaction.userProfileId, reaction.reactionId)
                if (bisq2Reaction != null) {
                    serviceOf(channel).deleteChatMessageReaction(bisq2Reaction, owner.networkIdWithKeyPair).await()
                    requireRemoved(message.chatMessageReactions.contains(bisq2Reaction), "reaction")
                }
            }.onFailure { currentCoroutineContext().ensureActive() }
        }

    override suspend fun consumeNotifications(channelId: String) {
        withContext(Dispatchers.Default) {
            findChannel(channelId)?.let { chatNotificationService.consume(it) }
        }
    }

    // Private

    /** Call from inside a `runCatching`, where the failure becomes the `Result.failure`. */
    private fun requireChannel(channelId: String): Bisq2CommonPublicChatChannel = findChannel(channelId) ?: error("No public chat channel found")

    private fun requireMessage(
        channel: Bisq2CommonPublicChatChannel,
        messageId: String,
    ): Bisq2CommonPublicChatMessage = channel.chatMessages.find { it.id == messageId } ?: error("No message found in the public chat channel")

    private fun requireSelectedIdentity(): UserIdentity = userIdentityService.selectedUserIdentity ?: error("No user identity is selected on this node")

    /** bisq2 resolves the author among ALL my identities, not only the selected one. */
    private fun requireAuthor(message: Bisq2CommonPublicChatMessage): UserIdentity = userIdentityService.findUserIdentity(message.authorUserProfileId).getOrNull() ?: throw PublicChatNotAuthorException()

    /** The checks the domain makes too late, or not at all, made before it is called. */
    private fun refuseIfBannedOrRateLimited(userProfileId: String) {
        if (bannedUserService.isUserProfileBanned(userProfileId)) {
            throw PublicChatSendRefusedException(PublicChatSendRejection.MY_PROFILE_BANNED)
        }
        if (bannedUserService.isRateLimitExceeding(userProfileId)) {
            throw PublicChatSendRefusedException(PublicChatSendRejection.RATE_LIMIT_EXCEEDED)
        }
    }

    /**
     * `CommonPublicChatMessage` verifies its text on construction, and the edit path constructs the
     * replacement *after* removing the original — so an oversized text has to be refused before the
     * domain sees it. Blank is refused by no message class at all. Same two checks as bisq2's
     * `ChatRequestValidation.textError`.
     */
    private fun requireValidText(text: String) {
        require(text.isNotBlank()) { "The message text must not be empty" }
        require(text.length <= Bisq2ChatMessage.MAX_TEXT_LENGTH) {
            "The message text must not be longer than ${Bisq2ChatMessage.MAX_TEXT_LENGTH} characters"
        }
    }

    /**
     * A completed future does not mean the removal happened: the store answers a rejected removal with a
     * successful, empty result and no event, leaving the message or reaction here while the network
     * forgets it.
     */
    private fun requireRemoved(
        stillPresent: Boolean,
        what: String,
    ) {
        if (stillPresent) {
            throw PublicChatRemovalRejectedException(what)
        }
    }

    /**
     * The service that owns the channel. Asserted rather than left implicit, as bisq2 does: without it
     * the caller gets the map's bare null and the failure says nothing about which domain went missing.
     */
    private fun serviceOf(channel: Bisq2CommonPublicChatChannel): CommonPublicChatChannelService =
        requireNotNull(servicesByDomain[channel.chatChannelDomain]) {
            "No CommonPublicChatChannelService is registered for domain ${channel.chatChannelDomain}"
        }

    private fun findReaction(
        message: Bisq2CommonPublicChatMessage,
        userProfileId: String,
        reactionId: Int,
    ): Bisq2CommonPublicChatMessageReaction? =
        message.chatMessageReactions
            .filterIsInstance<Bisq2CommonPublicChatMessageReaction>()
            .find { it.reactionId == reactionId && it.userProfileId == userProfileId }

    /** The publishes above are not awaited, so this is the only place their failure is seen. */
    private fun logIfFailed(action: String) =
        BiConsumer<BroadcastResult, Throwable?> { _, throwable ->
            if (throwable != null) {
                log.w(throwable) { "$action failed after the call was answered" }
            }
        }

    private fun observeChannel(channel: Bisq2CommonPublicChatChannel) {
        val model = channel.toDomain()
        // update, not `value = value...`, and sorted by domain so the list a presenter reads never
        // depends on the service map's iteration order.
        _channels.update { current ->
            (current.filterNot { it.id == model.id } + model).sortedBy { it.chatChannelDomain.ordinal }
        }
        model.setUnreadCount(chatNotificationService.getNumNotifications(channel))

        // addObserver replays the whole existing message set synchronously, so the pin is folded in
        // afterwards: publishing an empty set first would leave a deactivate landing in that window
        // with nothing to unbind.
        val pin =
            channel.chatMessages.addObserver(
                // onAllAdded is not overridden: CollectionObserver already defines it as
                // values.forEach(this::onAdded), which is what the initial replay needs.
                object : CollectionObserver<Bisq2CommonPublicChatMessage> {
                    override fun onAdded(element: Bisq2CommonPublicChatMessage) {
                        addMessageToModel(channel, model, element)
                    }

                    override fun onRemoved(element: Any) {
                        if (element is Bisq2CommonPublicChatMessage) {
                            removeMessageFromModel(model, element)
                        }
                    }

                    override fun onCleared() {
                        clearMessages(model)
                    }
                },
            )
        messagePinsByChannelId.compute(model.id) { _, existing ->
            (existing ?: ConcurrentHashMap.newKeySet<Pin>()).also { it += pin }
        }
        if (!isObserving) {
            pin.unbind()
            // This pin only, symmetric with the additive compute above: the map holds a set per channel.
            messagePinsByChannelId.computeIfPresent(model.id) { _, pins ->
                pins -= pin
                pins.ifEmpty { null }
            }
        }
    }

    private fun addMessageToModel(
        channel: Bisq2CommonPublicChatChannel,
        model: CommonPublicChatChannel,
        message: Bisq2CommonPublicChatMessage,
    ) {
        // Permanent verdicts drop for good — a parked copy of either would only replay a message
        // that must never show.
        if (message.isExpired || bannedUserService.isUserProfileBanned(message.authorUserProfileId)) {
            purgePark(message)
            return
        }
        // The profile store is pruned concurrently, so an author can vanish and reappear; on a
        // fresh install the whole inventory burst can precede its authors. Missing here is
        // "not yet", never "no" — park and let a store change retry. Same for the identity,
        // which does not exist until onboarding creates one, while facades activate earlier.
        val author = findUserProfile(message.authorUserProfileId)
        val myUserProfile = userIdentityService.selectedUserIdentity?.userProfile
        if (author == null || myUserProfile == null) {
            park(channel, model, message)
            return
        }
        purgePark(message)

        bindReactions(channel, model, message)

        val citationAuthorUserProfile =
            message.citation
                .flatMap { userProfileService.findUserProfile(it.authorUserProfileId) }
                .orElse(null)

        model.addChatMessage(
            message.toDomain(
                author = author,
                citationAuthorUserProfile = citationAuthorUserProfile,
                myUserProfile = myUserProfile,
                // bisq2's own authorization rule: ANY of my identities, not only the selected one.
                // This is what gates the Edit and Delete menu items.
                isMyMessage = userIdentityService.findUserIdentity(message.authorUserProfileId).isPresent,
                visibleReactions = visibleReactionsOf(message),
            ),
        )
    }

    /**
     * Deliberately not gated on [isVisible]: a message that expired or whose author was banned after it
     * was shown still has to be taken back, and gating the removal on the filter that admitted it would
     * leave it on screen for good.
     */
    private fun removeMessageFromModel(
        model: CommonPublicChatChannel,
        message: Bisq2CommonPublicChatMessage,
    ) {
        // By instance, like the reaction binding below: a stale removal arriving after an equal
        // re-delivered instance parked itself must not drop the live instance's park.
        parkedByMessageId.computeIfPresent(message.id) { _, parked ->
            if (parked.message === message) null else parked
        }
        val binding = reactionBindingsByMessageId[message.id]
        if (binding != null && binding.owner !== message) {
            // A newer instance of the same message has already taken over; see [ReactionBinding].
            return
        }
        reactionBindingsByMessageId.remove(message.id)?.pin?.unbind()
        model.removeChatMessage(message.id)
    }

    private fun clearMessages(model: CommonPublicChatChannel) {
        parkedByMessageId.values.removeAll { it.model === model }
        model.chatMessages.value.forEach { reactionBindingsByMessageId.remove(it.id)?.pin?.unbind() }
        model.setAllChatMessages(emptySet())
    }

    private fun park(
        channel: Bisq2CommonPublicChatChannel,
        model: CommonPublicChatChannel,
        message: Bisq2CommonPublicChatMessage,
    ) {
        if (parkedByMessageId.size >= PARKED_MESSAGES_CAP && message.id !in parkedByMessageId) {
            // Above the cap of bisq2's own TTL'd message store, so only reachable if something
            // upstream floods; dropping the newcomer keeps the park bounded either way.
            log.w { "Parked public chat messages at cap ($PARKED_MESSAGES_CAP) - dropping ${message.id}" }
            return
        }
        // A re-delivered equal instance replaces its predecessor as the park's owner.
        parkedByMessageId[message.id] = ParkedMessage(channel, model, message)
    }

    private fun purgePark(message: Bisq2CommonPublicChatMessage) {
        parkedByMessageId.remove(message.id)
    }

    private fun retryParkedMessages() {
        if (parkedByMessageId.isEmpty()) {
            return
        }
        // Snapshot: addMessageToModel re-parks whatever still cannot resolve, and iterating the
        // live map while it writes back would revisit those entries forever.
        parkedByMessageId.values.toList().forEach { parked ->
            // remove(key, value) is by-instance (ParkedMessage has identity equality): a successor
            // parked after this snapshot keeps its own entry and its own retry.
            if (parkedByMessageId.remove(parked.message.id, parked)) {
                addMessageToModel(parked.channel, parked.model, parked.message)
            }
        }
    }

    private fun bindReactions(
        channel: Bisq2CommonPublicChatChannel,
        model: CommonPublicChatChannel,
        message: Bisq2CommonPublicChatMessage,
    ) {
        val messageId = message.id
        if (reactionBindingsByMessageId[messageId]?.owner === message) {
            return
        }
        val pin =
            message.chatMessageReactions.addObserver {
                model.chatMessages.value
                    .find { it.id == messageId }
                    ?.setReactions(visibleReactionsOf(message).map { it.toDomain() })
            }
        reactionBindingsByMessageId.put(messageId, ReactionBinding(message, pin))?.pin?.unbind()

        // addObserver replays synchronously, so a removal — or a deactivate — landing while we bind
        // would have found nothing to unbind. By reference, never `contains`: an equal successor is a
        // different owner.
        if ((!isObserving || channel.chatMessages.none { it === message }) &&
            reactionBindingsByMessageId[messageId]?.owner === message
        ) {
            reactionBindingsByMessageId.remove(messageId)
            pin.unbind()
        }
    }

    /** bisq2's `PublicChatDtoFactory.isVisible`. */
    private fun isVisible(message: Bisq2CommonPublicChatMessage): Boolean = !message.isExpired && isVisibleAuthor(message.authorUserProfileId)

    private fun isVisibleAuthor(userProfileId: String): Boolean = !bannedUserService.isUserProfileBanned(userProfileId) && findUserProfile(userProfileId) != null

    /**
     * Checked at both levels, as bisq2 does: the message's author decides whether the reaction has a
     * message to sit on at all, the reaction's own sender whether it is shown on one that is visible.
     */
    private fun visibleReactionsOf(message: Bisq2CommonPublicChatMessage): List<Bisq2CommonPublicChatMessageReaction> {
        if (!isVisible(message)) {
            return emptyList()
        }
        return message.chatMessageReactions
            .filterIsInstance<Bisq2CommonPublicChatMessageReaction>()
            .filter { isVisibleAuthor(it.userProfileId) }
    }

    private fun findUserProfile(userProfileId: String): UserProfile? = userProfileService.findUserProfile(userProfileId).getOrNull()

    private fun refreshUnreadCounts() {
        _channels.value.forEach { model ->
            findChannel(model.id)?.let { model.setUnreadCount(chatNotificationService.getNumNotifications(it)) }
        }
    }

    /**
     * Over the services' own channel sets rather than through `ChatChannelService.findChannel`, as
     * bisq2's `PublicChatChannels` does: the ids compared here are the migrated ones the getters
     * answer, which is what a caller addressing `discussion.bisq` has.
     */
    private fun findChannel(channelId: String): Bisq2CommonPublicChatChannel? =
        servicesByDomain.values
            .flatMap { it.channels }
            .find { it.id == channelId }
}
