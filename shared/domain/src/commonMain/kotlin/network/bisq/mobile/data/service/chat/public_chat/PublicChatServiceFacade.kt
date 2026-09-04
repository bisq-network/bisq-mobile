package network.bisq.mobile.data.service.chat.public_chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.data.replicated.chat.Citation
import network.bisq.mobile.data.replicated.chat.common.CommonPublicChatChannel
import network.bisq.mobile.data.replicated.chat.reactions.CommonPublicChatMessageReaction
import network.bisq.mobile.data.replicated.chat.reactions.ReactionEnum
import network.bisq.mobile.data.service.LifeCycleAware

/**
 * The public chat channels: Discussions and Support, one channel per domain.
 *
 * Both domains are served from here even though this app version only shows Discussions, because
 * bisq2 exposes them as one surface (`PublicChatChannels`) and splitting them would mean two facades
 * over one service map. The hub's unread badge filters Support out on its own.
 *
 * Every operation is addressed by channel id, like
 * [network.bisq.mobile.data.service.chat.private_chat.PrivateChatServiceFacade] and unlike trade
 * chat's ambient selection.
 *
 * The failures are a closed set, so a presenter needs one error handler for both flavours:
 * [PublicChatSendRefusedException] for a banned or rate-limited profile,
 * [PublicChatNotAuthorException] for a message that is not mine, [PublicChatRemovalRejectedException]
 * for a removal the local store refused, and an [IllegalStateException] for a channel or message that
 * is gone.
 */
interface PublicChatServiceFacade : LifeCycleAware {
    /**
     * Always true on the node, which runs the channels in-process. On Bisq Connect it tracks whether
     * the paired trusted node advertises the `public-chat` capability — an older node exposes none of
     * these endpoints, so callers must hide the entry point rather than let the calls fail.
     *
     * A flow rather than a snapshot, for the reason `PrivateChatServiceFacade.isSupported` documents:
     * on Bisq Connect the capability set starts at the legacy baseline, so a caller that reads it once
     * can latch "unsupported" for good.
     */
    val isSupported: Flow<Boolean>

    /** One channel per domain: `discussion.bisq` and `support.support`. */
    val channels: StateFlow<List<CommonPublicChatChannel>>

    /** Sends as the node's selected identity — a public channel has no identity of its own. */
    suspend fun sendChatMessage(
        channelId: String,
        text: String,
        citation: Citation?,
    ): Result<Unit>

    /**
     * Publishes an edit as the message's author, which must be one of my identities. bisq2 models an
     * edit as a removal plus a new message carrying the original date and `wasEdited = true`.
     */
    suspend fun editChatMessage(
        channelId: String,
        messageId: String,
        text: String,
    ): Result<Unit>

    suspend fun deleteChatMessage(
        channelId: String,
        messageId: String,
    ): Result<Unit>

    /** Idempotent per identity: adding a reaction that is already there does nothing. */
    suspend fun addChatMessageReaction(
        channelId: String,
        messageId: String,
        reactionEnum: ReactionEnum,
    ): Result<Unit>

    /**
     * Takes the reaction itself rather than a reaction id, because a public message carries reactions
     * from many people and I may hold several identities: only the reaction names whose it is.
     * Succeeds when the reaction is already gone.
     */
    suspend fun removeChatMessageReaction(
        channelId: String,
        messageId: String,
        reaction: CommonPublicChatMessageReaction,
    ): Result<Unit>

    /** Marks every message in the channel as read. Backed by bisq2's persisted notification store. */
    suspend fun consumeNotifications(channelId: String)
}
