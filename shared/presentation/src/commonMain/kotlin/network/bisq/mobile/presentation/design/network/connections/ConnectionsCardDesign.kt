/**
 * ConnectionsCardDesign.kt — Design PoC (Issues #1524 / #1525)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 * Nothing here talks to Koin, `NodePeerInfo`, or a bisq2 `ConnectionDto` — all preview
 * data flows through [simulatedPeerConnection] (per-peer) and [simulatedNodeIdentity]
 * (screen-level keyId/nodeTag), both of which take only primitives.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * Redesign of the per-peer connection card used by the Node app's Connections screen
 * (production file: `apps/nodeApp/.../network/presentation/connections/NetworkConnectionsScreen.kt`,
 * `ConnectionCard` composable). Today that card shows only: direction dot, address
 * (tail-ellipsized), established date/time, and a "seed" badge. This PoC brings it up to
 * feature parity with Bisq2 Desktop's connection table while staying scannable in a long
 * mobile list.
 *
 * ======================================================================================
 * DESKTOP REFERENCE FIELD SET
 * ======================================================================================
 * `bisq2/apps/desktop/desktop/.../network/p2p_network/transport/ConnectionListItem.java`
 * exposes per peer: address, direction, creation date + time, RTT (formatted duration,
 * "-" until first measured), sent bytes + message count (live via `Connection.Listener`),
 * received bytes + message count, keyId, and nodeTag. keyId/nodeTag are NOT per-peer data
 * — see "KEY ID / NODE TAG — RELOCATED" below, this was corrected after tracing the bisq2
 * source with the team.
 *
 * Desktop also carries a `peer` field (username lookup via `UserProfileService`, falling
 * back to "Seed"/"Default"). NOT ported here — for P2P transport-layer connections it
 * resolves to the fallback string almost always, which just duplicates the `[SEED]` badge
 * the mobile card already has. Flag to product if there's a use case this misses.
 *
 * ======================================================================================
 * KEY ID / NODE TAG — RELOCATED OUT OF THE PER-PEER CARD
 * ======================================================================================
 * Initial drafts of this design put keyId/nodeTag inside each card's expanded section.
 * That was wrong: in `ConnectionListItem`, `keyId = node.getNetworkId().getKeyId()` and
 * `nodeTag` comes from `identityService.findAnyIdentityByNetworkId(node.getNetworkId())` —
 * both describe the LOCAL node's OWN identity, not anything about the peer. They are
 * IDENTICAL on every row of the table. Harmless as extra desktop table columns; on a
 * mobile per-peer card, repeating the same value on every single card is pure redundancy.
 *
 * Fixed placement: [ConnectionsScreenIdentityHeader] renders keyId + nodeTag ONCE, above
 * the peer list (see "Data sourcing" below for exactly where each app reads it from). The
 * per-peer card's expanded section now holds only Sent/Received.
 *
 * Node-app note: the "My Node" sub-page (`NetworkInfoDesign.kt`, issue #429) already shows
 * this node's keyId in more depth (full value + copy, alongside version/Tor status). This
 * header is a compact one-line echo for context ("these connections belong to THIS
 * identity"), not a duplicate deep-dive — deliberately terse (truncated + copy icon only).
 *
 * ======================================================================================
 * COLLAPSED VS. EXPANDED — WHY RTT STAYS VISIBLE AND THE REST DOESN'T
 * ======================================================================================
 * The collapsed card is deliberately kept at ~today's height (2 rows) so a 40+ peer list
 * scans exactly as fast as it does now. Of the new per-peer fields, only RTT earns a
 * permanent slot:
 *   - RTT answers "is this connection healthy" — the one thing worth acting on while
 *     scanning a long list (e.g. spotting the peer stuck at "–").
 *   - Sent/received bytes are debugging-tier ("is data actually flowing") — useful when
 *     investigating ONE peer, noise across forty.
 * Sent/received live behind a tap-to-expand chevron, reusing the exact accordion
 * affordance `FaqScreenDesign.kt` already uses (`ArrowRightIcon`/`ArrowDownIcon` swap,
 * 12dp) — no new interaction pattern for users to learn.
 *
 * RTT color-coding (green/amber/red) was considered and REJECTED for v1: Tor circuit
 * latency normally runs 300-800ms+, and there's no vetted threshold — a red badge would
 * misread as "broken" when it's just Tor being Tor. RTT renders as plain text.
 *
 * ======================================================================================
 * ADDRESS / KEY ID TRUNCATION + COPY
 * ======================================================================================
 * Onion v3 addresses are random base32 hashes — there's no meaningful prefix to scan by,
 * but the trailing `:port` IS meaningful (varies across a user's own nodes), so plain
 * tail-ellipsis (today's behavior) silently discards it. [truncateMiddle] keeps head +
 * tail, sized to preserve the port — this generalizes the same head+tail approach already
 * used by `OnionAddressRow` in `NetworkInfoDesign.kt`; extract a shared util at
 * implementation time instead of maintaining two copies. keyId (now in
 * [ConnectionsScreenIdentityHeader], not the card) has no port-equivalent tail worth
 * preserving, so [truncateLeading] (prefix + ellipsis) is enough for it.
 *
 * Both address and keyId get an inline [CopyIconButton] (the real atom —
 * `atoms/button/CopyIconButton.kt` — not a placeholder). Tapping the truncated text itself
 * does NOT copy; only the small copy icon does. This avoids an ambiguous gesture since the
 * whole card is also a tap target for expand/collapse. Compose's nested-clickable
 * consumption means the inner `IconButton` swallows its own tap before it reaches the
 * card's `clickable` — verify this on-device during implementation, but it's the same
 * mechanism every other inline icon-button-inside-clickable-row in this codebase relies on.
 *
 * ======================================================================================
 * DATA SOURCING — WHERE EACH FIELD COMES FROM, PER APP (traced against bisq2 branch
 * `network-info-connect`, issue #4878)
 * ======================================================================================
 * No new bisq2 endpoints are needed for either app. Node reads everything locally;
 * Connect needs the EXISTING NETWORK_INFO websocket topic's `ConnectionDto` widened, not a
 * new one.
 *
 * Per-peer fields — same bisq2 source for both apps
 * (`bisq.network.p2p.node.network_load.ConnectionMetrics`, via `connection.getConnectionMetrics()`):
 *   - rttMillis            <- Math.round(connectionMetrics.getAverageRtt())
 *   - sentBytes            <- connectionMetrics.getSentBytes()
 *   - sentMessageCount     <- connectionMetrics.getNumMessagesSent()
 *   - receivedBytes        <- connectionMetrics.getReceivedBytes()
 *   - receivedMessageCount <- connectionMetrics.getNumMessagesReceived()
 *   - establishedAtMillis  <- connectionMetrics.getCreationDate() (already present today)
 *   - isOutbound           <- connection.isOutboundConnection() (already present today)
 *   - address              <- connection.getPeerAddress().getFullAddress() (already present)
 *   - isSeed               <- peerGroupService.isSeed(peerAddress) (already present today)
 *
 * Node app (`apps/nodeApp/.../NodeNetworkServiceFacade.kt`): all of the above is available
 * IN-PROCESS off the local bisq2 `Connection` objects the facade already holds. No bisq2
 * API change, no new endpoint — just read the extra fields off `getConnectionMetrics()`
 * when building `NodePeerInfo`.
 *
 * Connect app (via trusted node): widen the EXISTING `api/src/main/java/bisq/api/dto/network/ConnectionDto.java`
 * (currently `connectionId, address, outbound, seed, establishedAtMillis`) with the five
 * fields above, and populate them in `NetworkInfoWebSocketService.toConnectionDtos()` —
 * it already iterates the node's `Connection` objects, so `getConnectionMetrics()` is
 * right there. This is a bisq2-side change (#4878 or a follow-up) + jar bump, but it rides
 * the existing NETWORK_INFO topic — no new endpoint. Mirror the widened DTO in the mobile
 * client's `NetworkInfoDto`/`ConnectionDto` and map to `NodePeerInfo`.
 *
 * Node-level identity fields (shown once, in [ConnectionsScreenIdentityHeader], not per-peer):
 *   - keyId    <- Node: `node.getNetworkId().getKeyId()` (own identity). Connect: the
 *                 trusted node's keyId — ALREADY present as `NetworkInfoDto.keyId` at the
 *                 top level, per issue #429's Overview screen work. No new field.
 *   - nodeTag  <- Node: `identityService.findAnyIdentityByNetworkId(...)`. Connect: NEW —
 *                 `NetworkInfoDto` has no `nodeTag` field yet; needs adding once (not
 *                 per-peer) alongside the #4878 DTO work.
 * Optional/future: Desktop also resolves a peer USERNAME per row via `UserProfileService` —
 * available later if there's a use case, not required for this design.
 *
 * ======================================================================================
 * I18N KEYS NEEDED
 * ======================================================================================
 * This PoC hardcodes literal English strings (project convention for design PoCs — see
 * `TradePaymentAccountSelectionDesign.kt`). Production wiring needs, added alongside the
 * existing `mobile.networkInfo.connections.*` block
 * (`shared/domain/src/commonMain/resources/mobile/mobile.properties`, ~line 330):
 *
 *   mobile.networkInfo.connections.rtt=RTT
 *   mobile.networkInfo.connections.sent=Sent
 *   mobile.networkInfo.connections.received=Received
 *   mobile.networkInfo.connections.identity.keyId=Key ID
 *   mobile.networkInfo.connections.identity.nodeTag=Node tag
 *   mobile.networkInfo.connections.ioData.1={0} · {1} message
 *   mobile.networkInfo.connections.ioData.*={0} · {1} messages
 *   mobile.networkInfo.connections.expand=Show connection details
 *   mobile.networkInfo.connections.collapse=Hide connection details
 *
 * Expansion check: "SENT"/"RECEIVED" are short in English but run ~30-40% longer in
 * German/Russian inside the 2-column expanded layout. They're non-critical secondary
 * labels — let them wrap to 2 lines rather than truncating. Same applies to the "Key ID"/
 * "Node tag" labels in [ConnectionsScreenIdentityHeader].
 *
 * ======================================================================================
 * HANDOFF — WHAT THE DEVELOPER NEEDS TO WIRE
 * ======================================================================================
 * (a) Data-field contract — see "Data sourcing" above for exact bisq2 origins.
 *     `NodePeerInfo` (`apps/nodeApp/.../common/domain/service/network/NodePeerInfo.kt`)
 *     currently has `connectionId, address, isOutbound, establishedAtMillis, isSeed`. Add
 *     PER-PEER: `rttMillis: Long?` (null until first measured — maps to this PoC's `-1L`
 *     sentinel), `sentBytes: Long`, `sentMessageCount: Int`, `receivedBytes: Long`,
 *     `receivedMessageCount: Int`. The bisq2 `ConnectionDto` (Connect app) needs the same
 *     per-peer shape so ONE card design serves both apps, per the issue requirement.
 *     keyId/nodeTag are NOT part of this per-peer contract — see (a2).
 *
 * (a2) Node-level identity contract (separate from NodePeerInfo). Node: read
 *     `node.getNetworkId().getKeyId()` / the identity tag directly in the Node app's
 *     Network Info presenter, same as the existing "My Node" sub-page already does.
 *     Connect: `keyId` is already on `NetworkInfoDto` (issue #429) — just thread it into
 *     this screen too. `nodeTag` is NOT yet on `NetworkInfoDto` — add it once, at the
 *     top level, alongside the #4878 DTO widening (NOT on the per-peer DTO).
 *
 * (b) `ByteUnitUtil.formatBytesPrecise`
 *     (`shared/domain/src/androidMain/.../utils/ByteUnitUtil.kt`) is androidMain-only
 *     despite being pure Kotlin math (no platform dependency) — move it to commonMain
 *     before wiring this card, otherwise iOS/Connect can't format sent/received bytes.
 *     [formatBytes] below duplicates its logic ONLY because of this gap — delete the local
 *     copy once the move happens.
 *
 * (c) Live updates. `NetworkConnectionsPresenter.onViewAttached()` currently only
 *     re-emits `connectedPeers` on peer set changes (connect/disconnect), not on metric
 *     changes. Desktop updates sent/received/RTT per network message via
 *     `Connection.Listener` — too chatty for a mobile `LazyColumn`. Recommended: a
 *     periodic re-snapshot (3-5s tick) that re-emits the whole peers list with fresh
 *     metrics, not a per-message push.
 *
 * (d) Connect app scope. The "My Connection" screen was cut — this Connections list is
 *     the WHOLE per-peer story for Connect. Same card design serves both Node (local data)
 *     and Connect (data arrives via the expanded `ConnectionDto` from the trusted node) —
 *     no per-app card variant needed, unlike the Overview screen in
 *     `NetworkInfoConnectDesign.kt` which does split Node/Connect.
 */
package network.bisq.mobile.presentation.design.network.connections

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.no_connections
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.button.CopyIconButton
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ArrowDownIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ArrowRightIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import org.jetbrains.compose.resources.painterResource
import kotlin.math.round

// -------------------------------------------------------------------------------------
// Simulated domain type (primitives only — no presenter/domain dependency)
// -------------------------------------------------------------------------------------

/**
 * Preview-only stand-in for `NodePeerInfo` (Node) / the future `ConnectionDto` (Connect).
 * See the HANDOFF section above for the production field mapping. Deliberately has NO
 * keyId/nodeTag — those are node-level, not per-peer, see "KEY ID / NODE TAG — RELOCATED"
 * above and [SimulatedNodeIdentity] below.
 *
 * `rttMillis = -1L` is the "not yet measured" sentinel, rendered as "–" by [formatRtt].
 * Production maps this to `rttMillis: Long?` with `null` in that role.
 */
internal data class SimulatedPeerConnection(
    val connectionId: String,
    val address: String,
    val isOutbound: Boolean,
    val establishedAt: String,
    val isSeed: Boolean,
    val rttMillis: Long,
    val sentBytes: Long,
    val sentMessageCount: Int,
    val receivedBytes: Long,
    val receivedMessageCount: Int,
)

/**
 * Builds a [SimulatedPeerConnection] from primitives with realistic defaults, so preview
 * call-sites only need to specify the field(s) relevant to the state being demonstrated.
 */
internal fun simulatedPeerConnection(
    connectionId: String,
    address: String,
    isOutbound: Boolean = true,
    establishedAt: String = "Jul 20, 2026 · 14:32:07",
    isSeed: Boolean = false,
    rttMillis: Long = 184L,
    sentBytes: Long = 12_400L,
    sentMessageCount: Int = 340,
    receivedBytes: Long = 18_900L,
    receivedMessageCount: Int = 512,
): SimulatedPeerConnection =
    SimulatedPeerConnection(
        connectionId = connectionId,
        address = address,
        isOutbound = isOutbound,
        establishedAt = establishedAt,
        isSeed = isSeed,
        rttMillis = rttMillis,
        sentBytes = sentBytes,
        sentMessageCount = sentMessageCount,
        receivedBytes = receivedBytes,
        receivedMessageCount = receivedMessageCount,
    )

/**
 * Preview-only stand-in for the screen-level node identity (own identity for Node, the
 * trusted node's identity for Connect). See "Data sourcing" above — Connect's `keyId`
 * already exists on `NetworkInfoDto`; `nodeTag` does not yet and needs adding.
 */
internal data class SimulatedNodeIdentity(
    val keyId: String,
    val nodeTag: String,
)

/** Builds a [SimulatedNodeIdentity] from primitives with a realistic default. */
internal fun simulatedNodeIdentity(
    keyId: String,
    nodeTag: String = "default",
): SimulatedNodeIdentity = SimulatedNodeIdentity(keyId = keyId, nodeTag = nodeTag)

// -------------------------------------------------------------------------------------
// Formatting helpers
// -------------------------------------------------------------------------------------

private const val ADDRESS_HEAD_CHARS = 10
private const val ADDRESS_TAIL_CHARS = 10
private const val KEY_ID_KEEP_CHARS = 16

/**
 * Head + "…" + tail truncation. Used for the address so the trailing `:port` (the one
 * part of an onion address that varies meaningfully across a user's own nodes) survives
 * truncation, unlike plain [TextOverflow.Ellipsis].
 */
private fun truncateMiddle(
    value: String,
    headChars: Int,
    tailChars: Int,
): String {
    if (value.length <= headChars + tailChars + 1) return value
    return "${value.take(headChars)}…${value.takeLast(tailChars)}"
}

/** Leading truncation for keyId — no tail worth preserving, unlike the address's port. */
private fun truncateLeading(
    value: String,
    keepChars: Int,
): String {
    if (value.length <= keepChars + 1) return value
    return "${value.take(keepChars)}…"
}

/** "184 ms" / "1.2 s" / "–" when unmeasured (`rttMillis < 0`). No color-coding — see KDoc. */
private fun formatRtt(rttMillis: Long): String =
    when {
        rttMillis < 0 -> "–"
        rttMillis < 1000 -> "$rttMillis ms"
        else -> "${round(rttMillis / 100.0) / 10.0} s"
    }

/**
 * Mirrors `ByteUnitUtil.formatBytesPrecise` — duplicated locally only because that util is
 * currently androidMain-only (see HANDOFF item b). Delete this once it moves to commonMain.
 */
private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = -1
    while (value >= 1024 && unitIndex < units.size - 1) {
        value /= 1024
        unitIndex++
    }
    val rounded = round(value * 10) / 10.0
    return "$rounded ${units[unitIndex]}"
}

/** "12.4 KB · 340 msgs" — see i18n key `mobile.networkInfo.connections.ioData` above. */
private fun formatIoLine(
    bytes: Long,
    messageCount: Int,
): String {
    val unit = if (messageCount == 1) "msg" else "msgs"
    return "${formatBytes(bytes)} · $messageCount $unit"
}

// -------------------------------------------------------------------------------------
// List header + empty state
// -------------------------------------------------------------------------------------

/** Unchanged from production — the existing "N peers" summary line already works well. */
@Composable
internal fun ConnectionsListHeader(
    peerCount: Int,
    modifier: Modifier = Modifier,
) {
    BisqText.SmallRegular(
        text = if (peerCount == 1) "1 peer" else "$peerCount peers",
        color = BisqTheme.colors.mid_grey20,
        modifier = modifier,
    )
}

/**
 * NEW — screen-level identity strip, rendered ONCE above [ConnectionsListHeader]/the peer
 * list. Shows the keyId/nodeTag that used to be (incorrectly) proposed per-card — see
 * "KEY ID / NODE TAG — RELOCATED" in the file KDoc for why this moved here.
 *
 * Deliberately terse: truncated keyId + copy icon + nodeTag on one line. Node's "My Node"
 * sub-page already covers keyId in depth; this is a lightweight contextual echo, not a
 * duplicate of that screen.
 */
@Composable
internal fun ConnectionsScreenIdentityHeader(
    identity: SimulatedNodeIdentity,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BisqText.XSmallLight(text = "Key ID", color = BisqTheme.colors.mid_grey20)
        BisqGap.HHalf()
        BisqText.StyledText(
            text = truncateLeading(identity.keyId, KEY_ID_KEEP_CHARS),
            style = BisqTheme.typography.xsmallMedium,
            color = BisqTheme.colors.white,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        CopyIconButton(value = identity.keyId, showToast = false)
        BisqGap.H1()
        BisqText.XSmallLight(text = "Node tag", color = BisqTheme.colors.mid_grey20)
        BisqGap.HHalf()
        BisqText.XSmallMedium(text = identity.nodeTag, color = BisqTheme.colors.white)
    }
}

/** Unchanged from production — replicated here for preview completeness. */
@Composable
internal fun ConnectionsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = BisqUIConstants.ScreenPadding2X),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(Res.drawable.no_connections),
            contentDescription = null,
            modifier = Modifier.size(BisqUIConstants.ScreenPadding4X),
        )
        BisqGap.V2()
        BisqText.BaseMedium(
            text = "No peers connected",
            color = BisqTheme.colors.mid_grey30,
            textAlign = TextAlign.Center,
        )
        BisqGap.V1()
        BisqText.SmallLight(
            text = "The node will connect to peers automatically once Tor is established.",
            color = BisqTheme.colors.mid_grey20,
            textAlign = TextAlign.Center,
        )
    }
}

// -------------------------------------------------------------------------------------
// Connection card
// -------------------------------------------------------------------------------------

/**
 * Redesigned per-peer card. Collapsed by default; tapping anywhere on the card (except
 * the copy icons) toggles the expanded section. [initiallyExpanded] exists purely so
 * previews can render the expanded state without simulating a tap.
 *
 * Layout — see the KDoc header for the full rationale:
 *   Row 1 (always visible): direction dot · address (truncated, copy icon) · RTT
 *   Row 2 (always visible): established date/time · seed badge · direction label · chevron
 *   Expanded section: divider, then Sent/Received (2-column). keyId/nodeTag are NOT here —
 *   see [ConnectionsScreenIdentityHeader], rendered once above the peer list instead.
 */
@Composable
internal fun ConnectionCard(
    peer: SimulatedPeerConnection,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
) {
    var isExpanded by remember(peer.connectionId) { mutableStateOf(initiallyExpanded) }
    val directionColor = if (peer.isOutbound) BisqTheme.colors.primary else BisqTheme.colors.mid_grey30
    val directionLabel = if (peer.isOutbound) "Outbound" else "Inbound"

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey40)
                .clickable { isExpanded = !isExpanded }
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPadding,
                ).testTag("connection_card_${peer.connectionId}"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(directionColor),
            )
            BisqGap.H1()
            BisqText.StyledText(
                text = truncateMiddle(peer.address, ADDRESS_HEAD_CHARS, ADDRESS_TAIL_CHARS),
                style = BisqTheme.typography.smallMedium,
                color = BisqTheme.colors.white,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            CopyIconButton(value = peer.address, showToast = false)
            BisqGap.H1()
            BisqText.StyledText(
                text = formatRtt(peer.rttMillis),
                style = BisqTheme.typography.xsmallMedium,
                color = if (peer.rttMillis < 0) BisqTheme.colors.mid_grey20 else BisqTheme.colors.white,
                maxLines = 1,
            )
        }

        BisqGap.VQuarter()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BisqText.XSmallLight(
                text = peer.establishedAt,
                color = BisqTheme.colors.mid_grey20,
                modifier = Modifier.weight(1f),
            )
            if (peer.isSeed) {
                SeedBadge()
                BisqGap.HHalf()
            }
            BisqText.XSmallLight(text = directionLabel, color = BisqTheme.colors.mid_grey20)
            BisqGap.HHalf()
            if (isExpanded) {
                ArrowDownIcon(modifier = Modifier.size(12.dp))
            } else {
                ArrowRightIcon(modifier = Modifier.size(12.dp))
            }
        }

        if (isExpanded) {
            ConnectionCardExpandedSection(peer = peer)
        }
    }
}

@Composable
private fun SeedBadge() {
    Box(
        modifier =
            Modifier
                .border(
                    width = 1.dp,
                    color = BisqTheme.colors.mid_grey10,
                    shape = RoundedCornerShape(BisqUIConstants.BorderRadiusSmall),
                ).padding(
                    horizontal = BisqUIConstants.ScreenPaddingHalf,
                    vertical = BisqUIConstants.ScreenPaddingQuarter,
                ),
    ) {
        BisqText.XSmallLight(text = "SEED", color = BisqTheme.colors.mid_grey20)
    }
}

@Composable
private fun ConnectionCardExpandedSection(peer: SimulatedPeerConnection) {
    Column {
        BisqHDivider(verticalPadding = BisqUIConstants.ScreenPaddingHalf)

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                BisqText.XSmallLight(text = "SENT", color = BisqTheme.colors.mid_grey20)
                BisqGap.VQuarter()
                BisqText.SmallRegular(
                    text = formatIoLine(peer.sentBytes, peer.sentMessageCount),
                    color = BisqTheme.colors.white,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                BisqText.XSmallLight(text = "RECEIVED", color = BisqTheme.colors.mid_grey20)
                BisqGap.VQuarter()
                BisqText.SmallRegular(
                    text = formatIoLine(peer.receivedBytes, peer.receivedMessageCount),
                    color = BisqTheme.colors.white,
                )
            }
        }
    }
}

// =======================================================================================
// PREVIEWS
// =======================================================================================

@ExcludeFromCoverage
@Preview(name = "1. Collapsed — outbound, non-seed, normal RTT")
@Composable
private fun ConnectionCard_Collapsed_Outbound_Preview() {
    BisqTheme.Preview {
        ConnectionCard(
            peer =
                simulatedPeerConnection(
                    connectionId = "1",
                    address = "wns3jrgvyxjafp7sazk4nzgcpndinftwotgxu4tdpg6ov5xshhbc4qd.onion:1893",
                    isOutbound = true,
                ),
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "2. Collapsed — inbound, seed peer")
@Composable
private fun ConnectionCard_Collapsed_Inbound_Seed_Preview() {
    BisqTheme.Preview {
        ConnectionCard(
            peer =
                simulatedPeerConnection(
                    connectionId = "2",
                    address = "mnop9012qrst3456uvwxyzab5678cdef9012ghij3456klmn.onion:1893",
                    isOutbound = false,
                    isSeed = true,
                    rttMillis = 62L,
                ),
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "3. Expanded — sent/received revealed (no keyId/nodeTag — see preview 8)")
@Composable
private fun ConnectionCard_Expanded_Preview() {
    BisqTheme.Preview {
        ConnectionCard(
            peer =
                simulatedPeerConnection(
                    connectionId = "3",
                    address = "wns3jrgvyxjafp7sazk4nzgcpndinftwotgxu4tdpg6ov5xshhbc4qd.onion:1893",
                    isOutbound = true,
                    isSeed = true,
                ),
            initiallyExpanded = true,
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "4. RTT not yet measured (\"–\")")
@Composable
private fun ConnectionCard_RttUnavailable_Preview() {
    BisqTheme.Preview {
        ConnectionCard(
            peer =
                simulatedPeerConnection(
                    connectionId = "4",
                    address = "abcd1234efgh5678ijkl9012mnop3456qrst7890uvwx.onion:1893",
                    isOutbound = false,
                    rttMillis = -1L,
                    sentBytes = 0L,
                    sentMessageCount = 0,
                    receivedBytes = 0L,
                    receivedMessageCount = 0,
                ),
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "5. Long address truncation, expanded")
@Composable
private fun ConnectionCard_LongAddress_Preview() {
    BisqTheme.Preview {
        ConnectionCard(
            peer =
                simulatedPeerConnection(
                    connectionId = "5",
                    address = "z6irj4nnvzws44obfrkyk5oqxlqjndp2v2q2f6d34ftfsee4kgue3iid.onion:9999",
                    isOutbound = true,
                ),
            initiallyExpanded = true,
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "6. Full screen — identity header + list header + mixed states")
@Composable
private fun ConnectionsList_MixedStates_Preview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            ConnectionsScreenIdentityHeader(
                identity = simulatedNodeIdentity(keyId = "02a1c98f4b7e29d3f18a"),
            )
            BisqHDivider(verticalPadding = BisqUIConstants.ScreenPaddingHalf)
            ConnectionsListHeader(peerCount = 3)
            ConnectionCard(
                peer =
                    simulatedPeerConnection(
                        connectionId = "a",
                        address = "wns3jrgvyxjafp7sazk4nzgcpndinftwotgxu4tdpg6ov5xshhbc4qd.onion:1893",
                        isOutbound = true,
                        isSeed = true,
                        rttMillis = 184L,
                    ),
            )
            ConnectionCard(
                peer =
                    simulatedPeerConnection(
                        connectionId = "b",
                        address = "mnop9012qrst3456uvwxyzab5678cdef9012ghij3456klmn.onion:1893",
                        isOutbound = false,
                        rttMillis = 412L,
                    ),
            )
            ConnectionCard(
                peer =
                    simulatedPeerConnection(
                        connectionId = "c",
                        address = "abcd1234efgh5678ijkl9012mnop3456qrst7890uvwx.onion:1893",
                        isOutbound = true,
                        rttMillis = -1L,
                    ),
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "7. Identity header — long key ID truncation")
@Composable
private fun ConnectionsScreenIdentityHeader_LongKeyId_Preview() {
    BisqTheme.Preview {
        ConnectionsScreenIdentityHeader(
            identity =
                simulatedNodeIdentity(
                    keyId = "9f8e7d6c5b4a3928170615243f3e2d1c0b9a8f7e6d5c4b3a2918f7e6d5c4b3a2",
                    nodeTag = "trading-2",
                ),
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "8. Empty state — no peers connected")
@Composable
private fun ConnectionsEmptyState_Preview() {
    BisqTheme.Preview {
        ConnectionsEmptyState(modifier = Modifier.padding(BisqUIConstants.ScreenPadding2X))
    }
}
