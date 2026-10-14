/**
 * DeviceSecurityDesign.kt
 *
 * Compose specification for the rooted / jailbroken device warning: a dismissible bottom sheet
 * shown on arrival at the dashboard while the device is detected as rooted (Android) or jailbroken
 * (iOS), and a Settings row that reopens it. Not wired to a presenter; the previews in
 * `DeviceSecurityPreviews.kt` build every state from `simulatedXxx` helpers that take primitives.
 *
 * ------------------------------------------------------------------------------------
 * 1. PURPOSE AND POLICY
 * ------------------------------------------------------------------------------------
 * One honest heads-up, never a gate. Warn only: no feature is blocked, hidden or degraded on a
 * rooted device. Detection is local heuristics only (file probes and build tags on Android, known
 * paths and a sandbox-write probe on iOS); no attestation service, no telemetry. The detection
 * result is never added to analytics events or crash-report tags, opted in or not. The Android
 * emulator reports as rooted and shows the warning; that is intended and is not special-cased.
 *
 * Copy always says "appears to be" and "can let"; it never says "unsafe", "compromised" or "is
 * rooted", because the result is a heuristic.
 *
 * ------------------------------------------------------------------------------------
 * 2. SHAPE: OWN BOTTOM SHEET, SHOWN BEFORE THE CAROUSEL
 * ------------------------------------------------------------------------------------
 * Rule: the warning is its own bottom sheet ([DeviceSecurityWarningSheet]), shown ahead of the
 * welcome carousel; it is not a carousel page.
 *
 * Why:
 * - The carousel has no plain dismissal. It is a centred modal card whose only exits are the
 *   primary action and "Don't ask again". A warning page inside it would either be inescapable
 *   or force the permanent decision on every user who just wants to move on. Warn-only needs an
 *   exit that costs nothing and decides nothing.
 * - The carousel is an opt-in wizard (step dots, one decision per card). A warning is not an
 *   opt-in; putting it in the wizard would present it as "step 1 of N" of setup.
 * - The warning must be reopenable from Settings. A standalone sheet is reusable from there; a
 *   carousel page is not, because the carousel is bound to the dashboard and to pending opt-ins.
 * - The shared molecule `BisqBottomSheet` already provides swipe, scrim and back dismissal and a
 *   scrollable body for large fonts.
 *
 * What is reused from the carousel: the arrival rule (pending on every arrival until an explicit
 * decision) and the idiom for the decision (a full-width primary button with a centred
 * `BisqButtonType.Clear` button below it, one tap, no confirmation dialog).
 *
 * ------------------------------------------------------------------------------------
 * 3. PERSISTED FIELDS AND DETECTION
 * ------------------------------------------------------------------------------------
 * Two new fields in `Settings`, both default to the first value:
 * - `deviceSecurityBaseline: DeviceSecurityBaseline` = `UNSET | CLEAN | ROOTED`. The detection
 *   result observed on the first dashboard arrival ever (or the first after upgrade). Written once,
 *   never overwritten. It drives the wording only.
 * - `deviceSecurityWarningSuppressed: Boolean` = false. True after the explicit "Don't show
 *   again" decision.
 *
 * Detection runs on every dashboard arrival (cheap, deterministic probes) and its latest result is
 * held in memory for the Settings row. It is never persisted other than through the two fields
 * above. [resolveDeviceSecurity] is the single pure function that turns (detection, baseline,
 * suppressed) into the new persisted values and what to show; the presenter calls it once per
 * arrival and writes the returned baseline and suppressed values.
 *
 * Existing installs: on the first arrival after the update the baseline is `UNSET`, so it is
 * recorded from that arrival. A device that was already rooted gets the standard wording (we
 * cannot know the state at setup, and the honest message is that this is the first time we tell
 * them).
 *
 * ------------------------------------------------------------------------------------
 * 4. WHEN THE WARNING SHOWS
 * ------------------------------------------------------------------------------------
 * "Arrival" means the dashboard entering composition, the same trigger the carousel uses.
 *
 * - Detection clean: nothing shows, no Settings row. The suppressed flag is cleared (re-arm, see
 *   below).
 * - Detection rooted and not suppressed: the warning is pending and shows on every arrival until
 *   the user taps "Don't show again".
 * - Detection rooted and suppressed: nothing shows on the dashboard; the Settings row remains.
 * - Demo mode: nothing shows (same gate as the carousel).
 *
 * A plain dismissal (swipe, scrim, back, "Got it") only sets an in-memory `closedThisArrival`
 * flag, reset when the dashboard leaves composition. It persists nothing, so the warning returns on
 * the next arrival.
 *
 * Rule: a CLEAN observation clears `deviceSecurityWarningSuppressed`, so a later CLEAN to ROOTED
 * change shows the warning again with the changed wording, even if the user chose "Don't show
 * again" during an earlier rooted period.
 * Why: "Don't show again" is a decision about a condition that persists. A clean observation ends
 * that condition, and a new rooting is new information (a ROM flash, a recovery tool, someone else
 * handling the device), which the earlier decision never covered. The cost of being wrong is one
 * extra dismissal; the cost of staying silent is a user who rooted again without knowing the app
 * exposes data. The clear only happens on an arrival where detection is clean, so a user who stays
 * rooted keeps their decision indefinitely.
 * TODO: the probes are deterministic, so no debounce is specified. If a custom ROM is found to
 * flicker between clean and rooted, require a clean result on two consecutive arrivals before
 * clearing the flag, so a flickering device does not override an explicit decision.
 *
 * Wording: [DeviceSecurityWording.CHANGED_SINCE_SETUP] iff the baseline is `CLEAN` and the device
 * is now rooted; otherwise [DeviceSecurityWording.FIRST_DETECTION]. The changed wording is an extra
 * lead paragraph above the standard body, and stays in use on every showing until the user decides,
 * because the baseline never changes.
 *
 * ------------------------------------------------------------------------------------
 * 5. ORDERING WITH THE WELCOME CAROUSEL
 * ------------------------------------------------------------------------------------
 * [resolveDashboardOverlay] decides which single overlay the dashboard renders. Only one overlay
 * is ever on screen.
 *
 * - While the warning is pending and not closed this arrival, the warning is shown and the
 *   carousel is not composed, even if carousel pages are pending.
 * - When the user dismisses the warning without deciding (`closedThisArrival = true`), the carousel
 *   shows in the same arrival if it has pending pages. Rule: a plain dismissal must never withhold
 *   the carousel. Why: a rooted user who never taps "Don't show again" would otherwise have the
 *   warning pending on every arrival, and the carousel (notifications, battery, analytics) would
 *   never reach them.
 * - When the user taps "Don't show again", the warning is no longer pending and the carousel
 *   follows immediately in the same arrival.
 * - When the warning is not pending, behaviour is exactly today's carousel behaviour.
 * - Returning to the dashboard later in the same process counts as a new arrival: the warning
 *   shows again if still pending, then the carousel.
 * - The Settings reopen path involves no carousel; the sheet is shown over Settings only.
 *
 * ------------------------------------------------------------------------------------
 * 6. ACTIONS
 * ------------------------------------------------------------------------------------
 * - [DeviceSecurityUiAction.OnDismiss]: swipe, scrim, back and the "Got it" button. Sets
 *   `closedThisArrival` (dashboard) or closes the sheet (Settings). Persists nothing.
 * - [DeviceSecurityUiAction.OnDontShowAgain]: sets `deviceSecurityWarningSuppressed = true`, closes
 *   the sheet and shows a snackbar (`mobile.deviceSecurity.dontShowAgain.confirmation`) that tells
 *   the user where the warning can still be found. Not shown if already suppressed.
 * - [DeviceSecurityUiAction.OnOpenFromSettings]: tap on the Settings row. Opens the sheet with
 *   `origin = SETTINGS`.
 * - The existing Settings action that resets all "Don't show again" flags also sets
 *   `deviceSecurityWarningSuppressed = false`.
 *
 * ------------------------------------------------------------------------------------
 * 7. BEHAVIOUR TABLE
 * ------------------------------------------------------------------------------------
 * | Detection | Baseline before | Suppressed before | Baseline after | Suppressed after | Dashboard | Row |
 * |-----------|-----------------|-------------------|----------------|------------------|-----------|-----|
 * | clean     | UNSET           | any               | CLEAN          | false            | nothing   | no  |
 * | clean     | CLEAN / ROOTED  | any               | unchanged      | false            | nothing   | no  |
 * | rooted    | UNSET           | any               | ROOTED         | unchanged        | warning, standard wording unless suppressed | yes |
 * | rooted    | ROOTED          | false             | ROOTED         | false            | warning, standard wording | yes |
 * | rooted    | CLEAN           | false             | CLEAN          | false            | warning, changed wording  | yes |
 * | rooted    | ROOTED / CLEAN  | true              | unchanged      | true             | nothing   | yes |
 *
 * Actions on the dashboard sheet:
 * | Action          | Persists                         | Result                                        |
 * |-----------------|----------------------------------|-----------------------------------------------|
 * | Got it          | nothing                          | sheet closes, carousel follows this arrival   |
 * | swipe/scrim/back| nothing                          | same as Got it                                |
 * | Don't show again| suppressed = true                | sheet closes, snackbar, carousel follows      |
 *
 * Actions on the Settings sheet:
 * | Action          | Persists                         | Result                                        |
 * |-----------------|----------------------------------|-----------------------------------------------|
 * | Got it / swipe  | nothing                          | sheet closes, user stays on Settings          |
 * | Don't show again| suppressed = true (if not set)   | sheet closes, snackbar                        |
 *
 * ------------------------------------------------------------------------------------
 * 8. SHEET ANATOMY
 * ------------------------------------------------------------------------------------
 * Top to bottom, centred like the carousel card: a circular icon holder with a warning glyph in
 * `BisqTheme.colors.warning` (the only use of that colour; it is for the icon, never for text),
 * the title as an accessibility heading, a scrollable body, the full-width "Got it" button, and
 * below it the centred Clear-type "Don't show again" button.
 *
 * - Node and Connect share layout and buttons and differ in the body text only: node names
 *   identity keys and trade history and recommends avoiding untrusted apps; Connect names the
 *   pairing, chats and payment details and offers unpairing from the node.
 * - Android and iOS differ only in the word rooted / jailbroken (title and Connect body).
 * - The Node app exists on Android only, so node plus iOS is not a valid combination.
 * - From Settings the sheet is identical, except the secondary action is replaced by a caption
 *   when the decision was already made ([DeviceSecurityUiState.isSuppressed]).
 * - The body scrolls inside the sheet and the buttons stay pinned below it, so long translations
 *   (German and Russian run 30 to 40 percent longer) and 200 percent font scale never push the
 *   buttons off screen. Buttons keep a minimum height of 48dp.
 * - Accessibility: the title carries the heading role and is the first focusable element, so the
 *   screen reader announces it on open. The icon is decorative. No information is conveyed by
 *   colour alone.
 *
 * ------------------------------------------------------------------------------------
 * 9. SETTINGS ROW
 * ------------------------------------------------------------------------------------
 * [DeviceSecuritySettingsRow] exists only while detection is positive, regardless of the
 * suppressed flag. It is a tappable row with a neutral shield icon, the label "Device security"
 * and the subtitle "This device appears to be rooted" / "jailbroken", and a chevron. It is
 * placed at the top of the Settings screen, above the language section, without a section header
 * (the row is the header). Why top: it reports the state of the device, not a preference, and a
 * one-time sheet is easy to forget, so the row must be found without scrolling. A clean device
 * gets no row and no positive "device is secure" state anywhere, because the heuristics are
 * evadable and have false positives.
 *
 * ------------------------------------------------------------------------------------
 * 10. I18N KEYS (English base, to add to mobile.properties at implementation time)
 * ------------------------------------------------------------------------------------
 * - mobile.deviceSecurity.title.rooted = This device appears to be rooted
 * - mobile.deviceSecurity.title.jailbroken = This device appears to be jailbroken
 * - mobile.deviceSecurity.changedLead = This device's security status has changed since you set up Bisq.
 * - mobile.deviceSecurity.body.node.rooted = Rooting can let other apps on this device access
 *   Bisq's data, including your identity keys and trade history. Bisq works normally, and the
 *   choice is yours. For best protection, avoid installing untrusted apps on this device.
 * - mobile.deviceSecurity.body.connect.rooted = Rooting can let other apps on this device access
 *   Bisq's data, including your pairing with your node, chats and payment details. Bisq works
 *   normally, and the choice is yours. If you suspect a problem, you can unpair from your node.
 * - mobile.deviceSecurity.body.connect.jailbroken = Jailbreaking can let other apps on this device
 *   access Bisq's data, including your pairing with your node, chats and payment details. Bisq
 *   works normally, and the choice is yours. If you suspect a problem, you can unpair from your
 *   node.
 * - mobile.deviceSecurity.gotIt = Got it
 * - mobile.deviceSecurity.dontShowAgain = Don't show again
 * - mobile.deviceSecurity.dontShowAgain.confirmation = We won't show this at startup again. You
 *   can still find it in Settings under Device security.
 * - mobile.deviceSecurity.suppressedNote = You chose not to see this warning at startup. Resetting
 *   all 'Don't show again' flags in Settings turns it back on.
 * - mobile.deviceSecurity.settings.label = Device security
 * - mobile.deviceSecurity.settings.subtitle.rooted = This device appears to be rooted
 * - mobile.deviceSecurity.settings.subtitle.jailbroken = This device appears to be jailbroken
 *
 * The changed wording is a separate lead paragraph rather than a rewritten first sentence, so each
 * translated string stays a complete unit and no sentences are concatenated mid-paragraph.
 * The previews use literal English strings of exactly these keys; the implementation resolves them
 * through `.i18n()`.
 *
 * ------------------------------------------------------------------------------------
 * 11. TEST HOOKS
 * ------------------------------------------------------------------------------------
 * `device_security_sheet`, `device_security_title`, `device_security_body`,
 * `device_security_got_it`, `device_security_dont_show_again`, `device_security_suppressed_note`,
 * `device_security_settings_row`.
 */
package network.bisq.mobile.presentation.design.device_security

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

// -------------------------------------------------------------------------------------
// Model
// -------------------------------------------------------------------------------------

/** Which app the warning renders in. Selects the body text. */
internal enum class DeviceSecurityApp { NODE, CONNECT }

/** Selects rooted or jailbroken wording. */
internal enum class DeviceSecurityPlatform { ANDROID, IOS }

/** Where the sheet was opened from. */
internal enum class DeviceSecurityOrigin { DASHBOARD, SETTINGS }

/** Detection result recorded on the first dashboard arrival. Persisted. */
internal enum class DeviceSecurityBaseline { UNSET, CLEAN, ROOTED }

/** Whether the lead paragraph announcing a change since setup is shown. */
internal enum class DeviceSecurityWording { FIRST_DETECTION, CHANGED_SINCE_SETUP }

/** The single overlay the dashboard renders at a time. */
internal enum class DashboardOverlay { NONE, DEVICE_SECURITY_WARNING, WELCOME_CAROUSEL }

/**
 * State of the warning sheet. [isVisible] is true only while the sheet should be on screen:
 * pending and not closed this arrival on the dashboard, or opened from the Settings row.
 * [isSuppressed] mirrors the persisted "Don't show again" decision and only changes what the
 * Settings-origin sheet offers.
 */
internal data class DeviceSecurityUiState(
    val isVisible: Boolean = false,
    val app: DeviceSecurityApp = DeviceSecurityApp.CONNECT,
    val platform: DeviceSecurityPlatform = DeviceSecurityPlatform.ANDROID,
    val wording: DeviceSecurityWording = DeviceSecurityWording.FIRST_DETECTION,
    val origin: DeviceSecurityOrigin = DeviceSecurityOrigin.DASHBOARD,
    val isSuppressed: Boolean = false,
)

/** State of the Settings row. [isVisible] is true only while detection is positive. */
internal data class DeviceSecurityRowUiState(
    val isVisible: Boolean = false,
    val platform: DeviceSecurityPlatform = DeviceSecurityPlatform.ANDROID,
)

internal sealed interface DeviceSecurityUiAction {
    /** Swipe, scrim, back and "Got it". Persists nothing. */
    data object OnDismiss : DeviceSecurityUiAction

    /** Explicit, persisted decision. Sets the suppressed flag. */
    data object OnDontShowAgain : DeviceSecurityUiAction

    /** Tap on the Settings row. */
    data object OnOpenFromSettings : DeviceSecurityUiAction
}

/**
 * Outcome of one arrival. [baseline] and [suppressed] are the values to persist;
 * [wording] is null when the device is clean.
 */
internal data class DeviceSecurityResolution(
    val baseline: DeviceSecurityBaseline,
    val suppressed: Boolean,
    val isPending: Boolean,
    val wording: DeviceSecurityWording?,
    val isRowVisible: Boolean,
)

/** See sections 3 and 4 of the file KDoc. */
internal fun resolveDeviceSecurity(
    isRooted: Boolean,
    baseline: DeviceSecurityBaseline,
    suppressed: Boolean,
): DeviceSecurityResolution {
    val newBaseline =
        when {
            baseline != DeviceSecurityBaseline.UNSET -> baseline
            isRooted -> DeviceSecurityBaseline.ROOTED
            else -> DeviceSecurityBaseline.CLEAN
        }
    if (!isRooted) {
        return DeviceSecurityResolution(
            baseline = newBaseline,
            suppressed = false,
            isPending = false,
            wording = null,
            isRowVisible = false,
        )
    }
    return DeviceSecurityResolution(
        baseline = newBaseline,
        suppressed = suppressed,
        isPending = !suppressed,
        wording =
            if (newBaseline == DeviceSecurityBaseline.CLEAN) {
                DeviceSecurityWording.CHANGED_SINCE_SETUP
            } else {
                DeviceSecurityWording.FIRST_DETECTION
            },
        isRowVisible = true,
    )
}

/** See section 5 of the file KDoc. */
internal fun resolveDashboardOverlay(
    isWarningPending: Boolean,
    isWarningClosedThisArrival: Boolean,
    hasCarouselPages: Boolean,
): DashboardOverlay =
    when {
        isWarningPending && !isWarningClosedThisArrival -> DashboardOverlay.DEVICE_SECURITY_WARNING
        hasCarouselPages -> DashboardOverlay.WELCOME_CAROUSEL
        else -> DashboardOverlay.NONE
    }

// -------------------------------------------------------------------------------------
// Copy (literal English until the keys in section 10 exist)
// -------------------------------------------------------------------------------------

internal data class DeviceSecurityCopy(
    val title: String,
    val changedLead: String?,
    val body: String,
    val gotIt: String,
    val dontShowAgain: String?,
    val suppressedNote: String?,
)

// TODO: replace literals with `.i18n()` lookups once the keys in section 10 are added to
// mobile.properties; the previews cannot resolve keys that do not exist yet.
internal fun deviceSecurityCopy(uiState: DeviceSecurityUiState): DeviceSecurityCopy {
    val isIos = uiState.platform == DeviceSecurityPlatform.IOS
    val isNode = uiState.app == DeviceSecurityApp.NODE
    val connectConsequence =
        "including your pairing with your node, chats and payment details. Bisq works normally, " +
            "and the choice is yours. If you suspect a problem, you can unpair from your node."
    val body =
        when {
            isNode ->
                "Rooting can let other apps on this device access Bisq's data, including your " +
                    "identity keys and trade history. Bisq works normally, and the choice is " +
                    "yours. For best protection, avoid installing untrusted apps on this device."

            isIos -> "Jailbreaking can let other apps on this device access Bisq's data, $connectConsequence"
            else -> "Rooting can let other apps on this device access Bisq's data, $connectConsequence"
        }
    val fromSettings = uiState.origin == DeviceSecurityOrigin.SETTINGS
    return DeviceSecurityCopy(
        title = if (isIos) "This device appears to be jailbroken" else "This device appears to be rooted",
        changedLead =
            if (uiState.wording == DeviceSecurityWording.CHANGED_SINCE_SETUP) {
                "This device's security status has changed since you set up Bisq."
            } else {
                null
            },
        body = body,
        gotIt = "Got it",
        dontShowAgain = if (fromSettings && uiState.isSuppressed) null else "Don't show again",
        suppressedNote =
            if (fromSettings && uiState.isSuppressed) {
                "You chose not to see this warning at startup. Resetting all 'Don't show again' " +
                    "flags in Settings turns it back on."
            } else {
                null
            },
    )
}

// -------------------------------------------------------------------------------------
// Warning sheet
// -------------------------------------------------------------------------------------

/** The warning bottom sheet. Swipe, scrim and back all route to [DeviceSecurityUiAction.OnDismiss]. */
@Composable
internal fun DeviceSecurityWarningSheet(
    uiState: DeviceSecurityUiState,
    onAction: (DeviceSecurityUiAction) -> Unit,
) {
    if (!uiState.isVisible) return
    BisqBottomSheet(onDismissRequest = { onAction(DeviceSecurityUiAction.OnDismiss) }) {
        DeviceSecurityWarningContent(
            copy = deviceSecurityCopy(uiState),
            onGotIt = { onAction(DeviceSecurityUiAction.OnDismiss) },
            onDontShowAgain = { onAction(DeviceSecurityUiAction.OnDontShowAgain) },
        )
    }
}

/** Sheet content, separated so previews can feed it arbitrary (for example very long) copy. */
@Composable
internal fun DeviceSecurityWarningContent(
    copy: DeviceSecurityCopy,
    onGotIt: () -> Unit,
    onDontShowAgain: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = BisqUIConstants.ScreenPadding2X)
                .testTag("device_security_sheet"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(BisqUIConstants.ScreenPadding4X)
                    .clip(CircleShape)
                    .background(BisqTheme.colors.warning.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = BisqTheme.colors.warning,
                modifier = Modifier.size(BisqUIConstants.ScreenPadding2X),
            )
        }

        BisqGap.V2()

        BisqText.H5Light(
            text = copy.title,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .semantics { heading() }
                    .testTag("device_security_title"),
        )

        BisqGap.V1()

        Column(
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .testTag("device_security_body"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (copy.changedLead != null) {
                BisqText.BaseRegular(
                    text = copy.changedLead,
                    textAlign = TextAlign.Center,
                )
                BisqGap.V1()
            }
            BisqText.BaseLightGrey(
                text = copy.body,
                textAlign = TextAlign.Center,
            )
        }

        BisqGap.V3()

        BisqButton(
            text = copy.gotIt,
            onClick = onGotIt,
            fullWidth = true,
            modifier =
                Modifier
                    .heightIn(min = BisqUIConstants.ScreenPadding4X)
                    .testTag("device_security_got_it"),
        )

        if (copy.dontShowAgain != null) {
            BisqGap.V1()
            BisqButton(
                text = copy.dontShowAgain,
                onClick = onDontShowAgain,
                type = BisqButtonType.Clear,
                modifier =
                    Modifier
                        .heightIn(min = BisqUIConstants.ScreenPadding4X)
                        .testTag("device_security_dont_show_again"),
            )
        }

        if (copy.suppressedNote != null) {
            BisqGap.V1()
            BisqText.SmallLightGrey(
                text = copy.suppressedNote,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("device_security_suppressed_note"),
            )
        }

        BisqGap.V1()
    }
}

// -------------------------------------------------------------------------------------
// Settings row
// -------------------------------------------------------------------------------------

/** Row shown at the top of Settings while detection is positive. Tapping reopens the sheet. */
@Composable
internal fun DeviceSecuritySettingsRow(
    rowState: DeviceSecurityRowUiState,
    onAction: (DeviceSecurityUiAction) -> Unit,
) {
    if (!rowState.isVisible) return
    val subtitle =
        if (rowState.platform == DeviceSecurityPlatform.IOS) {
            "This device appears to be jailbroken"
        } else {
            "This device appears to be rooted"
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = BisqUIConstants.ScreenPadding4X)
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .clickable(role = Role.Button) { onAction(DeviceSecurityUiAction.OnOpenFromSettings) }
                .padding(vertical = BisqUIConstants.ScreenPaddingHalf)
                .testTag("device_security_settings_row"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = BisqTheme.colors.light_grey20,
            modifier = Modifier.size(BisqUIConstants.ScreenPadding2X),
        )
        BisqGap.H2()
        Column(modifier = Modifier.weight(1f)) {
            BisqText.BaseRegular(text = "Device security")
            BisqText.SmallLightGrey(text = subtitle)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = BisqTheme.colors.mid_grey20,
        )
    }
}
