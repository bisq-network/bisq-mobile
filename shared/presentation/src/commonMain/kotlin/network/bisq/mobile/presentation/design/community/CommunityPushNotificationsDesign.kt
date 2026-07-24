/**
 * CommunityPushNotificationsDesign.kt — Design PoC (Milestone 11 "Bisq community")
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * Defines the notification COPY (title/body strings) and deep-link behaviour for the
 * community surface — new private message (#590) and channel mention/reply in
 * Discussions (#589) — for both the app-backgrounded and app-killed cases. This is a
 * documentation-and-mock-preview file: real system notifications are OS chrome, not
 * something Compose renders, so there is no real `NotificationCompat`/
 * `UNMutableNotificationContent` preview possible. See "WHY THESE ARE MOCK PREVIEWS"
 * below for exactly what IS and ISN'T previewable here.
 *
 * ======================================================================================
 * WHAT ALREADY EXISTS — aligned with, not reinvented (survey before designing)
 * ======================================================================================
 * This app already has a full, working, PRIVACY-CONSCIOUS notification pipeline for
 * trade messages. This file extends that exact pipeline; it does not propose a new one.
 *
 * 1. LOCAL notifications (app backgrounded but the process is alive — the default,
 *    non-relayed path):
 *    `OpenTradesNotificationService` (presentation/common/service/) observes trade chat
 *    messages and posts rich, full-content notifications via
 *    `NotificationController.notify { ... }` (presentation/common/notification/
 *    NotificationController.kt) — a builder DSL setting `id`, `title`, `body`, and a
 *    platform-specific `pressAction` (`NotificationPressAction.Route(NavRoute.X)` — see
 *    NotificationPressAction.kt) that deep-links a tap straight to the relevant screen.
 *    Existing trade-chat copy (mobile.properties):
 *    ```
 *    mobile.openTradeNotifications.newMessage.title   = Trade [{0}]
 *    mobile.openTradeNotifications.newMessage.message = You have a new message from {0}
 *    ```
 *    Posted on `NotificationChannels.USER_MESSAGES` (NotificationChannels.kt) — a channel
 *    that ALREADY EXISTS and is semantically exactly "a message from another user",
 *    which is what both DM and channel-mention notifications are. REUSED below, not
 *    duplicated with a new channel.
 *
 * 2. RELAYED notifications (app killed, or the user opted into relayed/FCM-APNs
 *    delivery — see `mobile.pushNotifications.optIn.*` copy already in mobile.properties):
 *    the trusted node encrypts a payload (AES-256-GCM) and pushes it through
 *    Google/Apple. CRITICALLY, this path is privacy-restricted by design — the decrypted
 *    content is NEVER shown in the system notification tray, only a generic,
 *    CATEGORY-based summary is:
 *    - Android: `BisqFirebaseMessagingService.NotificationCategory` (apps/clientApp/.../
 *      push_notification/) — `TRADE_UPDATE`, `CHAT_MESSAGE`, `OFFER_UPDATE`, `GENERAL`.
 *    - iOS: `NotificationService.swift`'s `NotificationCategory` enum (Notification
 *      Service Extension) — same four categories, same display text, decrypts
 *      server-side-encrypted payloads on-device before display but STILL only shows the
 *      category summary, never the plaintext.
 *    - Display text (mobile.properties, already exists):
 *      ```
 *      mobile.pushNotifications.category.tradeUpdate = Trade update
 *      mobile.pushNotifications.category.chatMessage  = New message
 *      mobile.pushNotifications.category.offerUpdate  = Offer update
 *      mobile.pushNotifications.category.general      = New notification
 *      ```
 *    - The system notification's title is ALWAYS the literal app name ("Bisq"), never a
 *      peer name or channel name, on both platforms.
 *    - Both platforms already carry an optional `tradeId` field in the decrypted payload
 *      (bisq-network/bisq-mobile#1395) so a tap can deep-link straight to the specific
 *      trade when the trusted node supplies it, falling back to the trade list when it
 *      doesn't (older trusted nodes). This exact fallback shape is reused below for
 *      community deep links.
 *
 * ======================================================================================
 * DECISION: NO NEW CATEGORY, NO NEW CHANNEL
 * ======================================================================================
 * Both DM messages and Discussions mentions/replies are, semantically and from a privacy
 * standpoint, the exact same thing the existing `CHAT_MESSAGE` category / `USER_MESSAGES`
 * channel already models: "a message from another user arrived, go look." Introducing a
 * separate `community_message` category would (a) require the trusted node to learn a
 * new wire value for no user-facing benefit, since the relayed-path display text is
 * identical either way ("New message"), and (b) add a 5th line to Android's
 * notification-channel settings list for a distinction only visible in the LOCAL
 * (rich-copy) case, where the copy itself already disambiguates. RECOMMENDATION: reuse
 * `CHAT_MESSAGE` / `USER_MESSAGES` for both. This is the same "align with what exists,
 * don't reinvent" instruction applied to the notification layer.
 *
 * ======================================================================================
 * LOCAL (rich) COPY — new keys, mirroring openTradeNotifications' title/body split
 * ======================================================================================
 * Only shown when the app generates the notification itself (foreground service alive,
 * non-relayed). Bypassing FCM/APNs only removes the THIRD-PARTY (Google/Apple) interception
 * risk — it does NOT prevent the OS notification chrome from rendering this content on the
 * lock screen / notification shade, where anyone with physical access to the device sees it.
 * So "full content" here is NOT automatically safe. Production MUST define and document an
 * explicit visibility/redaction policy before relying on this as a privacy boundary — e.g.
 * Android `NotificationCompat.VISIBILITY_PRIVATE` with a redacted public form, and the iOS
 * equivalent (hidden preview / redacted `content` on the lock screen) — mirroring the app's
 * existing privacy posture for trade-message notifications.
 *
 * Private message (#590, fast-follow):
 * ```
 * mobile.communityNotifications.newDirectMessage.title   = New private message
 * mobile.communityNotifications.newDirectMessage.message = {0}: {1}
 * ```
 * {0} = sender display name, {1} = message preview (presenter truncates to ~80 chars
 * with ellipsis — same truncation convention as `ConversationRow`'s 1-line preview in
 * PrivateChatListScreenDesign.kt, for visual/data consistency between the notification
 * and the inbox row the user lands on after tapping it).
 *
 * Discussions channel — MENTION mode (#589, ships this milestone; RECOMMENDED default —
 * see "NOTIFICATION MODE DEFAULT" below):
 * ```
 * mobile.communityNotifications.channelMention.title   = You were mentioned in {0}
 * mobile.communityNotifications.channelMention.message = {0}: {1}
 * ```
 * Title {0} = channel display name. Message {0} = sender name, {1} = message preview
 * (same truncation convention as above).
 *
 * Discussions channel — ALL mode (opt-in per channel, busier):
 * ```
 * mobile.communityNotifications.channelMessage.title   = New message in {0}
 * mobile.communityNotifications.channelMessage.message = {0}: {1}
 * ```
 *
 * ======================================================================================
 * NOTIFICATION MODE DEFAULT — tied to bisq2's own per-channel setting
 * ======================================================================================
 * bisq2 already models this exact choice per channel:
 * `ChatChannelNotificationTypeEnum` (data/replicated/chat/notifications/) —
 * `GLOBAL_DEFAULT | ALL | MENTION | OFF`. RECOMMENDATION: default new Discussions channel
 * memberships to `MENTION`, not `ALL`. Public channels are many-to-many and can be
 * high-traffic (see CommunityHubScreenDesign.kt's "General Discussion" fixture: 1284
 * members) — notifying on every single message would be Discord/Slack's exact
 * well-known anti-pattern for large channels. `ALL` stays available as a per-channel
 * opt-in for smaller/quieter channels a user explicitly wants full coverage on. Both
 * copy variants above exist because both modes are real, reachable states — not because
 * MENTION is the only one shipping.
 *
 * ======================================================================================
 * RELAYED (killed-app / opted-into-relay) COPY — reuses existing keys, no new ones
 * ======================================================================================
 * Uses the EXISTING `mobile.pushNotifications.category.chatMessage` = "New message" for
 * ALL THREE cases above (DM, mention, ALL-mode message) — same generic text already
 * shown for trade-chat relayed notifications today. No new privacy-category work
 * required; this is a direct consequence of the "no new category" decision above.
 *
 * ======================================================================================
 * DEEP LINKING
 * ======================================================================================
 * LOCAL path (rich, this milestone can wire immediately once the routes exist):
 *   `NotificationPressAction.Route(NavRoute.DiscussionsChannel(channelId))` for
 *   mentions/messages; `NotificationPressAction.Route(NavRoute.PrivateChat(channelId))`
 *   for DMs, once #590 ships. Both routes are already flagged as needed in
 *   project_milestone11_community_poc.md agent memory — not yet in NavRoute.kt.
 *
 * RELAYED path: mirrors the EXISTING `tradeId`-in-payload / graceful-fallback pattern
 * exactly (`BisqFirebaseMessagingService.deepLinkRouteFor` / the iOS NSE's
 * `deepLinkUri(tradeId:)`). The trusted node needs to add an equivalent optional
 * `channelId` (Discussions) / `conversationId` (DM) field to the decrypted payload,
 * mirroring how `tradeId` was added for bisq-network/bisq-mobile#1395:
 *   - Field present → tap deep-links straight to that channel/DM
 *     (`bisq://DiscussionsChannel/<id>` / `bisq://PrivateChat/<id>`).
 *   - Field absent (older trusted node) → tap falls back to `NavRoute.CommunityHub`
 *     (the hub root), exactly as an absent `tradeId` today falls back to the open-trades
 *     list rather than failing to navigate at all.
 *
 * ======================================================================================
 * BACKGROUNDED vs. KILLED — which path fires, concretely
 * ======================================================================================
 * This app's existing model (see `OpenTradesNotificationService.setKeepProcessAlive` /
 * `setLocalDeliverySuppressed`) is not literally "backgrounded=local, killed=relay" —
 * it's:
 *   - Backgrounded, process kept alive (default; user has NOT opted into relayed mode,
 *     and OS notification permission is granted) → LOCAL rich path fires, full copy
 *     above, deep-links directly.
 *   - Backgrounded with relayed mode opted in, OR the OS has killed the process (either
 *     because the user opted out of "keep connected" or the OS reclaimed it), OR the app
 *     is fully force-killed → RELAYED generic path fires, existing "New message" text,
 *     deep-links via the trusted-node-supplied id when present.
 * Community notifications slot into this SAME decision point with no new branching
 * logic — `observeChatMessages`-style observers just need to also watch Discussions
 * channel messages (filtered by notification mode) and, once #590 ships, DM messages,
 * calling the same `notify { ... }` builder with the copy above.
 *
 * ======================================================================================
 * DEPENDENCY ON #590
 * ======================================================================================
 * Discussions mention/message notifications (LOCAL + RELAYED, both copy variants) can
 * ship THIS milestone alongside #589 — they only need an observer on Discussions channel
 * messages, which is new work but not blocked on anything. DM notifications are
 * necessarily blocked on #590 (there is no DM feature to notify about yet) — but no
 * notification-INFRASTRUCTURE work is blocked: same category, same channel, same
 * `notify{}` builder, same relayed-payload-field pattern. When #590 ships, wiring its
 * notifications is "add an observer + these two i18n keys", not new plumbing.
 *
 * ======================================================================================
 * WHY THESE ARE MOCK PREVIEWS
 * ======================================================================================
 * `NotificationCompat`/`UNMutableNotificationContent` are OS APIs — Android's status bar
 * and iOS's lock screen render them outside the Compose tree entirely, so there is no
 * real system-notification `@Preview` possible in this codebase (or any Compose app).
 * `MockSystemNotificationCard` below is an ILLUSTRATIVE STAND-IN ONLY, styled loosely
 * like a notification (app icon, title, body, timestamp) so the COPY can be reviewed
 * visually — it does not attempt to replicate real Android/iOS notification chrome
 * pixel-for-pixel, and every preview using it is labelled "MOCK" for that reason.
 *
 * ======================================================================================
 * i18n KEYS NEEDED (new)
 * ======================================================================================
 * mobile.communityNotifications.newDirectMessage.title   → "New private message"
 * mobile.communityNotifications.newDirectMessage.message → "{0}: {1}"
 * mobile.communityNotifications.channelMention.title     → "You were mentioned in {0}"
 * mobile.communityNotifications.channelMention.message   → "{0}: {1}"
 * mobile.communityNotifications.channelMessage.title     → "New message in {0}"
 * mobile.communityNotifications.channelMessage.message   → "{0}: {1}"
 * (Relayed/generic copy reuses the EXISTING mobile.pushNotifications.category.chatMessage
 * — do not duplicate.)
 *
 * ======================================================================================
 * TEXT EXPANSION
 * ======================================================================================
 * "You were mentioned in {0}" in German: "Du wurdest in {0} erwähnt" (~15% longer,
 * restructures around the placeholder — translators need the placeholder position
 * unconstrained, not assumed to stay at the end). System notification title lines are
 * typically OS-truncated regardless (Android ~1 line, iOS ~2 lines) — this is normal and
 * matches how the existing "Trade [{0}]" title already behaves for long trade ids.
 */
package network.bisq.mobile.presentation.design.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// ---------------------------------------------------------------------------
// Mock system-notification card — illustrative only, see file KDoc "WHY THESE
// ARE MOCK PREVIEWS". Not real OS chrome.
// ---------------------------------------------------------------------------

@Composable
private fun MockSystemNotificationCard(
    title: String,
    body: String,
    timeLabel: String = "now",
    appName: String = "Bisq",
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BisqTheme.colors.dark_grey40, shape = RoundedCornerShape(BisqUIConstants.BorderRadius))
                .padding(BisqUIConstants.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(BisqTheme.colors.primary, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            BisqText.SmallMedium(text = "B", color = BisqTheme.colors.white)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BisqText.XSmallMedium(text = appName.uppercase(), color = BisqTheme.colors.mid_grey20)
                BisqText.XSmallMedium(text = timeLabel, color = BisqTheme.colors.mid_grey20)
            }
            BisqText.BaseRegular(text = title, color = BisqTheme.colors.white, singleLine = true)
            BisqText.SmallLight(text = body, color = BisqTheme.colors.light_grey10)
        }
    }
}

@Composable
private fun MockLabel(text: String) {
    BisqText.SmallRegular(text = text, color = BisqTheme.colors.mid_grey20)
}

// ---------------------------------------------------------------------------
// Previews — LOCAL (rich) copy, app backgrounded with process alive
// ---------------------------------------------------------------------------

@ExcludeFromCoverage
@Preview(name = "Notification (MOCK) — LOCAL, new private message")
@Composable
private fun Notification_Local_NewDirectMessagePreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.background(BisqTheme.colors.backgroundColor).padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            MockLabel("MOCK — illustrative only, not real OS chrome. LOCAL path (app backgrounded, process alive):")
            MockSystemNotificationCard(
                title = "New private message",
                body = "SatoshiFan#1234: Sure, let me know when you're ready to proceed.",
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "Notification (MOCK) — LOCAL, channel mention")
@Composable
private fun Notification_Local_ChannelMentionPreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.background(BisqTheme.colors.backgroundColor).padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            MockLabel("MOCK — illustrative only, not real OS chrome. LOCAL path, MENTION mode (recommended default):")
            MockSystemNotificationCard(
                title = "You were mentioned in General Discussion",
                body = "BitcoinBee#5678: @you what do you think about the new fee model?",
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "Notification (MOCK) — LOCAL, channel message (ALL mode)")
@Composable
private fun Notification_Local_ChannelMessagePreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.background(BisqTheme.colors.backgroundColor).padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            MockLabel("MOCK — illustrative only, not real OS chrome. LOCAL path, ALL mode (opt-in, busier channels):")
            MockSystemNotificationCard(
                title = "New message in General Discussion",
                body = "CryptoNomad#9012: Anyone had luck with SEPA transfers over 500 EUR?",
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Preview — RELAYED (generic, privacy-preserving) copy, app killed / relay opted-in
// ---------------------------------------------------------------------------

@ExcludeFromCoverage
@Preview(name = "Notification (MOCK) — RELAYED, generic (app killed, or relay opted-in)")
@Composable
private fun Notification_Relayed_GenericPreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.background(BisqTheme.colors.backgroundColor).padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            MockLabel(
                "MOCK — illustrative only, not real OS chrome. RELAYED path: reuses the EXISTING " +
                    "chat_message category text — same for DM, mention, and channel message. No sender " +
                    "name or preview shown here by design (privacy — see file KDoc).",
            )
            MockSystemNotificationCard(title = "Bisq", body = "New message")
        }
    }
}

/**
 * Preview: LOCAL vs. RELAYED side by side for the SAME underlying event (a DM from
 * SatoshiFan#1234), to make the privacy trade-off legible in one glance — full content
 * when the app itself generates the notification, generic category text when a relay
 * is involved.
 */
@ExcludeFromCoverage
@Preview(name = "Notification (MOCK) — LOCAL vs RELAYED comparison, same event")
@Composable
private fun Notification_LocalVsRelayed_ComparisonPreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.background(BisqTheme.colors.backgroundColor).padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            MockLabel("MOCK — illustrative only. Same event (new DM), two delivery paths:")
            Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)) {
                MockLabel("Backgrounded, process alive (LOCAL, rich):")
                MockSystemNotificationCard(
                    title = "New private message",
                    body = "SatoshiFan#1234: Sure, let me know when you're ready to proceed.",
                )
            }
            BisqGap.V1()
            Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)) {
                MockLabel("Killed / relay opted-in (RELAYED, generic):")
                MockSystemNotificationCard(title = "Bisq", body = "New message")
            }
        }
    }
}
