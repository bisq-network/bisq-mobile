package network.bisq.mobile.data.model

import kotlinx.serialization.Serializable

/**
 * The global Community notifications preference (#1812): how much public-chat activity the app
 * delivers as notifications. Mirrors the semantics of bisq2 desktop's ChatChannelNotificationType
 * global default (ALL / MENTION / OFF) — MENTIONS_AND_REPLIES is desktop's MENTION, which treats
 * a citation of one of my messages like a mention. App-local (DataStore), never synced to the
 * node: desktop keeps its own.
 */
@Serializable
enum class CommunityNotificationLevel {
    ALL,
    MENTIONS_AND_REPLIES,
    OFF,
}
