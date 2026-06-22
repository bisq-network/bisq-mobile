/**
 * NetworkInfoDesign.kt — Design PoC (Issue bisq-network/bisq-mobile#429)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * STAKEHOLDER SUMMARY
 * ======================================================================================
 * Issue has been discussed by rodvar (mobile maintainer), jigglypuff-bsq (designer),
 * nostrbuddha (contributor), and HenrikJannsen (bisq2 core) over several months.
 *
 * Key agreements reached:
 *   - Must show: peer count, peer list, user's own onion address (rodvar)
 *   - Connect app needs a "Routed/Bridged through trusted node" framing (rodvar)
 *   - Sub-pages per category preferred over a single cramped screen (jigglypuff + nostrbuddha + rodvar)
 *   - Priority is "nice to have" post-launch, not urgent (HenrikJannsen)
 *
 * ======================================================================================
 * DESIGN DECISION 1 — MORE MENU PLACEMENT (Option A chosen)
 * ======================================================================================
 * Option A: Add a standalone "Network" entry to the current flat More menu now, with a
 * comment marking where it lands when the new grouped menu (#1520) ships.
 *
 * Rationale: Option B (block on #1520) adds dependency risk — Network Info is useful
 * NOW for debugging and is independent of the menu restructure. Option A ships value
 * immediately and the grouped menu POC (MoreMenuDesign.kt) already defines the target
 * cluster. One comment in the presenter is the entire migration cost.
 *
 * Placement in current flat menu: between "Settings" and "Resources" — it is a read-only
 * informational screen, closer in character to Resources than to Settings.
 *
 * Target cluster in the grouped menu (#1520): APP section.
 * See MoreMenuDesign.kt: APP contains Settings and Resources; Network belongs alongside
 * them because it surfaces app-level infrastructure state, not identity or trading setup.
 *
 * Icon: Res.drawable.nav_network — this drawable ALREADY EXISTS in the shared
 * composeResources/drawable/ folder. No new asset needed.
 *
 * ======================================================================================
 * DESIGN DECISION 2 — DEPTH PATTERN (sub-pages validated, with one refinement)
 * ======================================================================================
 * jigglypuff's second proposal and rodvar's "I think this is it!" both point to
 * separate sub-pages per category. This design validates that choice.
 *
 * The overview screen replaces what jigglypuff called "Show All" — it is a scannable
 * summary card list with a chevron-right on each entry that navigates to the detail
 * sub-page. This is:
 *   - Faster to read than a wall of data (HenrikJannsen's observation: this is informational,
 *     not a control panel)
 *   - Exactly the pattern nostrbuddha proposed: "Connections and My Nodes as 2 menu links"
 *   - Already familiar to users via the More menu's own nav pattern
 *
 * The ONE addition beyond jigglypuff's second proposal: an "Overview" screen that sits
 * between the More menu entry and the two sub-pages. This avoids landing the user
 * directly in a raw peer list — the overview gives context (health badge, onion address
 * peek) before they drill in. For a "nice to have" informational screen, an overview
 * that communicates health at a glance is more valuable than jumping straight into data.
 *
 * Navigation tree:
 *   More → Network Info (Overview)
 *                ├── Connections  (sub-page)
 *                └── My Node      (sub-page, Node only)
 *                └── My Connection (sub-page, Connect only — describes the WS link)
 *
 * ======================================================================================
 * DESIGN DECISION 3 — CONNECT "BRIDGED THROUGH" FRAMING
 * ======================================================================================
 * Connect users are NOT on the P2P network directly. The framing must be crystal clear.
 *
 * Solution: the Connect Overview screen leads with a prominent "bridge" card that names
 * the trusted node and its reachability, then shows a muted secondary section labelled
 * "Via your node" for the peer count the trusted node sees. This visual
 * hierarchy — trusted-node health first, network second — prevents users from thinking
 * they are directly connected to peers.
 *
 * The sub-page for the WebSocket connection is called "My Connection" (not "My Node")
 * because the user has no node. It shows: WS endpoint (truncated host), latency, Tor
 * status, and last-seen timestamp.
 *
 * ======================================================================================
 * ANSWER TO nostrbuddha's UNANSWERED QUESTION
 * ======================================================================================
 * nostrbuddha asked: "What does the external-link icon next to the Connections header do?"
 *
 * Decision: the external-link icon is REMOVED from this design. In jigglypuff's first
 * mockup it appeared next to the "Connections" section header, implying an export or
 * deep-link action. Neither semantic makes sense on mobile:
 *   - Export: the data is low-value for end users (it matters for devs, not traders).
 *     HenrikJannsen confirmed this is informational, not a diagnostic dashboard.
 *   - Deep link to a URL: there is no external URL for a peer's onion address.
 * The detail sub-page itself provides enough context. Adding an ambiguous icon that
 * nobody could answer the purpose of is a UX anti-pattern. If a future dev want to add
 * copy-all-peers or share functionality, it belongs as a top-app-bar action on the
 * Connections sub-page, not as a mystery icon on a section header.
 *
 * ======================================================================================
 * WHAT FROM DESKTOP WAS DELIBERATELY NOT BROUGHT TO MOBILE
 * ======================================================================================
 * 1. Traffic stats (sent/received MB, serialize time, message class breakdown):
 *    This is developer telemetry. HenrikJannsen called it lower priority post-launch.
 *    For a mobile screen that most users will open once, a breakdown by Java class name
 *    adds noise without value. Omitted entirely.
 *
 * 2. System load / thread statistics (NumThreadStatistics, SystemLoad):
 *    Even on desktop these are commented out as "only relevant for devs". Not included.
 *
 * 3. Version distribution progress rings:
 *    Desktop shows circular progress rings per version. On mobile this is decorative
 *    for most users. The local app version is shown as a plain text label on the
 *    My Node sub-page; version distribution across peers is omitted (low-value for
 *    end users, high implementation cost, no API today).
 *
 * 4. I2P transport:
 *    Mobile Node uses Tor only. I2P is desktop-only. Not shown.
 *
 * 5. Per-connection RTT, key ID columns, node tag:
 *    The desktop connections table has 8 columns. Even in a vertically-scrolling card
 *    list, displaying all 8 fields per connection card is overwhelming on a 360dp
 *    phone screen. Each connection card shows: truncated peer address, direction
 *    indicator, connected-since duration. RTT is shown only in aggregate (or omitted)
 *    because individual RTT matters for debugging, not for a trader checking
 *    "am I connected?". Key ID and node tag are dev-level data; omitted.
 *
 * ======================================================================================
 * ISSUE SPLIT (#429 → A + B)
 * ======================================================================================
 * - Issue A (ship now): Overview screen + free data only. Sub-pages may exist as
 *   composables but remain non-navigable until Issue B lands. The syncing intermediate
 *   state (Preview 4c) is explicitly part of Issue A scope.
 * - Issue B (depends on A + bisq2 API extensions): per-peer lists, full onion address
 *   reveal, Connect peer-count-via-node. None of these block the overview landing.
 *
 * ======================================================================================
 * DATA AVAILABILITY — "FREE" vs "NEEDS NEW WIRING"
 * ======================================================================================
 * Already exposed by NetworkServiceFacade (shared/domain) — Issue A free data:
 *   - numConnections: StateFlow<Int>          ← drives the peer count badge
 *   - allDataReceived: StateFlow<Boolean>     ← drives the "Synced" health label
 *   - isTorEnabled(): suspend fun Boolean     ← drives the transport type label
 *   - KmpTorService.state                     ← drives Tor status display
 *
 * Needs new wiring (new API calls / new facade methods) — Issue B scope:
 *   - Per-peer list (address, direction, connected-since)  — Node only, needs bisq2 API
 *   - User's own onion address                             — Node only, needs bisq2 API
 *   - App version string                                   — available via BuildConfig
 *   - WebSocket endpoint host (Connect)                    — available from pairing config
 *   - Trusted node name/alias (Connect)                    — available from pairing config
 *   - WS connection latency (Connect)                      — available from ClientConnectivityService
 *   - Last-seen timestamp (Connect)                        — available from WebSocketClientImpl
 *   - Connect peer-count-via-node                          — needs new WS endpoint or REST
 *
 * The overview screen can be shipped with ONLY the "free" data (numConnections + Tor
 * status) as a first iteration, then the sub-pages filled in as the API is extended.
 *
 * ======================================================================================
 * PER-PEER ROLLING COUNTERS — FUTURE ENHANCEMENT NOTE
 * ======================================================================================
 * Per-peer rolling counters (messages sent, KB sent, KB received) are a planned future
 * enhancement, not an oversight. The intended UX is: tap a ConnectionCard to expand it,
 * revealing the counters in a secondary section below the address/direction/since row.
 *
 * These are distinct from the desktop's per-message-class breakdown (DataRequest,
 * AuthorizeAddressRequest, etc.). That breakdown is developer telemetry — it was
 * intentionally excluded from mobile (see "WHAT FROM DESKTOP WAS DELIBERATELY NOT
 * BROUGHT TO MOBILE" above). The rolling totals per peer are useful to end users
 * ("is this peer sending me data or is it dead?") without requiring knowledge of the
 * message type taxonomy.
 *
 * SimulatedPeer already reserves nullable fields for forward compatibility. Adding
 * them to the data class now avoids needing to touch call sites when the expansion
 * lands in Issue B or a follow-up.
 *
 * ======================================================================================
 * I18N KEYS NEEDED
 * ======================================================================================
 *   mobile.networkInfo.title                   = "Network Info"
 *   mobile.networkInfo.overview.health.healthy  = "Healthy"
 *   mobile.networkInfo.overview.health.syncing  = "Syncing"
 *   mobile.networkInfo.overview.health.offline  = "Offline"
 *   mobile.networkInfo.overview.connections     = "{0} peers connected"
 *   mobile.networkInfo.overview.transport       = "Transport"
 *   mobile.networkInfo.overview.tor             = "Tor"
 *   mobile.networkInfo.overview.clearnet        = "Clearnet"
 *   mobile.networkInfo.overview.myAddress       = "My onion address"
 *   mobile.networkInfo.overview.addressHidden   = "Hidden — see My Node"
 *   mobile.networkInfo.connections.title        = "Connections"
 *   mobile.networkInfo.connections.empty        = "No peers connected"
 *   mobile.networkInfo.connections.emptyHint    = "The node will connect to peers automatically once Tor is established."
 *   mobile.networkInfo.connections.inbound      = "Inbound"
 *   mobile.networkInfo.connections.outbound     = "Outbound"
 *   mobile.networkInfo.connections.since        = "Since {0}"
 *   mobile.networkInfo.connections.seed         = "Seed node"
 *   mobile.networkInfo.myNode.title             = "My Node"
 *   mobile.networkInfo.myNode.onionAddress      = "Onion address"
 *   mobile.networkInfo.myNode.keyId             = "Key ID"
 *   mobile.networkInfo.myNode.appVersion        = "App version"
 *   mobile.networkInfo.myNode.torStatus         = "Tor status"
 *   mobile.networkInfo.myNode.torStarted        = "Running"
 *   mobile.networkInfo.myNode.torStopped        = "Stopped"
 *   mobile.networkInfo.connect.bridgedTitle     = "Trusted Node"
 *   mobile.networkInfo.connect.reachable        = "Reachable"
 *   mobile.networkInfo.connect.unreachable      = "Unreachable"
 *   mobile.networkInfo.connect.viaNetwork       = "Via your node"
 *   mobile.networkInfo.connect.myConn.title     = "My Connection"
 *   mobile.networkInfo.connect.myConn.endpoint  = "Endpoint"
 *   mobile.networkInfo.connect.myConn.latency   = "Latency"
 *   mobile.networkInfo.connect.myConn.lastSeen  = "Last response"
 *   mobile.networkInfo.connect.myConn.torStatus = "Tor routing"
 *   mobile.more.network                         = "Network"
 *   mobile.more.networkInfo                     = "Network Info"
 *
 * ======================================================================================
 * FILE SPLIT
 * ======================================================================================
 * This file: shared overview composables, Node-app screens (Previews 1–4), More menu
 *            placement (Preview 8).
 * NetworkInfoConnectDesign.kt: Connect-app screens (Previews 5–7).
 *
 * ======================================================================================
 * PREVIEWS IN THIS FILE
 * ======================================================================================
 * Preview 1:  Node — Overview (connected, healthy)              DATA: free (Issue A)
 * Preview 2:  Node — Connections sub-page (populated list)      DATA: requires new bisq2 API (Issue B)
 * Preview 3:  Node — My Node sub-page (full identity details)   DATA: requires new bisq2 API (Issue B)
 * Preview 4:  Node — Overview (disconnected / zero peers)       DATA: free (Issue A)
 * Preview 4b: Node — Connections sub-page (empty / offline)     DATA: free (Issue A)
 * Preview 4c: Node — Overview (syncing / cold-launch state)     DATA: free (Issue A)
 * Preview 8:  More menu — Network entry placement (flat list)   DATA: no data
 */
package network.bisq.mobile.presentation.design.network_info

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.nav_network
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ArrowRightIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.common.ui.components.layout.BisqStaticLayout
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import org.jetbrains.compose.resources.painterResource

// ======================================================================================
// Simulated data — primitives only, no domain imports
// ======================================================================================

/**
 * Connection direction in the peer list.
 */
enum class SimulatedDirection { INBOUND, OUTBOUND }

/**
 * One connected peer for preview purposes.
 *
 * [addressTruncated] — first ~12 chars + "..." of the full onion address.
 * [direction]        — whether this connection was initiated by us or the peer.
 * [connectedSince]   — human-readable duration string ("3 min", "1 h 2 min").
 * [isSeed]           — true when the peer is a known seed node.
 *
 * Future enhancement (Issue B): per-peer rolling counters are planned as an
 * expandable section within ConnectionCard (tap to expand, reveals totals below
 * the address row). These differ from the desktop's per-message-class breakdown,
 * which is developer telemetry intentionally excluded from mobile. The nullable
 * fields below are reserved for that expansion so call sites need no changes when
 * it lands.
 *
 * [messagesSent] — total P2P messages sent to this peer since connection (nullable: not yet wired).
 * [kbSent]       — total KB sent to this peer (nullable: not yet wired).
 * [kbReceived]   — total KB received from this peer (nullable: not yet wired).
 */
data class SimulatedPeer(
    val addressTruncated: String,
    val direction: SimulatedDirection,
    val connectedSince: String,
    val isSeed: Boolean = false,
    val messagesSent: Int? = null,
    val kbSent: Float? = null,
    val kbReceived: Float? = null,
)

/**
 * Summary data for the Network Info Overview screen — Node app variant.
 *
 * [peerCount]        — numConnections StateFlow value.
 * [isDataSynced]     — allDataReceived StateFlow value.
 * [isTorEnabled]     — from isTorEnabled() in NetworkServiceFacade.
 * [isTorRunning]     — from KmpTorService.state.
 * [onionAddressPeek] — first 16 chars of the full onion address, or null if not yet known.
 */
data class SimulatedNodeOverview(
    val peerCount: Int,
    val isDataSynced: Boolean,
    val isTorEnabled: Boolean,
    val isTorRunning: Boolean,
    val onionAddressPeek: String?,
)

/**
 * Full identity data for the My Node sub-page — Node app only.
 *
 * [fullOnionAddress] — complete .onion address string (long, needs copy affordance).
 * [keyId]            — short hex key identifier.
 * [appVersion]       — from BuildConfig / ApplicationVersion.
 * [torStatus]        — human-readable Tor status string.
 */
data class SimulatedMyNode(
    val fullOnionAddress: String,
    val keyId: String,
    val appVersion: String,
    val torStatus: String,
)

// ======================================================================================
// Shared component: section label (same style as MoreMenuDesign section headers)
// ======================================================================================

@Composable
internal fun NetworkSectionLabel(text: String) {
    BisqText.XSmallMedium(
        text = text.uppercase(),
        color = BisqTheme.colors.mid_grey20,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = BisqUIConstants.ScreenPadding,
                    top = BisqUIConstants.ScreenPadding,
                    bottom = BisqUIConstants.ScreenPaddingHalf,
                ),
    )
}

// ======================================================================================
// Shared component: labeled info row (label + value, two-column layout)
// ======================================================================================

/**
 * A single label–value row used on the My Node and My Connection sub-pages.
 *
 * [label]         — small muted descriptor, e.g. "Transport".
 * [value]         — the actual data, rendered in white at base size.
 * [valueMaxLines] — truncate the value after this many lines (default 1). Use 2+
 *                   for the full onion address field where we want natural wrapping
 *                   before the copy button.
 * [trailing]      — optional composable slot to the right of the value (e.g. copy icon).
 */
@Composable
internal fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueMaxLines: Int = 1,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPaddingHalf,
                ),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BisqText.SmallRegular(
            text = label,
            color = BisqTheme.colors.mid_grey30,
            modifier = Modifier.weight(0.38f),
        )
        BisqGap.H1()
        Row(
            modifier = Modifier.weight(0.62f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Text(
                text = value,
                style = BisqTheme.typography.smallRegular,
                color = BisqTheme.colors.white,
                modifier = Modifier.weight(1f),
                maxLines = valueMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            trailing?.let {
                BisqGap.H1()
                it()
            }
        }
    }
}

// ======================================================================================
// Shared component: health status badge
// ======================================================================================

/**
 * Small pill badge communicating overall network health.
 *
 * Three states:
 *   HEALTHY  — green background, "Healthy"
 *   SYNCING  — warning amber, "Syncing"
 *   OFFLINE  — danger red, "Offline"
 *
 * Sized to be thumb-friendly as a tappable affordance (min 48dp tall row) even though
 * the badge itself is compact — the enclosing row always meets the 48dp minimum.
 */
enum class SimulatedHealthState { HEALTHY, SYNCING, OFFLINE }

@Composable
internal fun HealthBadge(state: SimulatedHealthState) {
    val (bgColor, label) =
        when (state) {
            SimulatedHealthState.HEALTHY -> Pair(BisqTheme.colors.primary, "Healthy")
            SimulatedHealthState.SYNCING -> Pair(BisqTheme.colors.warning, "Syncing")
            SimulatedHealthState.OFFLINE -> Pair(BisqTheme.colors.danger, "Offline")
        }
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(bgColor)
                .padding(horizontal = BisqUIConstants.ScreenPadding, vertical = BisqUIConstants.ScreenPaddingHalf),
        contentAlignment = Alignment.Center,
    ) {
        BisqText.XSmallMedium(text = label, color = BisqTheme.colors.dark_grey20)
    }
}

// ======================================================================================
// Shared component: navigation entry card (used on Overview to deep-link to sub-pages)
// ======================================================================================

/**
 * A full-width tappable card that represents a sub-page entry on the Overview screen.
 * Matches the visual style of BisqButton with dark_grey40 background and ArrowRightIcon,
 * but the content area is a composable slot rather than a single text string.
 */
@Composable
internal fun SubPageEntryCard(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey40)
                .clickable { onClick() }
                .then(
                    Modifier.padding(
                        horizontal = BisqUIConstants.ScreenPadding,
                        vertical = BisqUIConstants.ScreenPadding,
                    ),
                ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
            BisqGap.H1()
            ArrowRightIcon()
        }
    }
}

// ======================================================================================
// Shared component: connection card (one row per peer in the Connections sub-page)
// ======================================================================================

/**
 * One peer card in the Connections list.
 *
 * Visual layout (single full-width card, dark_grey40 background):
 *
 *   [direction dot] [truncated address]          [seed badge?]
 *                   [inbound/outbound label]      [since X]
 *
 * The direction dot is a small filled circle: green = outbound, white = inbound.
 * This is a colour + semantic distinction (not colour-only) because the text label
 * "Inbound" / "Outbound" is also shown — meets WCAG accessibility requirements.
 *
 * Seed nodes get a small "Seed" badge (grey outline pill) so power users can
 * distinguish them. Most users will never need to know what a seed node is.
 */
@Composable
internal fun ConnectionCard(peer: SimulatedPeer) {
    val directionColor =
        when (peer.direction) {
            SimulatedDirection.OUTBOUND -> BisqTheme.colors.primary
            SimulatedDirection.INBOUND -> BisqTheme.colors.mid_grey30
        }
    val directionLabel =
        when (peer.direction) {
            SimulatedDirection.OUTBOUND -> "Outbound"
            SimulatedDirection.INBOUND -> "Inbound"
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey40)
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        // Direction indicator dot
        Box(
            modifier =
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(directionColor),
        )

        BisqGap.H1()

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = peer.addressTruncated,
                style = BisqTheme.typography.smallMedium,
                color = BisqTheme.colors.white,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BisqGap.VQuarter()
            BisqText.XSmallLight(
                text = directionLabel,
                color = BisqTheme.colors.mid_grey20,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            if (peer.isSeed) {
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
                    BisqText.XSmallLight(text = "Seed", color = BisqTheme.colors.mid_grey20)
                }
                BisqGap.VQuarter()
            }
            BisqText.XSmallLight(
                text = peer.connectedSince,
                color = BisqTheme.colors.mid_grey20,
            )
        }
    }
}

// ======================================================================================
// NODE OVERVIEW — the entry screen for Node app users
// ======================================================================================

/**
 * Computes [SimulatedHealthState] from [SimulatedNodeOverview] fields.
 *
 * Logic mirrors what the production presenter should derive:
 *   - 0 peers or Tor not running → OFFLINE
 *   - peers > 0 but data not yet synced → SYNCING
 *   - peers > 0 and data synced → HEALTHY
 */
private fun SimulatedNodeOverview.healthState(): SimulatedHealthState =
    when {
        peerCount == 0 || (isTorEnabled && !isTorRunning) -> SimulatedHealthState.OFFLINE
        !isDataSynced -> SimulatedHealthState.SYNCING
        else -> SimulatedHealthState.HEALTHY
    }

/**
 * Node app — Network Info Overview screen.
 *
 * Two sub-page entry cards:
 *   1. "Connections" — shows live peer count as a secondary line.
 *   2. "My Node"     — shows truncated onion address peek as a secondary line.
 *
 * Health badge sits in the screen header row so status is visible instantly.
 *
 * [data]                  — overview data (already derived from StateFlow values).
 * [onConnectionsClick]    — navigate to the Connections sub-page.
 * [onMyNodeClick]         — navigate to the My Node sub-page.
 */
@Composable
fun NodeNetworkOverviewScreen(
    data: SimulatedNodeOverview,
    onConnectionsClick: () -> Unit,
    onMyNodeClick: () -> Unit,
) {
    val health = data.healthState()
    val transportLabel = if (data.isTorEnabled) "Tor" else "Clearnet"

    BisqScrollLayout(
        contentPadding =
            PaddingValues(
                horizontal = BisqUIConstants.ScreenPadding,
                vertical = BisqUIConstants.ScreenPadding,
            ),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        // ── Header row: title + health badge ──────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BisqText.H5Medium(text = "Network Info", color = BisqTheme.colors.white)
            HealthBadge(state = health)
        }

        BisqGap.V2()

        // ── At-a-glance stats row ────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            // Peer count chip
            StatChip(
                label = "Peers",
                value = data.peerCount.toString(),
                valueColor = if (data.peerCount > 0) BisqTheme.colors.primary else BisqTheme.colors.danger,
                modifier = Modifier.weight(1f),
            )
            // Transport chip
            StatChip(
                label = "Transport",
                value = transportLabel,
                valueColor = BisqTheme.colors.white,
                modifier = Modifier.weight(1f),
            )
            // Tor status chip
            StatChip(
                label = "Tor",
                value = if (data.isTorRunning) "Running" else "Stopped",
                valueColor = if (data.isTorRunning) BisqTheme.colors.primary else BisqTheme.colors.danger,
                modifier = Modifier.weight(1f),
            )
        }

        BisqGap.V2()

        // ── Sub-page navigation cards ────────────────────────────────────
        NetworkSectionLabel(text = "Details")
        BisqGap.VHalf()

        SubPageEntryCard(onClick = onConnectionsClick) {
            Column {
                BisqText.BaseRegular(text = "Connections", color = BisqTheme.colors.white)
                BisqGap.VQuarter()
                BisqText.SmallLight(
                    text = "${data.peerCount} peers connected",
                    color = BisqTheme.colors.mid_grey20,
                )
            }
        }

        BisqGap.VHalf()

        SubPageEntryCard(onClick = onMyNodeClick) {
            Column {
                BisqText.BaseRegular(text = "My Node", color = BisqTheme.colors.white)
                BisqGap.VQuarter()
                Text(
                    text = data.onionAddressPeek?.let { "$it..." } ?: "Address loading...",
                    style = BisqTheme.typography.smallLight,
                    color = BisqTheme.colors.mid_grey20,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ======================================================================================
// Shared component: compact stat chip (used on overview stat row)
// ======================================================================================

/**
 * A compact stat chip used in the three-column row on the overview screen.
 * dark_grey40 background, label on top in muted grey, value below in [valueColor].
 */
@Composable
private fun StatChip(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey40)
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPadding,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BisqText.XSmallLight(text = label, color = BisqTheme.colors.mid_grey20)
        BisqGap.VHalf()
        BisqText.BaseMedium(text = value, color = valueColor)
    }
}

// ======================================================================================
// NODE CONNECTIONS SUB-PAGE
// ======================================================================================

/**
 * Node app — Connections sub-page.
 *
 * Shows the full list of connected peers, one card per peer, scrollable.
 * When [peers] is empty, renders a friendly empty state with an explanatory hint —
 * this is the expected state during first launch before Tor bootstrap completes.
 *
 * [peerCount]   — displayed in the screen sub-header for quick orientation.
 * [peers]       — the list of connected peers. May be empty.
 */
@Composable
fun NodeConnectionsScreen(
    peerCount: Int,
    peers: List<SimulatedPeer>,
) {
    if (peers.isEmpty()) {
        NodeConnectionsEmptyState()
        return
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPadding,
                ),
        verticalArrangement = Arrangement.Top,
    ) {
        // Screen heading
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BisqText.H5Medium(text = "Connections", color = BisqTheme.colors.white)
            BisqText.SmallRegular(
                text = "$peerCount peers",
                color = BisqTheme.colors.mid_grey20,
            )
        }

        BisqGap.V2()

        peers.forEach { peer ->
            ConnectionCard(peer = peer)
            BisqGap.VHalf()
        }
    }
}

/**
 * Empty state for the Connections sub-page — shown during bootstrap or when
 * the node has lost all peers.
 *
 * Uses the existing no_connections.png drawable (confirmed present in composeResources).
 * Rationale for a separate composable: the empty state vs populated state differ enough
 * in layout that a single composable with conditional branches would become noisy.
 */
@Composable
fun NodeConnectionsEmptyState() {
    BisqStaticLayout(
        contentPadding = PaddingValues(all = BisqUIConstants.ScreenPadding),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BisqGap.V2()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BisqText.H5Medium(text = "Connections", color = BisqTheme.colors.white)
        }

        BisqGap.V4()

        // Muted icon (using Material Icons as a placeholder;
        // production should use the existing no_connections.png drawable)
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = "No connections",
            tint = BisqTheme.colors.mid_grey10,
            modifier = Modifier.size(48.dp),
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
            modifier = Modifier.padding(horizontal = BisqUIConstants.ScreenPadding2X),
        )
    }
}

// ======================================================================================
// Shared component: onion address row with progressive reveal
// ======================================================================================

/**
 * Onion address row for the My Node sub-page.
 *
 * Default (collapsed) state: shows a truncated peek — first 12 chars + "…" + last 8
 * chars + ".onion" — alongside a "Show full address" tap affordance in muted grey.
 *
 * Tapped (expanded) state: renders the full address with word-wrap enabled and the
 * copy icon active. A second tap collapses back to the peek.
 *
 * The truncation pattern (head + tail) lets users verify the address at a glance
 * (they recognise the beginning and can confirm the end) without needing a scroll
 * to see the full 56-character string.
 *
 * [fullAddress] — complete .onion address string (56 chars + ".onion" suffix).
 */
@Composable
internal fun OnionAddressRow(fullAddress: String) {
    var isExpanded by remember { mutableStateOf(false) }

    // Derive truncated peek: first 12 + "…" + last 8 + ".onion"
    val domainPart = fullAddress.removeSuffix(".onion")
    val peek =
        if (domainPart.length > 20) {
            "${domainPart.take(12)}…${domainPart.takeLast(8)}.onion"
        } else {
            fullAddress
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPaddingHalf,
                ),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BisqText.SmallRegular(
            text = "Onion address",
            color = BisqTheme.colors.mid_grey30,
            modifier = Modifier.weight(0.38f),
        )
        BisqGap.H1()
        Column(modifier = Modifier.weight(0.62f)) {
            if (isExpanded) {
                // Full address + copy icon
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Text(
                        text = fullAddress,
                        style = BisqTheme.typography.smallRegular,
                        color = BisqTheme.colors.white,
                        modifier = Modifier.weight(1f),
                    )
                    BisqGap.H1()
                    // Production: replace with CopyIcon + onClick to ClipboardService
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Copy onion address",
                        tint = BisqTheme.colors.mid_grey20,
                        modifier =
                            Modifier
                                .size(16.dp)
                                .clickable { },
                    )
                }
            } else {
                // Truncated peek
                Text(
                    text = peek,
                    style = BisqTheme.typography.smallRegular,
                    color = BisqTheme.colors.mid_grey20,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BisqGap.VQuarter()
            Text(
                text = if (isExpanded) "Hide address" else "Show full address",
                style = BisqTheme.typography.xsmallLight,
                color = BisqTheme.colors.primary,
                modifier = Modifier.clickable { isExpanded = !isExpanded },
            )
        }
    }
}

// ======================================================================================
// NODE MY NODE SUB-PAGE
// ======================================================================================

/**
 * Node app — My Node sub-page.
 *
 * Surfaces the user's own P2P identity: onion address (truncated with inline copy
 * affordance placeholder), key ID, app version, and Tor status.
 *
 * The onion address is the most privacy-sensitive field:
 *   - Displayed in SmallRegular (not XSmall) to be legible without zooming.
 *   - Truncated to 2 lines max with a visual "..." — a copy button allows the full value.
 *   - No tooltip needed (mobile has no hover); the copy action gives the user the full string.
 *
 * The "Copy" action is represented by a small [CopyIconPlaceholder] composable here.
 * In production this would call into ClipboardService.
 *
 * [data] — full identity data for this node.
 */
@Composable
fun NodeMyNodeScreen(data: SimulatedMyNode) {
    BisqScrollLayout(
        contentPadding =
            PaddingValues(
                horizontal = BisqUIConstants.ScreenPadding,
                vertical = BisqUIConstants.ScreenPadding,
            ),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        BisqText.H5Medium(text = "My Node", color = BisqTheme.colors.white)
        BisqGap.V2()

        // Identity card
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                    .background(BisqTheme.colors.dark_grey40),
        ) {
            NetworkSectionLabel(text = "Identity")
            BisqHDivider()

            // Onion address — progressive reveal (peek → full + copy)
            OnionAddressRow(fullAddress = data.fullOnionAddress)
            BisqHDivider()

            InfoRow(label = "Key ID", value = data.keyId)
            BisqHDivider()

            NetworkSectionLabel(text = "Software")
            BisqHDivider()

            InfoRow(label = "App version", value = data.appVersion)
            BisqHDivider()

            NetworkSectionLabel(text = "Transport")
            BisqHDivider()

            InfoRow(label = "Tor status", value = data.torStatus)
            BisqGap.VHalf()
        }
    }
}

// ======================================================================================
// MORE MENU PLACEMENT — flat list (current production, pre-#1520 grouped menu)
// ======================================================================================

/**
 * Simulated data for the flat More menu, with the Network entry inserted.
 *
 * Placement: between "Settings" and "Resources".
 * Rationale: Network Info is a read-only informational screen, closer in character to
 * the Resources screen (version info, web links) than to Settings (configuration).
 * Inserting it here gives the user a logical "app diagnostics" cluster at the bottom
 * of the flat list.
 *
 * When the grouped menu (#1520) lands, Network moves to the APP section alongside
 * Settings and Resources. The production presenter needs one move: shift the Network
 * MenuItem.Leaf from its flat-list position into the APP section items list.
 * See: MoreMenuDesign.kt, SimulatedMenuSection(headingLabel = "App", ...).
 *
 * Icon: Res.drawable.nav_network — ALREADY EXISTS in shared composeResources/drawable/.
 * No new asset required.
 */
private data class SimulatedMoreMenuItem(
    val label: String,
    val iconSlot: @Composable () -> Unit,
    val isHighlighted: Boolean = false,
)

@Composable
private fun MoreMenuWithNetworkEntry(items: List<SimulatedMoreMenuItem>) {
    BisqStaticLayout(
        contentPadding = PaddingValues(all = BisqUIConstants.Zero),
        verticalArrangement = Arrangement.Top,
    ) {
        items.forEach { item ->
            BisqButton(
                text = item.label,
                onClick = {},
                fullWidth = true,
                backgroundColor = if (item.isHighlighted) BisqTheme.colors.primary2 else BisqTheme.colors.dark_grey40,
                leftIcon = item.iconSlot,
                rightIcon = { ArrowRightIcon() },
                textAlign = TextAlign.Start,
                padding = PaddingValues(all = BisqUIConstants.ScreenPadding),
            )
            BisqGap.VHalf()
        }
    }
}

private val flatMoreMenuItems: List<SimulatedMoreMenuItem>
    get() =
        listOf(
            SimulatedMoreMenuItem(
                label = "Support",
                iconSlot = {
                    Icon(Icons.Filled.SupportAgent, "Support", Modifier.size(20.dp), tint = BisqTheme.colors.light_grey10)
                },
            ),
            SimulatedMoreMenuItem(
                label = "Payment Accounts",
                iconSlot = {
                    Icon(Icons.Filled.CreditCard, "Payment Accounts", Modifier.size(20.dp), tint = BisqTheme.colors.light_grey10)
                },
            ),
            SimulatedMoreMenuItem(
                label = "Reputation",
                iconSlot = {
                    Icon(Icons.Filled.Star, "Reputation", Modifier.size(20.dp), tint = BisqTheme.colors.light_grey10)
                },
            ),
            SimulatedMoreMenuItem(
                label = "User Profile",
                iconSlot = {
                    Icon(Icons.Filled.AccountCircle, "User Profile", Modifier.size(20.dp), tint = BisqTheme.colors.light_grey10)
                },
            ),
            SimulatedMoreMenuItem(
                label = "Settings",
                iconSlot = {
                    Icon(Icons.Filled.Settings, "Settings", Modifier.size(20.dp), tint = BisqTheme.colors.light_grey10)
                },
            ),
            // NEW ENTRY — nav_network drawable already exists in shared/presentation resources
            SimulatedMoreMenuItem(
                label = "Network",
                // In preview, painterResource(Res.drawable.nav_network) renders a
                // white-on-transparent PNG — visible on dark_grey40 background.
                // Production uses nav_network.png directly via painterResource().
                iconSlot = {
                    androidx.compose.foundation.Image(
                        painter = painterResource(Res.drawable.nav_network),
                        contentDescription = "Network",
                        modifier = Modifier.size(20.dp),
                    )
                },
                isHighlighted = true,
            ),
            SimulatedMoreMenuItem(
                label = "Resources",
                iconSlot = {
                    Icon(Icons.Filled.Info, "Resources", Modifier.size(20.dp), tint = BisqTheme.colors.light_grey10)
                },
            ),
        )

// ======================================================================================
// SIMULATED DATA FOR NODE PREVIEWS
// ======================================================================================

private val healthyNodeOverview =
    SimulatedNodeOverview(
        peerCount = 7,
        isDataSynced = true,
        isTorEnabled = true,
        isTorRunning = true,
        onionAddressPeek = "jd4tx3nljykg5z3v",
    )

private val disconnectedNodeOverview =
    SimulatedNodeOverview(
        peerCount = 0,
        isDataSynced = false,
        isTorEnabled = true,
        isTorRunning = false,
        onionAddressPeek = null,
    )

// Syncing state: Tor is up and peers are connected, but allDataReceived is still false.
// This is the normal cold-launch window (typically 30-60 seconds on a healthy connection).
private val syncingNodeOverview =
    SimulatedNodeOverview(
        peerCount = 3,
        isDataSynced = false,
        isTorEnabled = true,
        isTorRunning = true,
        onionAddressPeek = "jd4tx3nljykg5z3v",
    )

private val simulatedPeers =
    listOf(
        SimulatedPeer(
            addressTruncated = "jd4tx3nljykg5z3v...",
            direction = SimulatedDirection.OUTBOUND,
            connectedSince = "2 min",
            isSeed = true,
        ),
        SimulatedPeer(
            addressTruncated = "r7m2xpqowg3bvf8t...",
            direction = SimulatedDirection.INBOUND,
            connectedSince = "14 min",
        ),
        SimulatedPeer(
            addressTruncated = "k9fz6wqnmaecyh2p...",
            direction = SimulatedDirection.OUTBOUND,
            connectedSince = "1 h 3 min",
            isSeed = true,
        ),
        SimulatedPeer(
            addressTruncated = "v5bld8qkzrweyxn3...",
            direction = SimulatedDirection.INBOUND,
            connectedSince = "4 min",
        ),
        SimulatedPeer(
            addressTruncated = "p2ajmtq9xsgfhdo7...",
            direction = SimulatedDirection.OUTBOUND,
            connectedSince = "33 min",
        ),
        SimulatedPeer(
            addressTruncated = "eg6rkvhs8mzonxw4...",
            direction = SimulatedDirection.INBOUND,
            connectedSince = "8 min",
        ),
        SimulatedPeer(
            addressTruncated = "n3cqfpd7yltiuwgb...",
            direction = SimulatedDirection.OUTBOUND,
            connectedSince = "19 min",
        ),
    )

private val simulatedMyNode =
    SimulatedMyNode(
        fullOnionAddress = "jd4tx3nljykg5z3vbqrd6fkwpouneimxsacyt2q7hegh5dolk3n.onion",
        keyId = "a3f8c1e2",
        appVersion = "0.4.2 (build 1c3d8fa)",
        torStatus = "Running",
    )

// ======================================================================================
// PREVIEWS
// ======================================================================================

/**
 * Preview 1: Node app — Overview screen (connected and healthy).
 *
 * DATA: free (Issue A) — numConnections + Tor status + allDataReceived. onionAddressPeek
 * is a graceful-null placeholder shown as "jd4tx3nljykg5z3v..." — no new API needed for
 * the overview peek; full address reveal is Issue B scope (see Preview 3).
 *
 * Validates: health badge GREEN, peer count chip shows "7", Tor chip shows "Running",
 * two sub-page entry cards with correct secondary labels.
 */
@ExcludeFromCoverage
@Preview(name = "1. Node — Overview (healthy, 7 peers)")
@Composable
private fun NodeOverview_Healthy_Preview() {
    BisqTheme.Preview {
        NodeNetworkOverviewScreen(
            data = healthyNodeOverview,
            onConnectionsClick = {},
            onMyNodeClick = {},
        )
    }
}

/**
 * Preview 2: Node app — Connections sub-page (7 peers, mix of inbound/outbound, 2 seeds).
 *
 * DATA: requires new bisq2 API (Issue B scope) — per-peer list (address, direction,
 * connected-since) is not yet exposed by NetworkServiceFacade. This sub-page is
 * non-navigable until the bisq2 API extension lands.
 *
 * Validates: seed badge appears on the first and third cards, direction dots render in
 * green vs grey correctly, connected-since strings are right-aligned.
 */
@ExcludeFromCoverage
@Preview(name = "2. Node — Connections sub-page (7 peers)")
@Composable
private fun NodeConnections_Populated_Preview() {
    BisqTheme.Preview {
        NodeConnectionsScreen(
            peerCount = simulatedPeers.size,
            peers = simulatedPeers,
        )
    }
}

/**
 * Preview 3: Node app — My Node sub-page (full identity).
 *
 * DATA: requires new bisq2 API (Issue B scope) — full onion address and key ID need a
 * new facade method exposing the node's NetworkId. App version is available via
 * BuildConfig but the address reveal is the gating dependency.
 *
 * Validates: onion address shows truncated peek by default, tapping "Show full address"
 * expands to the full 56-char string with copy icon active; key ID and app version
 * display correctly; transport section shows "Tor: Running".
 */
@ExcludeFromCoverage
@Preview(name = "3. Node — My Node sub-page (full identity)")
@Composable
private fun NodeMyNode_Preview() {
    BisqTheme.Preview {
        NodeMyNodeScreen(data = simulatedMyNode)
    }
}

/**
 * Preview 4: Node app — Overview edge case: disconnected / zero peers.
 *
 * DATA: free (Issue A) — numConnections=0, allDataReceived=false, isTorRunning=false
 * are all from existing StateFlow fields. onionAddressPeek graceful-null renders as
 * "Address loading..." without requiring any new API.
 *
 * This is the expected state during first launch before Tor establishes.
 * Validates: health badge RED "Offline", peer count chip shows "0" in danger red,
 * Tor chip shows "Stopped", onion address peek shows "Address loading...",
 * sub-page cards still render (user can tap "My Node" even offline to see version info).
 *
 * Design note: we deliberately DO NOT hide the sub-page cards when offline.
 * "My Node" is useful offline — the user can copy their onion address to share it
 * with a peer out-of-band. Hiding it would be a discoverability regression.
 */
@ExcludeFromCoverage
@Preview(name = "4. Node — Overview edge case (disconnected, 0 peers)")
@Composable
private fun NodeOverview_Disconnected_Preview() {
    BisqTheme.Preview {
        NodeNetworkOverviewScreen(
            data = disconnectedNodeOverview,
            onConnectionsClick = {},
            onMyNodeClick = {},
        )
    }
}

/**
 * Preview 4b: Node app — Connections sub-page in empty state.
 *
 * DATA: free (Issue A) — empty peer list derives from numConnections=0, no new API.
 * This screen is technically Issue B scope for the populated case but the empty state
 * itself requires no API; it is safe to ship as a navigable screen in Issue A.
 *
 * Validates: friendly empty state with hint text visible; no peer cards rendered.
 * This is the correct state when disconnectedNodeOverview is active.
 */
@ExcludeFromCoverage
@Preview(name = "4b. Node — Connections sub-page (empty / offline)")
@Composable
private fun NodeConnections_Empty_Preview() {
    BisqTheme.Preview {
        NodeConnectionsEmptyState()
    }
}

/**
 * Preview 4c: Node app — Overview in the syncing / cold-launch intermediate state.
 *
 * DATA: free (Issue A) — peerCount=3 from numConnections StateFlow, isDataSynced=false
 * from allDataReceived StateFlow. Both are available without new API work. This is the
 * state users see for 30-60 seconds on every cold launch and is the most common
 * intermediate state — it must have its own preview to confirm amber badge rendering.
 *
 * Validates: health badge AMBER "Syncing", peer count chip shows "3" in green (peers
 * ARE connected), Tor chip shows "Running", onion address peek visible. Sub-page cards
 * render normally — the user can navigate to "Connections" to see the 3 live peers even
 * while data is still syncing.
 */
@ExcludeFromCoverage
@Preview(name = "4c. Node — Overview (syncing, 3 peers, cold launch)")
@Composable
private fun NodeOverview_Syncing_Preview() {
    BisqTheme.Preview {
        NodeNetworkOverviewScreen(
            data = syncingNodeOverview,
            onConnectionsClick = {},
            onMyNodeClick = {},
        )
    }
}

/**
 * Preview 8: More menu — Network entry placement in the current flat menu.
 *
 * DATA: no network data — pure nav/menu placement preview.
 *
 * The "Network" entry is highlighted (green tint) so reviewers immediately spot it.
 * Sits between "Settings" and "Resources".
 *
 * Navigation note for implementation:
 *   - Add MenuItem.Leaf(label = "mobile.more.network".i18n(), icon = Res.drawable.nav_network,
 *       route = NavRoute.NetworkInfo)
 *     to MiscItemsPresenter.buildMenu(), after the Settings leaf.
 *   - When #1520 grouped menu lands, move this leaf into the APP section of
 *     buildCoreSections() in the production presenter. The screen itself is unchanged.
 */
@ExcludeFromCoverage
@Preview(name = "8. More menu — Network entry placement (flat list)")
@Composable
private fun MoreMenu_NetworkPlacement_Preview() {
    BisqTheme.Preview {
        MoreMenuWithNetworkEntry(items = flatMoreMenuItems)
    }
}

/**
 * Preview 8b: More menu — Network entry in the grouped #1520 layout (APP section).
 *
 * Shows the APP section only, with Network appearing between Settings and Resources.
 * This illustrates where it lands after the MoreMenuDesign.kt grouped restructure ships.
 * Shared with the Connect-app too — both apps have the same Network menu entry.
 */
@ExcludeFromCoverage
@Preview(name = "8b. More menu — Network in grouped APP section (#1520 target)")
@Composable
private fun MoreMenu_NetworkInGroupedApp_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(
                        horizontal = BisqUIConstants.ScreenPadding,
                        vertical = BisqUIConstants.ScreenPadding,
                    ),
        ) {
            // Section header — same style as MoreMenuDesign SectionHeader
            BisqText.XSmallMedium(
                text = "APP",
                color = BisqTheme.colors.mid_grey20,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = BisqUIConstants.ScreenPaddingHalf,
                            top = BisqUIConstants.ScreenPaddingHalf,
                        ),
            )

            // Settings row
            BisqButton(
                text = "Settings",
                onClick = {},
                fullWidth = true,
                backgroundColor = BisqTheme.colors.dark_grey40,
                leftIcon = {
                    Icon(
                        Icons.Filled.Settings,
                        "Settings",
                        Modifier.size(20.dp),
                        tint = BisqTheme.colors.light_grey10,
                    )
                },
                rightIcon = { ArrowRightIcon() },
                textAlign = TextAlign.Start,
                padding = PaddingValues(all = BisqUIConstants.ScreenPadding),
            )
            BisqGap.VHalf()

            // Network row — NEW, highlighted
            BisqButton(
                text = "Network",
                onClick = {},
                fullWidth = true,
                backgroundColor = BisqTheme.colors.primary2,
                leftIcon = {
                    androidx.compose.foundation.Image(
                        painter = painterResource(Res.drawable.nav_network),
                        contentDescription = "Network",
                        modifier = Modifier.size(20.dp),
                    )
                },
                rightIcon = { ArrowRightIcon() },
                textAlign = TextAlign.Start,
                padding = PaddingValues(all = BisqUIConstants.ScreenPadding),
            )
            BisqGap.VHalf()

            // Resources row
            BisqButton(
                text = "Resources",
                onClick = {},
                fullWidth = true,
                backgroundColor = BisqTheme.colors.dark_grey40,
                leftIcon = {
                    Icon(
                        Icons.Filled.Info,
                        "Resources",
                        Modifier.size(20.dp),
                        tint = BisqTheme.colors.light_grey10,
                    )
                },
                rightIcon = { ArrowRightIcon() },
                textAlign = TextAlign.Start,
                padding = PaddingValues(all = BisqUIConstants.ScreenPadding),
            )
            BisqGap.VHalf()

            // Contextual note for implementers (will not appear in production)
            BisqGap.V1()
            BisqText.XSmallLight(
                text = "Network moves to APP section when #1520 grouped menu ships.",
                color = BisqTheme.colors.mid_grey20,
            )
        }
    }
}
