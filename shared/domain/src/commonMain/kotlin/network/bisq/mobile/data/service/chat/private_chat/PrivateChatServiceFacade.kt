package network.bisq.mobile.data.service.chat.private_chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.data.replicated.chat.Citation
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatChannel
import network.bisq.mobile.data.replicated.chat.two_party.TwoPartyPrivateChatMessageReaction
import network.bisq.mobile.data.service.LifeCycleAware

/**
 * Two-party private chat (DM) with an arbitrary peer, outside of a trade.
 *
 * Every operation is addressed by channel id, deliberately unlike
 * [network.bisq.mobile.data.service.chat.trade.TradeChatMessagesServiceFacade], which reads the
 * currently selected trade as ambient state. Ambient selection cannot serve a channel that is not
 * on screen, which notifications need.
 */
interface PrivateChatServiceFacade : LifeCycleAware {
    /**
     * Always true on the node. On Bisq Connect it tracks whether the paired trusted node advertises
     * the private-chat capability — an older node exposes none of the endpoints below, so callers
     * must hide the entry point rather than let the calls fail.
     *
     * Capability is not permission: the node advertises `private-chat` from a public, unauthenticated
     * endpoint, so this can be true while the pairing withheld the private-chat permission. The calls
     * below then fail with [PrivateChatNotPermittedException], not here.
     *
     * That permission covers the REST routes and nothing else. Bisq 2 authenticates a WebSocket at the
     * handshake but never authorises it — `SubscriptionService` has no permission check and there is no
     * WebSocket counterpart to `RestApiAuthorizationFilter` — so a pairing without the permission is
     * refused every call below and still receives the DMs themselves over the `PRIVATE_CHAT_*` topics.
     * Pre-existing and true of every topic, trade chat included.
     *
     * A flow rather than a snapshot: on Bisq Connect the capability set starts at the legacy baseline
     * and only becomes accurate once the node's manifest lands, so a caller that reads it once can
     * latch "unsupported" for good and hide the entry point with no way back.
     */
    val isSupported: Flow<Boolean>

    val channels: StateFlow<List<TwoPartyPrivateChatChannel>>

    /**
     * Finds the existing channel with the peer or creates one, and returns its id. Creating is a
     * purely local operation in Bisq 2 — nothing is sent until the first message, so the peer learns
     * nothing from this call.
     */
    suspend fun findOrCreateChannel(peerProfileId: String): Result<String>

    suspend fun sendChatMessage(
        channelId: String,
        text: String,
        citation: Citation?,
    ): Result<Unit>

    suspend fun addChatMessageReaction(
        channelId: String,
        messageId: String,
        reactionEnum: ReactionEnum,
    ): Result<Unit>

    /** @return false when the reaction is not ours to remove. */
    suspend fun removeChatMessageReaction(
        channelId: String,
        messageId: String,
        reaction: TwoPartyPrivateChatMessageReaction,
    ): Result<Boolean>

    /**
     * Leaves and deletes the channel locally. One-sided and irreversible: the peer keeps their copy of
     * the conversation and sees a "left" system message. Bisq 2 sends that message only when the
     * channel has messages — an empty channel never reached the peer in the first place.
     */
    suspend fun leaveChannel(channelId: String): Result<Unit>

    /** Marks every message in the channel as read. Backed by Bisq 2's persisted notification store. */
    suspend fun consumeNotifications(channelId: String)
}

/**
 * The paired trusted node runs a version that has private chat, but this pairing was not granted the
 * permission for it.
 *
 * Its own type rather than a status code, so presenters in `:shared:presentation` can tell this apart
 * from a dropped connection without depending on the client app's HTTP types. Only the Bisq Connect
 * flavour can produce it — the node flavour has no permission layer.
 */
class PrivateChatNotPermittedException : Exception("The paired trusted node did not grant permission for private chats")
