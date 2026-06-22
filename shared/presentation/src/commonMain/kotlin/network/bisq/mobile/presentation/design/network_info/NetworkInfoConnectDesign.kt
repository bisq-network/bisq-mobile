/**
 * NetworkInfoConnectDesign.kt — Design PoC (Issue bisq-network/bisq-mobile#429)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * This file covers the Connect-app variants (Previews 5–7).
 * See NetworkInfoDesign.kt for the full design rationale, stakeholder summary,
 * data availability notes, i18n keys, and Node-app variants (Previews 1–4 and 8).
 *
 * ======================================================================================
 * CONNECT APP — CONCEPTUAL DIFFERENCE FROM NODE
 * ======================================================================================
 * Connect users are NOT direct P2P participants. They communicate with the Bisq network
 * through a trusted node they paired with at setup. The Network Info screen must make
 * this one-hop-removed topology VISUALLY OBVIOUS — users should never think they are
 * directly exposed to the peer network or that the peer list is "their" connections.
 *
 * This is achieved through:
 *   1. A prominent "Trusted Node" card at the top of the overview, showing the node's
 *      name and reachability badge. This is the user's PRIMARY network relationship.
 *   2. A subdued secondary section labelled "Via your node" for the peer count —
 *      framing it explicitly as data seen through the trusted node.
 *   3. The sub-page for the user's connection is called "My Connection", not "My Node",
 *      because the user has no node. It shows the WebSocket connection to the trusted node.
 *   4. "Connections" sub-page for Connect shows the peers that the TRUSTED NODE sees,
 *      with a persistent banner at the top reminding the user of this indirection:
 *      "These are peers connected to your trusted node, not to you directly."
 *
 * ======================================================================================
 * DATA AVAILABILITY FOR CONNECT
 * ======================================================================================
 * Already available without new API work:
 *   - Trusted node name / alias        → from pairing config (stored at pair-time)
 *   - WebSocket endpoint host          → from pairing config
 *   - WS connection state              → from ClientConnectivityService
 *   - Last response timestamp          → from WebSocketClientImpl.lastFrameReceivedAt
 *   - Average round-trip latency       → from ClientConnectivityService.averageTripTime
 *   - Tor routing flag                 → from pairing config (isTorEnabled)
 *
 * Needs new API plumbing:
 *   - Peer count seen by trusted node  → needs new websocket subscription topic or
 *                                        REST endpoint (/network/peer-count or similar)
 *   - Per-peer list from trusted node  → new REST endpoint (lower priority per HenrikJannsen)
 *
 * Note: the peer count is shown as "?" if the API endpoint does not exist yet.
 * This is the correct graceful degradation — show the card, leave the value empty,
 * rather than hiding the card and confusing users who wonder where "Connections" went.
 *
 * ======================================================================================
 * PREVIEWS IN THIS FILE
 * ======================================================================================
 * Preview 5:  Connect — Overview (trusted node reachable, 12 peers via node)
 *             DATA: topology + latency + last-seen = free (Issue A).
 *             peerCountViaNode requires new WS endpoint (Issue B Connect).
 * Preview 5b: Connect — Overview edge case (trusted node unreachable)
 *             DATA: free (Issue A) — isNodeReachable=false from ClientConnectivityService.
 * Preview 6:  Connect — My Connection sub-page (WS link connected, Tor-routed)
 *             DATA: free (Issue A) — all fields available from pairing config +
 *             ClientConnectivityService + WebSocketClientImpl.
 * Preview 6b: Connect — My Connection sub-page (WS disconnected)
 *             DATA: free (Issue A).
 * Preview 7:  Connect — Connections sub-page (5 peers via trusted node)
 *             DATA: requires new bisq2 API (Issue B scope).
 * Preview 7b: Connect — Connections sub-page (API unavailable / initial state)
 *             DATA: no API data needed for the unavailable state itself.
 */
package network.bisq.mobile.presentation.design.network_info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// ======================================================================================
// Simulated data — Connect app variants
// ======================================================================================

/**
 * Connection state from the perspective of the Connect app.
 * Maps to ClientConnectivityService state, not P2P network state.
 */
enum class SimulatedWsState {
    /** WebSocket connected and receiving frames. */
    CONNECTED,

    /** WebSocket disconnected or reconnecting. */
    DISCONNECTED,
}

/**
 * Summary data for the Network Info Overview screen — Connect app variant.
 *
 * [trustedNodeName]     — the alias the user gave the node at pairing, or host-truncated.
 * [trustedNodeEndpoint] — WS endpoint host, truncated for display (e.g. "abc123.onion").
 * [isNodeReachable]     — reflects current WS connection state from ClientConnectivityService.
 * [isTorRouted]         — whether the WS connection goes through Tor.
 * [peerCountViaNode]    — peer count seen by the trusted node, or null if API unavailable.
 * [wsLatencyMs]         — average round-trip latency in ms, or null if not measured yet.
 * [lastSeenDescription] — human-readable "X seconds ago" string from lastFrameReceivedAt.
 */
data class SimulatedConnectOverview(
    val trustedNodeName: String,
    val trustedNodeEndpoint: String,
    val isNodeReachable: Boolean,
    val isTorRouted: Boolean,
    val peerCountViaNode: Int?,
    val wsLatencyMs: Int?,
    val lastSeenDescription: String,
)

// ======================================================================================
// UI Actions — preview-only sealed class
// ======================================================================================

/**
 * Actions that the Network Info screens can emit to their host presenter.
 *
 * Preview-only in this PoC — production would route these through the MVP presenter.
 *
 * [OnCheckConnectionSettings] — emitted when the user taps "Check connection settings"
 *   in the unreachable state of BridgeTopologyCard. Production should navigate to the
 *   pairing / trusted-node settings screen so the user can correct a stale address or
 *   re-pair. This is a secondary affordance for the persistent-failure case; the primary
 *   tagline ("Connection lost — retrying") remains for the transient-failure case.
 */
sealed class NetworkInfoUiAction {
    data object OnCheckConnectionSettings : NetworkInfoUiAction()
}

// ======================================================================================
// Shared component: "bridge topology" diagram card
// ======================================================================================

/**
 * A visual affordance showing the single-hop topology: [You] → [Trusted Node] → [Network].
 *
 * This is the primary trust signal on the Connect overview. The user needs to understand
 * at a glance that:
 *   a. They are connected to ONE trusted node (the middle box).
 *   b. The trusted node connects them to the broader Bisq network.
 *   c. Their IP / identity is NOT exposed to the peer network.
 *
 * The diagram uses three boxes connected by right-arrow separators.
 * It is deliberately simple: no animation, no icons that might be misread.
 * Colors: the trusted node box uses primary (green) when reachable, danger (red) otherwise.
 * This makes the central relationship — "is my trusted node up?" — instantly scannable.
 */
@Composable
private fun BridgeTopologyCard(
    trustedNodeName: String,
    isReachable: Boolean,
    onAction: (NetworkInfoUiAction) -> Unit,
) {
    val nodeColor = if (isReachable) BisqTheme.colors.primary else BisqTheme.colors.danger

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey40)
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPadding2X,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BisqText.XSmallMedium(
            text = "YOU ARE CONNECTED VIA",
            color = BisqTheme.colors.mid_grey20,
        )
        BisqGap.V1()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            // You box
            TopologyBox(label = "You", color = BisqTheme.colors.mid_grey30)

            BisqGap.H1()
            BisqText.XSmallLight(text = "→", color = BisqTheme.colors.mid_grey20)
            BisqGap.H1()

            // Trusted node box — coloured by reachability.
            // Width-capped so a long node name ellipsizes instead of overflowing the row
            // and compressing the You / Network boxes.
            TopologyBox(
                label = trustedNodeName,
                color = nodeColor,
                modifier = Modifier.widthIn(max = 140.dp),
            )

            BisqGap.H1()
            BisqText.XSmallLight(text = "→", color = BisqTheme.colors.mid_grey20)
            BisqGap.H1()

            // Network box
            TopologyBox(label = "Network", color = BisqTheme.colors.mid_grey30)
        }

        BisqGap.V1()

        // Primary tagline — kept for the transient-failure case so the auto-retry
        // reassurance is still visible. Do NOT change this to "permanently down" framing:
        // most disconnections are transient (Tor circuit rotation, brief network drop).
        val tagline =
            if (isReachable) {
                "Your identity is not exposed to the P2P network"
            } else {
                "Connection lost — retrying"
            }
        BisqText.XSmallLight(
            text = tagline,
            color = if (isReachable) BisqTheme.colors.mid_grey20 else BisqTheme.colors.danger,
            textAlign = TextAlign.Center,
        )

        // Secondary affordance — only shown when unreachable. Gives the user an
        // explicit out for the persistent-failure case (wrong address, stale
        // credentials, decommissioned node) without overriding the primary tagline.
        if (!isReachable) {
            BisqGap.V1()
            BisqButton(
                text = "Check connection settings",
                onClick = { onAction(NetworkInfoUiAction.OnCheckConnectionSettings) },
                backgroundColor = androidx.compose.ui.graphics.Color.Transparent,
                color = BisqTheme.colors.mid_grey30,
            )
        }
    }
}

@Composable
private fun TopologyBox(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadiusSmall))
                .background(BisqTheme.colors.dark_grey50)
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPaddingHalf,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = BisqTheme.typography.xsmallMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ======================================================================================
// CONNECT OVERVIEW SCREEN
// ======================================================================================

/**
 * Connect app — Network Info Overview screen.
 *
 * Visual hierarchy (top to bottom):
 *   1. Screen title "Network Info"
 *   2. Bridge topology card — YOU → [trusted node] → Network
 *      This is the dominant element. It communicates the privacy model immediately.
 *   3. Trusted Node section — name, endpoint, reachability badge
 *   4. "Network (via your node)" secondary section — peer count the trusted node sees
 *   5. Two sub-page entry cards: "My Connection" (WS link details) + "Connections"
 *      (peers seen by trusted node, if API available)
 *
 * Rationale for ordering: hierarchy follows user priority.
 *   - "Am I connected?" (the topology card and node reachability) is the highest priority.
 *   - "How many peers does my node see?" is secondary, useful context but not critical.
 *   - Sub-page deep links are always last — they are for users who want more detail.
 *
 * [data]                  — overview data (already derived from service state).
 * [onMyConnectionClick]   — navigate to the My Connection sub-page.
 * [onConnectionsClick]    — navigate to the Connections sub-page (trusted node peers).
 * [onAction]              — UI action callback (e.g. OnCheckConnectionSettings).
 */
@Composable
fun ConnectNetworkOverviewScreen(
    data: SimulatedConnectOverview,
    onMyConnectionClick: () -> Unit,
    onConnectionsClick: () -> Unit,
    onAction: (NetworkInfoUiAction) -> Unit = {},
) {
    val nodeHealth = if (data.isNodeReachable) SimulatedHealthState.HEALTHY else SimulatedHealthState.OFFLINE

    BisqScrollLayout(
        contentPadding =
            PaddingValues(
                horizontal = BisqUIConstants.ScreenPadding,
                vertical = BisqUIConstants.ScreenPadding,
            ),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        // ── Header row: title + node reachability badge ───────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BisqText.H5Medium(text = "Network Info", color = BisqTheme.colors.white)
            HealthBadge(state = nodeHealth)
        }

        BisqGap.V2()

        // ── Bridge topology diagram ───────────────────────────────────────
        BridgeTopologyCard(
            trustedNodeName = data.trustedNodeName,
            isReachable = data.isNodeReachable,
            onAction = onAction,
        )

        BisqGap.V2()

        // ── Trusted Node detail section ───────────────────────────────────
        NetworkSectionLabel(text = "Trusted Node")
        BisqGap.VHalf()

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                    .background(BisqTheme.colors.dark_grey40),
        ) {
            InfoRow(label = "Name", value = data.trustedNodeName)
            BisqHDivider()
            InfoRow(
                label = "Endpoint",
                value = data.trustedNodeEndpoint,
                valueMaxLines = 1,
            )
            BisqHDivider()
            InfoRow(
                label = "Routing",
                value = if (data.isTorRouted) "Tor" else "Clearnet",
            )
            BisqHDivider()
            InfoRow(
                label = "Latency",
                value = data.wsLatencyMs?.let { "${it}ms" } ?: "Measuring...",
                trailing = null,
            )
            BisqGap.VHalf()
        }

        BisqGap.V2()

        // ── Network (via trusted node) section ────────────────────────────
        NetworkSectionLabel(text = "Via your node")
        BisqGap.VHalf()

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                    .background(BisqTheme.colors.dark_grey40),
        ) {
            InfoRow(
                label = "Peers",
                value = data.peerCountViaNode?.let { "$it connected" } ?: "Not available",
            )
            BisqGap.VHalf()
        }

        BisqGap.V2()

        // ── Sub-page navigation cards ─────────────────────────────────────
        NetworkSectionLabel(text = "Details")
        BisqGap.VHalf()

        SubPageEntryCard(onClick = onMyConnectionClick) {
            Column {
                BisqText.BaseRegular(text = "My Connection", color = BisqTheme.colors.white)
                BisqGap.VQuarter()
                Text(
                    text = "WebSocket link to ${data.trustedNodeName}",
                    style = BisqTheme.typography.smallLight,
                    color = BisqTheme.colors.mid_grey20,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        BisqGap.VHalf()

        SubPageEntryCard(onClick = onConnectionsClick) {
            Column {
                BisqText.BaseRegular(text = "Connections", color = BisqTheme.colors.white)
                BisqGap.VQuarter()
                Text(
                    text =
                        data.peerCountViaNode?.let { "Peers seen by ${data.trustedNodeName}: $it" }
                            ?: "Requires node API extension",
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
// CONNECT — MY CONNECTION SUB-PAGE
// ======================================================================================

/**
 * Connect app — My Connection sub-page.
 *
 * This replaces "My Node" for Connect users. There is no local node; this screen
 * describes the WebSocket connection to the trusted node.
 *
 * Fields shown:
 *   - Endpoint: the trusted node's WS host (may be .onion)
 *   - Routing: Tor / Clearnet
 *   - Latency: average round-trip time from ClientConnectivityService.averageTripTime
 *   - Last response: human-readable "X seconds ago" from lastFrameReceivedAt
 *   - Connection state: Connected / Disconnected badge
 *
 * What is NOT shown here:
 *   - Per-message stats (sent/received bytes) — developer-level data, not user-facing.
 *   - WebSocket session ID — not meaningful to end users.
 *
 * [data] — same SimulatedConnectOverview used by the overview screen (data is shared).
 * [wsState] — current WS connection state.
 */
@Composable
fun ConnectMyConnectionScreen(
    data: SimulatedConnectOverview,
    wsState: SimulatedWsState,
) {
    val stateLabel =
        when (wsState) {
            SimulatedWsState.CONNECTED -> "Connected"
            SimulatedWsState.DISCONNECTED -> "Disconnected"
        }
    val stateColor =
        when (wsState) {
            SimulatedWsState.CONNECTED -> BisqTheme.colors.primary
            SimulatedWsState.DISCONNECTED -> BisqTheme.colors.danger
        }

    BisqScrollLayout(
        contentPadding =
            PaddingValues(
                horizontal = BisqUIConstants.ScreenPadding,
                vertical = BisqUIConstants.ScreenPadding,
            ),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BisqText.H5Medium(text = "My Connection", color = BisqTheme.colors.white)
            // Inline status badge (reuse HealthBadge pattern, but text from wsState)
            androidx.compose.foundation.layout.Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                        .background(stateColor.copy(alpha = 0.15f))
                        .padding(
                            horizontal = BisqUIConstants.ScreenPadding,
                            vertical = BisqUIConstants.ScreenPaddingHalf,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                BisqText.XSmallMedium(text = stateLabel, color = stateColor)
            }
        }

        BisqGap.V2()

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                    .background(BisqTheme.colors.dark_grey40),
        ) {
            NetworkSectionLabel(text = "Trusted Node")
            BisqHDivider()
            InfoRow(label = "Name", value = data.trustedNodeName)
            BisqHDivider()
            InfoRow(label = "Endpoint", value = data.trustedNodeEndpoint, valueMaxLines = 1)
            BisqGap.VHalf()
        }

        BisqGap.V2()

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                    .background(BisqTheme.colors.dark_grey40),
        ) {
            NetworkSectionLabel(text = "Connection Quality")
            BisqHDivider()
            InfoRow(
                label = "Routing",
                value = if (data.isTorRouted) "Tor" else "Clearnet",
            )
            BisqHDivider()
            InfoRow(
                label = "Latency",
                value = data.wsLatencyMs?.let { "${it}ms" } ?: "Measuring...",
            )
            BisqHDivider()
            InfoRow(
                label = "Last response",
                value = data.lastSeenDescription,
            )
            BisqGap.VHalf()
        }
    }
}

// ======================================================================================
// CONNECT — CONNECTIONS SUB-PAGE (peers seen by trusted node)
// ======================================================================================

/**
 * Connect app — Connections sub-page.
 *
 * IMPORTANT: this screen shows peers connected to the TRUSTED NODE, not to the user.
 * The banner at the top makes this explicit. Without it, users might think these are
 * their own connections, which would be a trust / privacy misunderstanding.
 *
 * Two sub-states:
 *   a. API unavailable — shows a friendly "not yet available" state with an explanation.
 *   b. Populated — shows the same ConnectionCard list used by the Node app,
 *      preceded by the "via your node" banner.
 *
 * [trustedNodeName]  — used in the banner and heading for context.
 * [peers]            — the list of peers seen by the trusted node. Null = API not available.
 */
@Composable
fun ConnectConnectionsScreen(
    trustedNodeName: String,
    peers: List<SimulatedPeer>?,
) {
    if (peers == null) {
        ConnectConnectionsUnavailableState(trustedNodeName = trustedNodeName)
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BisqText.H5Medium(text = "Connections", color = BisqTheme.colors.white)
            BisqText.SmallRegular(
                text = "${peers.size} peers",
                color = BisqTheme.colors.mid_grey20,
            )
        }

        BisqGap.V1()

        // "Via trusted node" informational banner
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
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Start,
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Info",
                tint = BisqTheme.colors.mid_grey20,
                modifier = Modifier.size(16.dp),
            )
            BisqGap.H1()
            BisqText.XSmallLight(
                text =
                    "These are peers connected to $trustedNodeName, not to you directly. " +
                        "Your device is one hop removed from the P2P network.",
                color = BisqTheme.colors.mid_grey20,
            )
        }

        BisqGap.V1()

        peers.forEach { peer ->
            ConnectionCard(peer = peer)
            BisqGap.VHalf()
        }
    }
}

/**
 * Empty/unavailable state for the Connect Connections sub-page.
 *
 * Shown when the trusted node has not yet exposed a peer-list API endpoint.
 * This is the expected state for the initial implementation — the overview shows
 * the peer COUNT (once that API exists), but the per-peer list is lower priority.
 *
 * The state is informative, not an error: the user's node is working fine; the
 * app simply doesn't surface the peer list yet.
 */
@Composable
private fun ConnectConnectionsUnavailableState(trustedNodeName: String) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPadding,
                ),
        verticalArrangement = Arrangement.Top,
    ) {
        BisqText.H5Medium(text = "Connections", color = BisqTheme.colors.white)

        BisqGap.V4()

        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = "Not available",
            tint = BisqTheme.colors.mid_grey10,
            modifier =
                Modifier
                    .size(48.dp)
                    .align(Alignment.CenterHorizontally),
        )

        BisqGap.V2()

        BisqText.BaseMedium(
            text = "Peer list not yet available",
            color = BisqTheme.colors.mid_grey30,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        BisqGap.V1()

        BisqText.SmallLight(
            text =
                "$trustedNodeName is connected to the network. The detailed peer list " +
                    "will be available in a future update.",
            color = BisqTheme.colors.mid_grey20,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BisqUIConstants.ScreenPadding2X),
        )
    }
}

// ======================================================================================
// SIMULATED DATA FOR CONNECT PREVIEWS
// ======================================================================================

private val reachableConnectOverview =
    SimulatedConnectOverview(
        trustedNodeName = "My Home Node",
        trustedNodeEndpoint = "r7m2xpqowg3bvf8t.onion",
        isNodeReachable = true,
        isTorRouted = true,
        peerCountViaNode = 12,
        wsLatencyMs = 340,
        lastSeenDescription = "2 seconds ago",
    )

private val unreachableConnectOverview =
    SimulatedConnectOverview(
        trustedNodeName = "My Home Node",
        trustedNodeEndpoint = "r7m2xpqowg3bvf8t.onion",
        isNodeReachable = false,
        isTorRouted = true,
        peerCountViaNode = null,
        wsLatencyMs = null,
        lastSeenDescription = "4 minutes ago",
    )

private val connectPeers =
    listOf(
        SimulatedPeer(
            addressTruncated = "jd4tx3nljykg5z3v...",
            direction = SimulatedDirection.OUTBOUND,
            connectedSince = "7 min",
            isSeed = true,
        ),
        SimulatedPeer(
            addressTruncated = "r7m2xpqowg3bvf8t...",
            direction = SimulatedDirection.INBOUND,
            connectedSince = "22 min",
        ),
        SimulatedPeer(
            addressTruncated = "k9fz6wqnmaecyh2p...",
            direction = SimulatedDirection.OUTBOUND,
            connectedSince = "1 h",
            isSeed = true,
        ),
        SimulatedPeer(
            addressTruncated = "v5bld8qkzrweyxn3...",
            direction = SimulatedDirection.INBOUND,
            connectedSince = "5 min",
        ),
        SimulatedPeer(
            addressTruncated = "p2ajmtq9xsgfhdo7...",
            direction = SimulatedDirection.OUTBOUND,
            connectedSince = "41 min",
        ),
    )

// ======================================================================================
// PREVIEWS
// ======================================================================================

/**
 * Preview 5: Connect app — Overview (trusted node reachable, 12 peers via node).
 *
 * DATA: topology + latency + last-seen = free (Issue A). peerCountViaNode=12 requires
 * a new WS subscription topic or REST endpoint (Issue B Connect scope). In Issue A the
 * peers row renders as "Not available" graceful-null until the API lands.
 *
 * Validates:
 *   - Bridge topology card renders [You] → [My Home Node (green)] → [Network]
 *   - Health badge shows "Healthy" in green
 *   - "Via your node" section shows "12 connected"
 *   - Both sub-page entry cards render with correct secondary labels
 *   - Tor routing shown in Trusted Node section
 */
@ExcludeFromCoverage
@Preview(name = "5. Connect — Overview (reachable, 12 peers via node)")
@Composable
private fun ConnectOverview_Reachable_Preview() {
    BisqTheme.Preview {
        ConnectNetworkOverviewScreen(
            data = reachableConnectOverview,
            onMyConnectionClick = {},
            onConnectionsClick = {},
        )
    }
}

/**
 * Preview 5b: Connect app — Overview edge case (trusted node unreachable).
 *
 * DATA: free (Issue A) — isNodeReachable=false derives from ClientConnectivityService
 * with no new API work. The "Check connection settings" secondary button is visible
 * here, wired to NetworkInfoUiAction.OnCheckConnectionSettings.
 *
 * Validates:
 *   - Bridge topology card shows [My Home Node] in RED
 *   - Health badge shows "Offline" in red
 *   - "Via your node" section shows "Not available" since node is down
 *   - Primary tagline reads "Connection lost — retrying" (transient reassurance)
 *   - "Check connection settings" text button appears below the tagline (persistent-failure out)
 *   - Sub-page cards still render (user can review "My Connection" even when offline,
 *     e.g., to verify the endpoint is correct or to diagnose why reconnection fails)
 */
@ExcludeFromCoverage
@Preview(name = "5b. Connect — Overview (trusted node unreachable)")
@Composable
private fun ConnectOverview_Unreachable_Preview() {
    BisqTheme.Preview {
        ConnectNetworkOverviewScreen(
            data = unreachableConnectOverview,
            onMyConnectionClick = {},
            onConnectionsClick = {},
        )
    }
}

/**
 * Preview 6: Connect app — My Connection sub-page (WS link connected, Tor-routed).
 *
 * DATA: free (Issue A) — all fields sourced from pairing config (name, endpoint, isTorRouted)
 * + ClientConnectivityService.averageTripTime (latency) + WebSocketClientImpl.lastFrameReceivedAt
 * (last-seen). No new bisq2 API required.
 *
 * Validates:
 *   - "Connected" badge in green at top right
 *   - Trusted Node card shows name + .onion endpoint
 *   - Connection Quality card shows Tor, latency 340ms, "2 seconds ago"
 */
@ExcludeFromCoverage
@Preview(name = "6. Connect — My Connection sub-page (connected via Tor)")
@Composable
private fun ConnectMyConnection_Connected_Preview() {
    BisqTheme.Preview {
        ConnectMyConnectionScreen(
            data = reachableConnectOverview,
            wsState = SimulatedWsState.CONNECTED,
        )
    }
}

/**
 * Preview 6b: Connect app — My Connection sub-page (WS disconnected).
 *
 * DATA: free (Issue A) — wsState=DISCONNECTED from ClientConnectivityService; last-seen
 * "4 minutes ago" from WebSocketClientImpl.lastFrameReceivedAt. No new API needed.
 *
 * Validates: "Disconnected" badge in red at top right; last-response shows "4 minutes ago"
 * from unreachableConnectOverview, making it clear the connection has been down for a while.
 */
@ExcludeFromCoverage
@Preview(name = "6b. Connect — My Connection sub-page (disconnected)")
@Composable
private fun ConnectMyConnection_Disconnected_Preview() {
    BisqTheme.Preview {
        ConnectMyConnectionScreen(
            data = unreachableConnectOverview,
            wsState = SimulatedWsState.DISCONNECTED,
        )
    }
}

/**
 * Preview 7: Connect app — Connections sub-page (5 peers seen by trusted node).
 *
 * DATA: requires new bisq2 API (Issue B scope) — per-peer list from the trusted node
 * needs a new REST endpoint. Until that lands, this sub-page renders Preview 7b instead.
 *
 * Validates:
 *   - "Via your node" informational banner appears at the top — the key design requirement
 *     from rodvar: users must understand they are NOT directly connected to these peers.
 *   - ConnectionCard list renders correctly using the same component as the Node app.
 *   - Seed badges appear on the first and third cards.
 */
@ExcludeFromCoverage
@Preview(name = "7. Connect — Connections sub-page (5 peers via trusted node)")
@Composable
private fun ConnectConnections_Populated_Preview() {
    BisqTheme.Preview {
        ConnectConnectionsScreen(
            trustedNodeName = reachableConnectOverview.trustedNodeName,
            peers = connectPeers,
        )
    }
}

/**
 * Preview 7b: Connect app — Connections sub-page (API unavailable / not yet wired).
 *
 * DATA: no API data needed for the unavailable state itself — peers=null renders an
 * informational empty state. This is the correct initial state for Issue A / early
 * Issue B; no bisq2 endpoint exists yet.
 *
 * This is the expected state for the initial implementation — the trusted node does not
 * yet expose a peer-list endpoint. The screen gracefully explains the situation without
 * making it look like an error.
 */
@ExcludeFromCoverage
@Preview(name = "7b. Connect — Connections sub-page (API unavailable, initial state)")
@Composable
private fun ConnectConnections_Unavailable_Preview() {
    BisqTheme.Preview {
        ConnectConnectionsScreen(
            trustedNodeName = reachableConnectOverview.trustedNodeName,
            peers = null,
        )
    }
}
