/**
 * BootstrapDesign.kt — Design PoC (Issue bisq-network/bisq-mobile#434)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ====================================================================================
 * WHAT THIS REPLACES (AND WHAT IT DOESN'T)
 * ====================================================================================
 * The existing SplashScreen shows: logo centred, a thin LinearProgressIndicator, and a
 * single status string below it ("Starting Bisq", "Bootstrap to P2P network", etc.).
 * This PoC augments that phase only. It does NOT touch onboarding, pairing, or
 * the post-bootstrap screens.
 *
 * ====================================================================================
 * DESKTOP → MOBILE TRANSLATION RATIONALE
 * ====================================================================================
 * The Bisq2 desktop bootstrap UI (SplashView.java + BootstrapElement.java) has:
 *   - Logo + version header, centred
 *   - A 535px-wide LinearProgressBar that advances over a 120s window
 *   - An overall state label + elapsed timer ("Bootstrap to P2P network | 12 sec.")
 *   - A vertically-stacked grid of BootstrapElement rows, one per transport × step:
 *       [icon] [label: "Starting Tor"] | [duration: "3 sec."]
 *       [icon] [label: "Tor Started"]  | [5 sec.]
 *       [icon] [label: "Connecting to P2P network"] | [0 connections]
 *   - A yellow "slow startup" warning injected at position 2 if > 120s, with a
 *     "Delete Tor directory and shut down" action button.
 *
 * What stayed on mobile:
 *   - The linear progress bar (already exists as BisqProgressBar; preserved position)
 *   - The overall state label as a title
 *   - The per-step row pattern (icon + label + secondary detail) — translated to a
 *     vertical step list with tinted backgrounds and completion checkmarks
 *   - The slow-startup warning surface (translated to an amber inline banner replacing
 *     the bottom region, not a modal, because modals interrupt; the user can see
 *     progress is still happening)
 *
 * What changed:
 *   - Desktop grid is horizontal-label-heavy (535px wide). Mobile uses a card-per-step
 *     approach: each step occupies a full-width row with a status indicator on the left.
 *     This is more thumb-friendly and readable at 360dp widths.
 *   - Desktop shows elapsed time per step ("3 sec."). On mobile this clutters the
 *     limited vertical space; elapsed time is surfaced only in the slow-path banner
 *     where it becomes meaningful ("still working after 75 seconds").
 *   - Desktop has three simultaneous transports (Clear, Tor, I2P). Mobile Node app runs
 *     Tor only. Steps are linearised rather than shown per-transport.
 *   - Connection peer count ("12 connections") is shown as a live count in the
 *     peer-discovery step row — compact, does not need a separate column.
 *
 * ====================================================================================
 * CONNECT vs NODE: WHY THEY LOOK DIFFERENT
 * ====================================================================================
 * Connect's bootstrap is a single-phase WebSocket handshake. There is no local Tor,
 * no P2P peer graph, no inventory download. The "steps" the user cares about are:
 *   1. Connecting to trusted node (TCP/WS)
 *   2. Loading initial data (offers, market price, profile)
 *   (3. Done)
 * Showing a Tor icon or peer-count here would be misleading — the Tor transport, if
 * any, lives on the remote node, not in the app. The Connect UI therefore uses a
 * two-step horizontal progress indicator (dots + line) that matches the actual
 * work happening. It is deliberately simpler: sub-second on a local Wi-Fi hop,
 * a few seconds over a Tor onion.
 *
 * Node's bootstrap is the full Bisq2 startup:
 *   1. Starting Tor (can take 30-60s+ cold, shows % via TorState.Starting)
 *   2. Discovering P2P peers (shows live connection count vs target)
 *   3. Syncing data inventory (offers + market price)
 *   4. Ready
 * Each step has a distinct icon (Tor onion, network, cloud-download, checkmark) and
 * a distinct secondary line. This matches what the desktop surfaces and gives users
 * a mental model for why they are waiting.
 *
 * ====================================================================================
 * STATE-TRANSITION CONTRACT
 * ====================================================================================
 * Connect app — driven by overallProgress from ClientSplashPresenter:
 *
 *   overallProgress = 0.0         → strip phase 1 active (Connecting)
 *   overallProgress = 0.5         → phase 2 active (Loading data)
 *   overallProgress = 1.0 + error string → navigate to TrustedNodeSetup
 *                                    (no "Ready" frame rendered)
 *   overallProgress = 1.0 + success     → navigateToNextScreen() fires automatically
 *                                    ("Ready" frame visible ~100ms then dismissed)
 *
 * !!! IMPLEMENTER TRAP — progress=1.0f is AMBIGUOUS !!!
 * Both success AND failure emit overallProgress = 1.0f. The presenter must check the
 * connectivity status string (or an explicit error flag) IN ADDITION to the float.
 * Never use `progress == 1.0f` alone to decide whether to show "Ready" — it will
 * flash "Ready" for ~100ms before navigating to the error screen on failures.
 *
 * Connect failures navigate to TrustedNodeSetup rather than showing an inline
 * failure state — this is by design, see ClientSplashPresenter.navigateToTrustedNodeSetup.
 * Do not add a Connect failure state here; it would fight the existing navigation logic.
 *
 * Node app — driven by observeTorState() + NodeApplicationBootstrapFacade. Progress is
 * COARSE: the facade emits 4 ApplicationService stages (+ Tor %), so the 4-step UI maps
 * onto the real setProgress() values like this:
 *
 *   Tor progress 0-99%                     → step 1 IN_PROGRESS  (overallProgress 0.0)
 *   Tor started                            → step 1 DONE         (0.25)
 *   ApplicationService INITIALIZE_NETWORK  → step 2 (peers)      (0.5)
 *   ApplicationService INITIALIZE_SERVICES → step 3 (data)       (0.75)
 *   APP_INITIALIZED + success              → step 4 DONE, navigateToNextScreen() (1.0)
 *   overallProgress = 1.0 + failure        → step 1 FAILED, TorFailureActions shown
 *
 * NOTE: those are the ONLY progress signals the facade emits — there is no fine-grained
 * "peer count ≥ target" or "inventory received" progress event. The peers/data step
 * labels are a UX abstraction over INITIALIZE_NETWORK / INITIALIZE_SERVICES; a live peer
 * count (step-2 detail) would need extra wiring and is not surfaced by the facade today.
 *
 * ====================================================================================
 * SLOW-PATH TREATMENT  (the generic "silent hang" timeout — distinct from Tor failure)
 * ====================================================================================
 * This covers a stage that simply HANGS with no error — NOT a Tor crash (that is the
 * 60s torBootstrapFailed path, see FAILURE / RETRY below). At 90 seconds (the existing
 * BOOTSTRAP_STAGE_TIMEOUT_MS) the SplashPresenter shows the generic timeout dialog
 * ("taking longer than usual — restart / keep waiting", isTimeoutDialogVisible). The PoC
 * adds a non-dismissible amber inline banner that appears at 75 seconds — 15 seconds
 * before the dialog — so the user receives a plausible explanation before the harder
 * interrupt arrives. The 75s offset is intentional: it pre-empts the dialog rather than
 * racing with it. NOTE: the 75s banner does NOT exist in production yet — it is new work
 * this issue introduces; only the 90s dialog exists today.
 *
 * IMPORTANT — the banner's elapsed time counter must measure wall-clock time from
 * bootstrap start, NOT from any individual stage. The per-step timeout timers reset
 * the 90s dialog clock; the banner must not follow suit or it will show "15s elapsed"
 * when the user has actually been waiting 75 seconds.
 *
 *   "Still connecting — this is normal on first launch over Tor.
 *    Average first-launch time: 2-3 minutes."
 * The banner stays below the step list. The timeout dialog is still available for the
 * "truly stuck" case (the existing dialog flow is unchanged), but the banner defers
 * that by giving the user a plausible explanation first.
 *
 * ====================================================================================
 * FAILURE / RETRY TREATMENT  (Node app only — Connect has no local Tor)
 * ====================================================================================
 * The inline Tor-failure state (step 1 FAILED: danger-colored icon + strikethrough
 * label, plus the retry actions) is driven by ApplicationBootstrapFacade.torBootstrapFailed
 * — the same flag the existing dialog uses. The PoC just INLINES it; it adds no new
 * trigger or timer.
 *
 * IMPORTANT — the Tor-failure trigger is a 60s grace, NOT 90s:
 * The facade sets torBootstrapFailed=true only when Tor reaches Stopped WITH an error AND
 * that error happens after TOR_FAILURE_GRACE_PERIOD_MS (60s) from torStartingTimestamp.
 * A Tor error within the first 60s is SUPPRESSED (logged, state stays "Tor starting"),
 * because Tor frequently recovers on slow networks / onion circuits — surfacing it early
 * is a false alarm. The inline failure must therefore reflect torBootstrapFailed and must
 * never roll its own timer.
 *
 * This is a SEPARATE mechanism from the 90s slow-path timeout above:
 *   - 60s + an actual Tor error  → torBootstrapFailed → inline FAILED + retry actions
 *   - 90s silent hang (no error) → isTimeoutDialogVisible → generic timeout dialog
 * When a real Tor failure fires, the facade calls cancelTimeout(), and
 * SplashPresenter.computeActiveDialog prioritizes torBootstrapFailed above the timeout —
 * so the two never collide.
 *
 * The two retry actions (Restart Tor / Clear Tor Data) are unchanged — they correspond to
 * the existing SplashUiAction.OnRestartTor and OnPurgeRestartTor.
 *
 * ====================================================================================
 * I18N KEYS NEEDED (not in current mobile.properties or application.properties)
 * ====================================================================================
 *
 * -- Connect app --
 * mobile.bootstrap.connect.step.connecting=Connecting to trusted node
 * mobile.bootstrap.connect.step.connecting.detail=Establishing encrypted connection
 * mobile.bootstrap.connect.step.loadingData=Loading initial data
 * mobile.bootstrap.connect.step.loadingData.detail=Fetching offers and market price
 * mobile.bootstrap.connect.step.done=Ready
 * mobile.bootstrap.connect.title=Connecting to your node
 * mobile.bootstrap.connect.subtitle=Bisq Connect links to your trusted Bisq2 node
 *
 * -- Node app (Tor step labels beyond existing mobile.bootstrap.tor.*) --
 * mobile.bootstrap.node.title=Starting Bisq Node
 * mobile.bootstrap.node.subtitle=Connecting to the Bisq P2P network via Tor
 * mobile.bootstrap.node.step.tor=Starting Tor
 * mobile.bootstrap.node.step.tor.detail={0}% — building circuit
 * mobile.bootstrap.node.step.tor.done.detail=Tor circuit established
 * mobile.bootstrap.node.step.peers=Discovering peers
 * mobile.bootstrap.node.step.peers.detail={0} of {1} peers connected
 * mobile.bootstrap.node.step.peers.done.detail={0} peers connected
 * mobile.bootstrap.node.step.data=Syncing data
 * mobile.bootstrap.node.step.data.detail=Downloading offers and market prices
 * mobile.bootstrap.node.step.data.done.detail=Data received
 * mobile.bootstrap.node.step.ready=Ready
 *
 * -- Slow-path banner (both apps, triggered at 75s wall-clock from bootstrap start) --
 * mobile.bootstrap.slowPath.title=Still connecting — this is normal
 * mobile.bootstrap.slowPath.body=First launch over Tor typically takes 2-3 minutes.\nYour connection is encrypted and private.
 * mobile.bootstrap.slowPath.elapsed={0}s elapsed
 *
 * -- Connect time-aware reassurance (shown after ~8-10s stalled on phase 1, P1) --
 * mobile.bootstrap.connect.step.connecting.detail.slow=Encrypted connections can take a moment to establish.
 *
 * -- Node failure row label --
 * mobile.bootstrap.node.step.tor.failed.label=Tor failed to start
 * mobile.bootstrap.node.step.tor.failed.detail=Check your internet connection
 */

package network.bisq.mobile.presentation.design.bootstrap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.ProgressIndicatorDefaults.drawStopIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.BisqLogoGrey
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// ==================================================================================
// Data model for previews — no domain type imports
// ==================================================================================

private enum class SimulatedStepStatus {
    PENDING,
    IN_PROGRESS,
    DONE,
    FAILED,
}

private data class SimulatedStep(
    val icon: String,
    val label: String,
    val detail: String,
    val status: SimulatedStepStatus,
)

// ==================================================================================
// Shared layout shell
// ==================================================================================

/**
 * Full-screen bootstrap shell used by both Connect and Node variants.
 * Keeps the logo/version header at top, step content in the middle (vertically
 * centred), and the overall progress bar pinned to the bottom above the status line.
 *
 * @param overallProgress Expected range: 0f..1f (clamped visually by LinearProgressIndicator).
 *   In production, callers must also check an error flag alongside this value — see the
 *   STATE-TRANSITION CONTRACT note at the top of this file. Do NOT use require() here:
 *   this is a preview-only composable and a hard throw would break the preview renderer
 *   during iteration. Add the validation in the production presenter instead.
 */
@Composable
private fun BootstrapShell(
    title: String,
    subtitle: String,
    overallProgress: Float,
    appVersion: String,
    slowPathBannerVisible: Boolean = false,
    slowPathElapsed: String = "",
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BisqTheme.colors.backgroundColor)
                .padding(horizontal = BisqUIConstants.ScreenPadding2X),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ---- Header ----
        Spacer(modifier = Modifier.weight(0.12f))
        BisqLogoGrey(modifier = Modifier.size(80.dp))
        BisqGap.V1()
        BisqText.BaseLight(
            text = appVersion,
            color = BisqTheme.colors.mid_grey20,
        )
        Spacer(modifier = Modifier.weight(0.08f))

        // ---- Title block ----
        BisqText.H5Light(
            text = title,
            color = BisqTheme.colors.white,
            textAlign = TextAlign.Center,
        )
        BisqGap.VHalf()
        BisqText.SmallLight(
            text = subtitle,
            color = BisqTheme.colors.mid_grey20,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.weight(0.1f))

        // ---- Step content ----
        content()

        // ---- Slow-path reassurance banner ----
        if (slowPathBannerVisible) {
            BisqGap.V2()
            SlowPathBanner(elapsed = slowPathElapsed)
        }

        Spacer(modifier = Modifier.weight(1f))

        // ---- Overall progress bar (bottom) ----
        BootstrapProgressBar(progress = overallProgress)
        BisqGap.VHalf()
    }
}

// ==================================================================================
// Atoms
// ==================================================================================

@Composable
private fun BootstrapProgressBar(progress: Float) {
    LinearProgressIndicator(
        progress = { progress },
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .height(2.dp),
        trackColor = BisqTheme.colors.mid_grey10,
        color = BisqTheme.colors.primary,
        gapSize = 0.dp,
        drawStopIndicator = {
            drawStopIndicator(
                drawScope = this,
                stopSize = 0.dp,
                color = BisqTheme.colors.mid_grey10,
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )
        },
    )
}

@Composable
private fun SlowPathBanner(elapsed: String) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.yellow50)
                .border(
                    width = 1.dp,
                    color = BisqTheme.colors.yellow30,
                    shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                ).padding(BisqUIConstants.ScreenPadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BisqText.SmallMedium(
                text = "Still connecting - this is normal",
                color = BisqTheme.colors.yellow,
            )
            if (elapsed.isNotEmpty()) {
                Spacer(modifier = Modifier.weight(1f))
                BisqText.XSmallLight(
                    text = elapsed,
                    color = BisqTheme.colors.yellow10,
                )
            }
        }
        BisqGap.VHalf()
        BisqText.XSmallLight(
            text = "First launch over Tor typically takes 2-3 minutes.\nYour connection is encrypted and private.",
            color = BisqTheme.colors.yellow,
        )
    }
}

// ==================================================================================
// NODE VARIANT — step list
// ==================================================================================

/**
 * A single bootstrap step row for the Node app.
 *
 * Layout: [status dot | icon text] [label + detail]
 *
 * Status states:
 *   PENDING    — mid_grey10 dot, greyed label
 *   IN_PROGRESS — animated-style primary dot (pulsing is handled in production via
 *                 InfiniteTransition; in preview we use a brighter tint)
 *   DONE       — filled primary circle with check mark
 *   FAILED     — danger-colored circle with X
 */
@Composable
private fun NodeBootstrapStepRow(step: SimulatedStep) {
    val isActive = step.status == SimulatedStepStatus.IN_PROGRESS
    val isDone = step.status == SimulatedStepStatus.DONE
    val isFailed = step.status == SimulatedStepStatus.FAILED
    val isPending = step.status == SimulatedStepStatus.PENDING

    val dotColor =
        when {
            isDone -> BisqTheme.colors.primary
            isFailed -> BisqTheme.colors.danger
            isActive -> BisqTheme.colors.primaryHover
            else -> BisqTheme.colors.mid_grey10
        }

    val labelColor =
        when {
            isPending -> BisqTheme.colors.mid_grey10
            isFailed -> BisqTheme.colors.danger
            else -> BisqTheme.colors.white
        }

    val detailColor =
        when {
            isPending -> BisqTheme.colors.dark_grey50
            isFailed -> BisqTheme.colors.danger
            isActive -> BisqTheme.colors.primary65
            else -> BisqTheme.colors.mid_grey20
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = BisqUIConstants.ScreenPaddingHalf),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left status indicator
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isDone ->
                    // Dark glyph on green circle — high contrast against primary green.
                    // White would also pass WCAG AA on green, but dark is stronger here.
                    BisqText.XSmallMedium(
                        text = "✓",
                        color = BisqTheme.colors.backgroundColor,
                    )

                isFailed ->
                    // White glyph on red circle — high contrast against danger red.
                    // Different from the ✓ glyph color intentionally: each glyph is
                    // chosen to contrast against its own circle, not to be uniform
                    // with the other state. Dark on red would reduce contrast.
                    BisqText.XSmallMedium(
                        text = "✕",
                        color = Color.White,
                    )

                isActive ->
                    BisqText.XSmallLight(
                        text = step.icon,
                        color = BisqTheme.colors.backgroundColor,
                    )

                else ->
                    BisqText.XSmallLight(
                        text = step.icon,
                        color = BisqTheme.colors.mid_grey20,
                    )
            }
        }

        BisqGap.H2()

        Column(modifier = Modifier.weight(1f)) {
            BisqText.BaseRegular(
                text = step.label,
                color = labelColor,
            )
            if (step.detail.isNotEmpty()) {
                BisqGap.VQuarter()
                BisqText.XSmallLight(
                    text = step.detail,
                    color = detailColor,
                )
            }
        }

        // Right: in-progress spinner (simulated as a pulsing dot in preview)
        if (isActive) {
            BisqGap.H1()
            Box(
                modifier =
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(BisqTheme.colors.primary),
            )
        }
    }
}

@Composable
private fun NodeStepList(steps: List<SimulatedStep>) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey30)
                .padding(horizontal = BisqUIConstants.ScreenPadding, vertical = BisqUIConstants.ScreenPaddingHalf),
    ) {
        steps.forEachIndexed { index, step ->
            NodeBootstrapStepRow(step = step)
            if (index < steps.lastIndex) {
                // Connector line between steps
                Row {
                    Spacer(modifier = Modifier.width(13.dp)) // centre under 28dp dot
                    Box(
                        modifier =
                            Modifier
                                .width(2.dp)
                                .height(BisqUIConstants.ScreenPaddingHalf)
                                .background(BisqTheme.colors.dark_grey50),
                    )
                }
            }
        }
    }
}

// ==================================================================================
// CONNECT VARIANT — two-dot progress strip
// ==================================================================================

/**
 * Connect bootstrap: two phases only (Connect WS -> Load data).
 * Shown as a horizontal progress strip: dot + label, connector line, dot + label.
 * Avoids importing the "steps" mental model from Node when the work is much simpler.
 *
 * P1 — TIME-AWARE REASSURANCE LINE:
 * After 8-10 seconds with no phase transition, the detail text should update to:
 *   "Encrypted connections can take a moment to establish."
 * Rationale: honest about latency without naming Tor (which may not be in use). No
 * facade changes needed — the presenter drives this with a simple coroutine timer.
 * See Connect_SlowStart_Preview for the visual target.
 */
@Composable
private fun ConnectBootstrapStrip(
    connectingDone: Boolean,
    loadingDataActive: Boolean,
    loadingDataDone: Boolean,
    connectingDetail: String = "",
    loadingDetail: String = "",
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            ConnectPhaseNode(
                label = "Connect",
                done = connectingDone,
                active = !connectingDone && !loadingDataActive,
            )
            ConnectPhaseConnector(active = connectingDone)
            ConnectPhaseNode(
                label = "Load data",
                done = loadingDataDone,
                active = loadingDataActive,
            )
        }

        if (connectingDetail.isNotEmpty() || loadingDetail.isNotEmpty()) {
            BisqGap.V1()
            val activeDetail = if (connectingDone) loadingDetail else connectingDetail
            BisqText.SmallLight(
                text = activeDetail,
                color = BisqTheme.colors.mid_grey20,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ConnectPhaseNode(
    label: String,
    done: Boolean,
    active: Boolean,
) {
    val bgColor =
        when {
            done -> BisqTheme.colors.primary
            active -> BisqTheme.colors.primaryHover
            else -> BisqTheme.colors.dark_grey40
        }
    val textColor =
        when {
            done || active -> BisqTheme.colors.backgroundColor
            else -> BisqTheme.colors.mid_grey10
        }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(bgColor),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                BisqText.SmallMedium(text = "✓", color = textColor)
            } else {
                Box(
                    modifier =
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(textColor),
                )
            }
        }
        BisqGap.VQuarter()
        BisqText.XSmallLight(
            text = label,
            color =
                when {
                    done -> BisqTheme.colors.mid_grey20
                    active -> BisqTheme.colors.white
                    else -> BisqTheme.colors.mid_grey10
                },
        )
    }
}

@Composable
private fun ConnectPhaseConnector(active: Boolean) {
    Box(
        modifier =
            Modifier
                .width(60.dp)
                .padding(bottom = 18.dp) // offset to align with circle centres
                .height(2.dp)
                .background(if (active) BisqTheme.colors.primary else BisqTheme.colors.dark_grey40),
    )
}

// ==================================================================================
// CONNECT PREVIEWS
// ==================================================================================

@ExcludeFromCoverage
@Preview(name = "Connect 1: Initial — just paired, connecting WS")
@Composable
private fun Connect_Initial_Preview() {
    BisqTheme.Preview {
        BootstrapShell(
            title = "Connecting to your node",
            subtitle = "Bisq Connect links to your trusted Bisq2 node",
            overallProgress = 0.05f,
            appVersion = "Bisq Connect v0.5.0",
        ) {
            ConnectBootstrapStrip(
                connectingDone = false,
                loadingDataActive = false,
                loadingDataDone = false,
                connectingDetail = "Establishing encrypted connection...",
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "Connect 2: Loading data — WS connected, fetching offerbook/profile")
@Composable
private fun Connect_LoadingData_Preview() {
    BisqTheme.Preview {
        BootstrapShell(
            title = "Loading initial data",
            subtitle = "Fetching offers, market price and your profile",
            overallProgress = 0.6f,
            appVersion = "Bisq Connect v0.5.0",
        ) {
            ConnectBootstrapStrip(
                connectingDone = true,
                loadingDataActive = true,
                loadingDataDone = false,
                loadingDetail = "Fetching offers and market price...",
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "Connect 3: Done — about to navigate to home")
@Composable
private fun Connect_Done_Preview() {
    BisqTheme.Preview {
        BootstrapShell(
            title = "Ready",
            subtitle = "Connected to your trusted node",
            overallProgress = 1f,
            appVersion = "Bisq Connect v0.5.0",
        ) {
            ConnectBootstrapStrip(
                connectingDone = true,
                loadingDataActive = false,
                loadingDataDone = true,
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "Connect 4: Slow start — 8s+ stalled on phase 1, reassurance line shown")
@Composable
private fun Connect_SlowStart_Preview() {
    // P1: after ~8-10s with no phase transition the presenter swaps the detail text.
    // This preview shows the target visual. No UI structure changes — only the detail
    // string differs from Connect_Initial_Preview.
    BisqTheme.Preview {
        BootstrapShell(
            title = "Connecting to your node",
            subtitle = "Bisq Connect links to your trusted Bisq2 node",
            overallProgress = 0.05f,
            appVersion = "Bisq Connect v0.5.0",
        ) {
            ConnectBootstrapStrip(
                connectingDone = false,
                loadingDataActive = false,
                loadingDataDone = false,
                connectingDetail = "Encrypted connections can take a moment to establish.",
            )
        }
    }
}

// ==================================================================================
// NODE PREVIEWS
// ==================================================================================

private fun simulatedNodeSteps(
    torPercent: Int = 0,
    torDone: Boolean = false,
    torFailed: Boolean = false,
    peersActive: Boolean = false,
    peerCount: Int = 0,
    peerTarget: Int = 8,
    peersDone: Boolean = false,
    dataActive: Boolean = false,
    dataDone: Boolean = false,
    readyDone: Boolean = false,
): List<SimulatedStep> {
    val torStatus =
        when {
            torFailed -> SimulatedStepStatus.FAILED
            torDone -> SimulatedStepStatus.DONE
            torPercent > 0 -> SimulatedStepStatus.IN_PROGRESS
            else -> SimulatedStepStatus.PENDING
        }

    val torDetail =
        when {
            torFailed -> "Check your internet connection"
            torDone -> "Tor circuit established"
            torPercent > 0 -> "$torPercent% — building circuit"
            else -> ""
        }

    val peersStatus =
        when {
            peersDone -> SimulatedStepStatus.DONE
            peersActive -> SimulatedStepStatus.IN_PROGRESS
            else -> SimulatedStepStatus.PENDING
        }

    val peersDetail =
        when {
            peersDone -> "$peerCount peers connected"
            peersActive -> "$peerCount of $peerTarget peers connected"
            else -> ""
        }

    val dataStatus =
        when {
            dataDone -> SimulatedStepStatus.DONE
            dataActive -> SimulatedStepStatus.IN_PROGRESS
            else -> SimulatedStepStatus.PENDING
        }

    val dataDetail =
        when {
            dataDone -> "Data received"
            dataActive -> "Downloading offers and market prices"
            else -> ""
        }

    return listOf(
        SimulatedStep(
            icon = "T",
            label = if (torFailed) "Tor failed to start" else "Starting Tor",
            detail = torDetail,
            status = torStatus,
        ),
        SimulatedStep(
            icon = "P",
            label = "Discovering peers",
            detail = peersDetail,
            status = peersStatus,
        ),
        SimulatedStep(
            icon = "D",
            label = "Syncing data",
            detail = dataDetail,
            status = dataStatus,
        ),
        SimulatedStep(
            icon = "★",
            label = "Ready",
            detail = "",
            status = if (readyDone) SimulatedStepStatus.DONE else SimulatedStepStatus.PENDING,
        ),
    )
}

@ExcludeFromCoverage
@Preview(name = "Node 1: Tor bootstrap ~50%")
@Composable
private fun Node_TorBootstrap_Preview() {
    BisqTheme.Preview {
        BootstrapShell(
            title = "Starting Bisq Node",
            subtitle = "Connecting to the Bisq P2P network via Tor",
            overallProgress = 0.15f,
            appVersion = "Bisq Easy v0.5.0",
        ) {
            NodeStepList(
                steps =
                    simulatedNodeSteps(
                        torPercent = 50,
                    ),
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "Node 2: P2P peer discovery (Tor done, finding peers)")
@Composable
private fun Node_PeerDiscovery_Preview() {
    BisqTheme.Preview {
        BootstrapShell(
            title = "Discovering peers",
            subtitle = "Connecting to the Bisq P2P network via Tor",
            overallProgress = 0.45f,
            appVersion = "Bisq Easy v0.5.0",
        ) {
            NodeStepList(
                steps =
                    simulatedNodeSteps(
                        torPercent = 100,
                        torDone = true,
                        peersActive = true,
                        peerCount = 3,
                        peerTarget = 8,
                    ),
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "Node 3: Data inventory sync (peers found, loading offerbook)")
@Composable
private fun Node_DataSync_Preview() {
    BisqTheme.Preview {
        BootstrapShell(
            title = "Syncing network data",
            subtitle = "Downloading offers and market prices",
            overallProgress = 0.75f,
            appVersion = "Bisq Easy v0.5.0",
        ) {
            NodeStepList(
                steps =
                    simulatedNodeSteps(
                        torPercent = 100,
                        torDone = true,
                        peersDone = true,
                        peerCount = 8,
                        dataActive = true,
                    ),
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "Node 4: Ready / transitioning out")
@Composable
private fun Node_Ready_Preview() {
    BisqTheme.Preview {
        BootstrapShell(
            title = "Ready",
            subtitle = "Bisq Node is connected and synced",
            overallProgress = 1f,
            appVersion = "Bisq Easy v0.5.0",
        ) {
            NodeStepList(
                steps =
                    simulatedNodeSteps(
                        torPercent = 100,
                        torDone = true,
                        peersDone = true,
                        peerCount = 8,
                        dataDone = true,
                        readyDone = true,
                    ),
            )
        }
    }
}

// ==================================================================================
// SLOW-PATH EDGE CASE — 75+ second mark
// ==================================================================================

@ExcludeFromCoverage
// Banner triggers at 75s wall-clock from bootstrap start — 15s before the existing
// 90s timeout dialog — so the user receives a reassurance message before the harder
// interrupt. Wall-clock elapsed, NOT per-stage elapsed (see STATE-TRANSITION CONTRACT).
@Preview(name = "Slow path — 75s mark, Node still discovering peers")
@Composable
private fun SlowPath_Preview() {
    BisqTheme.Preview {
        BootstrapShell(
            title = "Discovering peers",
            subtitle = "Connecting to the Bisq P2P network via Tor",
            overallProgress = 0.35f,
            appVersion = "Bisq Easy v0.5.0",
            slowPathBannerVisible = true,
            slowPathElapsed = "76s elapsed",
        ) {
            NodeStepList(
                steps =
                    simulatedNodeSteps(
                        torPercent = 100,
                        torDone = true,
                        peersActive = true,
                        peerCount = 1,
                        peerTarget = 8,
                    ),
            )
        }
    }
}

// ==================================================================================
// FAILURE / RETRY
// ==================================================================================

/**
 * Tor failure state. The failed step row is shown in danger color, and the Tor
 * retry/purge actions appear below the step list — surfaced inline so the user
 * sees the failed step and the action in one glance before the modal dialog appears.
 *
 * The two buttons map to the existing SplashUiAction.OnRestartTor and
 * SplashUiAction.OnPurgeRestartTor. The existing WarningConfirmationDialog is still
 * triggered by torBootstrapFailed = true; these inline buttons serve as a secondary
 * affordance for users who tap away the dialog and wonder why the screen is stuck.
 */
@Composable
private fun TorFailureActions(
    onRestartTor: () -> Unit,
    onPurgeRestart: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BisqGap.V2()
        BisqButton(
            text = "Restart Tor",
            onClick = onRestartTor,
            fullWidth = true,
            type = BisqButtonType.Outline,
        )
        BisqGap.V1()
        BisqButton(
            text = "Clear Tor data and restart",
            onClick = onPurgeRestart,
            fullWidth = true,
            type = BisqButtonType.Danger,
        )
        BisqGap.VHalf()
        BisqText.XSmallLight(
            text = "Clearing Tor data takes longer but often resolves persistent failures.",
            color = BisqTheme.colors.mid_grey20,
            textAlign = TextAlign.Center,
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Failure / retry — Tor failed to start")
@Composable
private fun Failure_TorFailed_Preview() {
    BisqTheme.Preview {
        BootstrapShell(
            title = "Tor failed to start",
            subtitle = "Please check your internet connection",
            overallProgress = 0.08f,
            appVersion = "Bisq Easy v0.5.0",
        ) {
            NodeStepList(
                steps =
                    simulatedNodeSteps(
                        torFailed = true,
                    ),
            )
            TorFailureActions(
                onRestartTor = {},
                onPurgeRestart = {},
            )
        }
    }
}
