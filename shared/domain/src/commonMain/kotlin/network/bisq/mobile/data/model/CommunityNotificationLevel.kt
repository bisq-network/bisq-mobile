package network.bisq.mobile.data.model

import kotlinx.serialization.Serializable

/**
 * How much public-chat activity the app delivers as notifications. Mirrors the semantics of bisq2
 * desktop's ChatChannelNotificationType global default (ALL / MENTION / OFF) — MENTIONS_AND_REPLIES
 * is desktop's MENTION, which treats a citation of one of my messages like a mention. App-local
 * (DataStore), never synced to the node: desktop keeps its own. Defaults to ALL, like desktop.
 * Besides notifications the level also filters the public channels' contribution to the Community
 * badge (`CommunityUnreadCountAggregator`), again matching desktop, whose nav badges count
 * notifications.
 */
@Serializable
enum class CommunityNotificationLevel {
    ALL,
    MENTIONS_AND_REPLIES,
    OFF,
}
