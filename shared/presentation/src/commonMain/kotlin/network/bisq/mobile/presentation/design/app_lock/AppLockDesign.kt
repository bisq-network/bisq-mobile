/**
 * AppLockDesign.kt
 *
 * Compose specification for the app lock: a platform-biometric / device-credential gate that hides
 * app content on cold start and on return from background after a grace period. Not wired to a
 * presenter; state shown in the previews below is produced entirely by the `simulatedXxx` helpers,
 * which take only primitives.
 *
 * ------------------------------------------------------------------------------------
 * 1. PURPOSE
 * ------------------------------------------------------------------------------------
 * A UI gate only: it hides content, it does not wrap key material. The node's foreground service
 * and Tor, and Connect's WebSocket, keep running behind it, so background notifications are
 * unaffected. No app-owned PIN — both platforms already give "biometric or device credential"
 * through `BiometricPrompt` (Android) and `LocalAuthentication` (iOS), so a custom PIN would only
 * add attack surface and a recovery flow for no benefit.
 *
 * This also fixes the policy model and the step-up primitive that a later milestone reuses once a
 * Connect pairing can authorise spending: the same `Required` policy state and the same
 * [StepUpGate] composable guard a spend request then, so both are designed now even though 1.0.0
 * only reaches `Off` and `Enabled`.
 *
 * ------------------------------------------------------------------------------------
 * 2. POLICY MODEL
 * ------------------------------------------------------------------------------------
 * [AppLockPolicy] is `Off | Enabled | Required(reason)`. Only `Off` and `Enabled` are reachable in
 * 1.0.0; `Required` is designed now with generic copy ("Required while this device can authorise
 * spending") so the control and its explanation exist before the MuSig milestone needs them.
 *
 * The effective-override rule mirrors `AnimationSettings`
 * (`shared/presentation/.../common/ui/animation/AnimationSettings.kt`): `Required` and
 * "disabled by device" (no biometrics/credential enrolled) are both *effective* overrides on top
 * of the stored user preference, never a mutation of it. If a device later gains an enrolled
 * credential, or a spend-capable permission is later revoked, the previous preference re-applies
 * unchanged rather than needing to be re-entered. [AppLockControlState] is the derived, three-way
 * render state (`Editable`, `DisabledByDevice`, `RequiredByPolicy`) a settings screen reads —
 * analogous to `isUseAnimationsChangeEnabled` plus `AnimationSettings.lockedByDevice`, but kept as
 * one enum here because a third state (`RequiredByPolicy`) exists that animations does not have.
 *
 * ------------------------------------------------------------------------------------
 * 3. TRIGGERS
 * ------------------------------------------------------------------------------------
 * Two triggers set `isLocked = true`:
 * - Cold start, unconditionally when the policy is not `Off`. `ForegroundDetector.isForeground`
 *   (`shared/domain/.../data/service/AppForegroundController.kt`) cannot be used to derive this:
 *   Android's actual/expect starts it at `false`, iOS starts it at `true`, so branching on the flag
 *   at process start would lock on Android and not lock on iOS for the exact same event. Cold start
 *   must be its own explicit check, run once at startup, independent of that flag.
 * - Backgrounding for longer than the chosen [GracePeriod]. `ForegroundDetector` uses
 *   started/stopped counting on Android (so a transient pause — a permission dialog, a share sheet —
 *   does not flip it) and did-enter-background on iOS (so a control-centre peek does not flip it),
 *   which makes it the right signal to key off; no "time since backgrounded" timestamp exists
 *   anywhere in the codebase today, so it is new state, written once on the foreground→background
 *   transition and compared against on the next foreground→true transition.
 * - Testable clock: reuse the `currentTimeMillis()` idiom from
 *   `ApplicationBootstrapFacade.currentTimeMillis()` (`shared/domain/.../data/service/bootstrap/
 *   ApplicationBootstrapFacade.kt:63`, `protected open fun currentTimeMillis(): Long =
 *   DateUtils.now()`) — a small overridable seam rather than a direct system-clock call, so a grace
 *   period test can fake elapsed time deterministically instead of sleeping.
 *
 * ------------------------------------------------------------------------------------
 * 4. LOCK SCREEN RULES
 * ------------------------------------------------------------------------------------
 * [AppLockScreen] shows nothing of app content: an opaque, branded screen, not a blur. A blurred
 * screen still leaks shapes and rough text length (offer amounts, a trade counterparty's presence)
 * at a glance; opaque is the only treatment that fully satisfies "hides app content." The secure-
 * screen treatment (`SecureScreenEffect`, Android `FLAG_SECURE`, iOS app-switcher blur — already
 * used on 13 sensitive screens) applies app-wide while locked, for the same reason: the app-switcher
 * thumbnail is itself a place content could leak even though the lock screen composable is correct.
 *
 * The OS prompt opens automatically on lock (`phase = Prompting` immediately, not `Idle` first) so
 * the common path costs zero extra taps; `Idle` exists for the case where the OS prompt has been
 * dismissed or has not been re-triggered yet (e.g. after a failed attempt the user did not retry).
 * Failed attempts, lockout and cooldown are entirely the OS prompt's own UI and its own timing — no
 * custom attempt counter is kept here, matching "no app-owned PIN": building a parallel lockout
 * mechanism would duplicate exactly the state the platform primitive already owns safely. If
 * biometrics were removed at the OS level between sessions, the fallback to device credential is
 * silent — this is a normal OS event, not a security incident worth a scary interstitial.
 *
 * The lock screen consumes all touches and the back gesture (`BackHandler`,
 * `shared/presentation/.../common/ui/components/BackHandler.kt`, an expect fun with no
 * unconditional-consume call site today). Android back on the lock screen moves the app to the
 * background rather than closing app content — `moveAppToBackground` already exists on
 * `PlatformPresentationAbstractions` (`shared/presentation/.../common/ui/platform/
 * PlatformPresentationAbstractions.kt`) — so back behaves like leaving a locked device rather than
 * like an in-app "cancel."
 *
 * A content description states the app is locked and how to unlock (see "12. Accessibility").
 *
 * ------------------------------------------------------------------------------------
 * 5. OVERLAY PRECEDENCE
 * ------------------------------------------------------------------------------------
 * `App.kt` renders, inside `SafeInsetsContainer`'s Box: `Column { NetworkStatusBanner();
 * AlertNotificationBanner(); navGraphContent() }`, then `GenericErrorOverlay()`, then the
 * connections-lost dialog or `ReconnectingOverlay`, then `LoadingOverlay`, then
 * `AlertNotificationDialog`, then `BisqSnackbar`. [AppLockScreen] is inserted as a new sibling
 * right after that `Column` and before `GenericErrorOverlay()` — this is the dividing line the
 * stacking rules below actually need, not simply "last in the Box":
 * - Hidden while locked: the `Column`'s two banners (covered — opaque lock renders above them),
 *   the connections-lost `WarningConfirmationDialog` (its trigger is gated on `!isLocked` so it
 *   does not fire visibly at all while locked, deferred until unlock rather than rendered behind
 *   an overlay that could itself leak the dialog's text), and the real `ReconnectingOverlay` (while
 *   locked, `showReconnectOverlay` instead feeds `AppLockScreen`'s own
 *   `showConnectReconnectingIndicator`, a small in-lock-screen indicator with no trade or account
 *   text — see "1. Lock screen — Connect reconnecting").
 * - Still shown above the lock: `GenericErrorOverlay` and `LoadingOverlay`, because both are
 *   content-free (a generic crash/error affordance and a blocking spinner) — hiding either behind
 *   the unlock gate would strand the user looking at a stuck screen with no feedback, which is a
 *   worse failure than briefly showing a content-free overlay before authentication.
 * - `AlertNotificationDialog` is deferred until unlock, exactly like the connections-lost dialog:
 *   a signed alert's text can be trade- or user-specific, and a dialog that opens over the lock
 *   would be the first thing a bystander reads. `BisqSnackbar` is suppressed while locked for the
 *   same reason; a snackbar raised during the locked window is dropped, not queued, because its
 *   message describes a moment that has passed by the time the user authenticates.
 *
 * On the node, `AppLockScreen` renders on top of a *completed* splash/Tor-bootstrap route, never
 * during it — the bootstrap screen is a route, not an overlay in this Box, so this falls out of
 * ordering rather than needing a special case: the lock only starts rendering once the app has
 * navigated past bootstrap into the tab shell.
 *
 * ------------------------------------------------------------------------------------
 * 6. ENROLMENT
 * ------------------------------------------------------------------------------------
 * Opt-in only, never an onboarding interstitial: forcing a biometric-enrolment decision before the
 * user has seen any value in the app is a known abandonment point. [AppLockEnrolmentCard] appears
 * on the dashboard after profile creation, the same "dismissible card among other dashboard cards"
 * pattern `DashBoardCard`/`HomeInfoCard` already establish
 * (`shared/presentation/.../tabs/dashboard/DashboardScreen.kt`). Dismissing the card is permanent —
 * a persisted flag, not a session flag — because the plan is Settings-is-the-long-term-entry, and a
 * card that keeps reappearing after an explicit "not now" reproduces exactly the nagging the
 * dismissed-offending-offers banner rule elsewhere in this design system exists to avoid. The
 * Settings "App lock" section (see "7. Settings section") is always reachable regardless of the
 * card's dismissal state.
 *
 * ------------------------------------------------------------------------------------
 * 7. SETTINGS SECTION
 * ------------------------------------------------------------------------------------
 * [AppLockSettingsSection] is a new headed block in `SettingsScreen.kt`, positioned as the display
 * section's neighbour (`settings.display.headline`, `SettingsScreen.kt` lines 255-274) since both
 * are device-local, UI-only preferences — not synced to bisq2 settings the way trade preferences
 * are. Its toggle reuses `BisqSwitch`'s `disabled` + `onDisabledTap` contract exactly as the
 * animations toggle does (`SettingsScreen.kt` lines 259-265): `disabled` is true whenever
 * [AppLockControlState] is not `Editable`, and `onDisabledTap` shows the explanation — a snackbar
 * for `DisabledByDevice` (routing to OS settings), inline caption text for `RequiredByPolicy`
 * (nothing to tap through to, since there is no user action that changes a required policy in
 * 1.0.0).
 *
 * The switch itself renders three visual states, one more than the animations toggle's two: on/
 * editable, off/editable, and greyed-on for both `DisabledByDevice` (paradoxically shown as *off*
 * and greyed, since there is nothing to protect if no credential exists) and `RequiredByPolicy`
 * (shown as *on* and greyed, since the policy forces protection regardless of what the user would
 * otherwise choose).
 *
 * The grace-period row only renders once the lock is effectively on (`Enabled` or `Required`) and
 * a device credential exists — showing a grace picker for a lock that cannot currently engage would
 * be confusing chrome. Three options only — Immediately / 1 minute / 5 minutes — because this is a
 * once-per-install decision, not a power-user knob; more options would just be more UI for a choice
 * most users make once and never revisit. Default 1 minute: immediate defeats casual re-opens
 * (checking a notification, glancing at the offerbook) and 5+ minutes defeats the point of the
 * feature on a device that gets picked up constantly. The row opens [GracePeriodPickerSheet], a
 * `BisqBottomSheet` (`shared/presentation/.../common/ui/components/molecules/bottom_sheet/
 * BottomSheet.kt`) with one row per option and a short description each — a sheet rather than a
 * `BisqSelect` dropdown because three short, mutually exclusive physical choices read better as
 * a native picker sheet than as a searchable dropdown built for long option lists (`BisqSelect`'s
 * own `searchable` flag signals it is meant for that longer-list case, e.g. language).
 *
 * ------------------------------------------------------------------------------------
 * 8. STEP-UP PRIMITIVE AND ITS CALL SITES
 * ------------------------------------------------------------------------------------
 * [StepUpGate] is one reusable composable, not one bespoke dialog per screen: it is a fresh
 * device-auth check at action time, independent of whether the app lock is currently on or
 * currently unlocked — the app may already be unlocked (cold start happened minutes ago) when the
 * user reaches a step-up-guarded screen, so step-up cannot piggyback on lock state. It renders as a
 * blocking card over the target screen (own small state: `Gate → Prompting → Failed` until
 * `Granted`, at which point it stops rendering and the screen underneath is fully interactive) —
 * see "4. Step-up" previews for the before/after pairing per call site.
 *
 * Call sites in scope, each independent, each supplying its own `actionLabel` copy for the gate's
 * message:
 * - Node backup view/export: `BackupPresenter` (`apps/nodeApp/.../settings/backup/presentation/
 *   BackupPresenter.kt`) already gates `OnBackupToFile` behind a password dialog and a working
 *   dialog (`BackupScreen.kt`); step-up wraps entry into that flow, one prompt before the password
 *   dialog opens, not one per password character or per retry.
 * - Node restore: the same presenter's restore action currently has no gate beyond the archive's
 *   own encryption password — step-up adds the missing first check, since restoring silently
 *   overwrites local state.
 * - Connect trusted-node pairing: `TrustedNodeSetupPresenter`
 *   (`apps/clientApp/.../trusted_node_setup/TrustedNodeSetupPresenter.kt`) — pairing to a node is
 *   itself a credential-adjacent action (the pairing code can carry spend-capable permissions once
 *   MuSig lands), so it is gated even before that milestone ships.
 * - Payment accounts: shared `PaymentAccountsPresenter`
 *   (`shared/presentation/.../settings/payment_accounts/PaymentAccountsPresenter.kt`) and Connect's
 *   MuSig account presenters, `PaymentAccountsMusigPresenter` and
 *   `PaymentAccountMusigDetailPresenter` (`apps/clientApp/.../payment_accounts/presentation/...`).
 *   The Connect MuSig screens (`PaymentAccountsMusigScreen.kt`,
 *   `PaymentAccountMusigDetailScreen.kt`) are also the one gap in `SecureScreenEffect`'s otherwise
 *   13-call-site coverage of sensitive screens — they gain the effect at the same time step-up is
 *   wired in, not as a separate follow-up, since both are "this screen is more sensitive than the
 *   rest of Payment Accounts" fixes to the same two files.
 *
 * Explicitly out of scope for this plan: wiring the actual spend step-up prompt before a MuSig
 * transaction is signed — see "11. Out of scope."
 *
 * ------------------------------------------------------------------------------------
 * 9. PLATFORM PREREQUISITES
 * ------------------------------------------------------------------------------------
 * Neither app currently declares a biometric dependency or permission — grepping the codebase for
 * `androidx.biometric`, `USE_BIOMETRIC` and `NSFaceIDUsageDescription` returns nothing. Production
 * wiring needs: the `androidx.biometric:biometric` dependency and the `USE_BIOMETRIC` manifest
 * permission on Android; an `NSFaceIDUsageDescription` Info.plist entry on iOS (Face ID requires an
 * explicit usage string the way camera/location do; Touch ID and device-passcode fallback do not).
 * The prompt itself follows the established expect/actual shape for an OS prompt —
 * `PermissionRequestLauncher` / `rememberNotificationPermissionLauncher`
 * (`shared/presentation/.../common/ui/utils/PermissionRequestLauncher.kt`) — as a
 * `rememberDeviceAuthPrompt`-style expect fun returning a launcher the lock screen and each step-up
 * call site both use; iOS `LocalAuthentication` is callable directly from Kotlin/Native, no Swift
 * bridge needed, unlike some other iOS platform calls in this codebase.
 *
 * ------------------------------------------------------------------------------------
 * 10. PERSISTENCE
 * ------------------------------------------------------------------------------------
 * Device-local, not synced: unlike the trade-preference settings that round-trip through bisq2 and
 * back to the node on Connect, the lock is meaningless to share across devices (a stolen phone
 * should not inherit whatever a different device decided). It belongs in the same place the other
 * device-local toggles already live — the DataStore `Settings` model
 * (`shared/domain/.../data/model/Settings.kt`) and `SettingsRepositoryImpl`
 * (`shared/domain/.../data/repository/SettingsRepositoryImpl.kt`), alongside
 * `keepConnectedInBackground` and `analyticsEnabled`. New fields there are forward- and
 * backward-safe: the DataStore model already tolerates unknown/missing keys
 * (`ignoreUnknownKeys`), so an older build reading a newer store, or a newer build reading an older
 * store, both fall back to the field default rather than failing to parse.
 *
 * ------------------------------------------------------------------------------------
 * 11. OUT OF SCOPE (WITH HOOKS)
 * ------------------------------------------------------------------------------------
 * - Key binding: making the lock cryptographically meaningful (an unlock actually required to use a
 *   key), rather than a UI gate. The hooks already exist and are unused today: on Android, the
 *   keystore key is currently created with `setUserAuthenticationRequired(false)`; on iOS, the
 *   keychain item has no access control set. Both would need to change for key binding, which is
 *   why they are named here rather than left implicit.
 * - An app-owned PIN — see "1. Purpose" for why this is deliberately not built.
 * - Spend step-up wiring — the *mechanism* ([StepUpGate]) is designed now (see "8"), but calling it
 *   before an actual MuSig spend is a separate milestone's work: that milestone also has to decide
 *   whether the node should demand its own per-request proof, which is a bisq2 API question outside
 *   this design's scope.
 *
 * ------------------------------------------------------------------------------------
 * 12. ACCESSIBILITY
 * ------------------------------------------------------------------------------------
 * - [AppLockScreen]'s root carries a content description stating the app is locked and how to
 *   unlock ("App locked. Double-tap Unlock to continue.") since a screen reader user otherwise has
 *   nothing else on screen to announce.
 * - [StepUpGate] carries its own content description distinct from the lock screen's, naming the
 *   specific action being protected (from `actionLabel`), so a screen reader user does not have to
 *   guess whether this is the app lock again or a different, narrower check.
 * - `Modifier.testTag(...)` marks the lock screen root, its unlock button, the enrolment card's
 *   enable/dismiss controls, the settings toggle, the grace-period row and sheet options, and the
 *   step-up gate's continue/retry/cancel controls, per repo convention — no `semantics {}` block is
 *   used as a test-id substitute.
 * - Long/localized copy: the lock screen's message area and the settings section's caption text
 *   wrap rather than truncate, since German/Russian run 30-40% longer — see the long-locale
 *   previews.
 *
 * ------------------------------------------------------------------------------------
 * 13. PROPOSED I18N KEYS
 * ------------------------------------------------------------------------------------
 * English base values only, per repo convention — nothing is added to `mobile.properties` by this
 * file; production implementation adds these.
 *
 *   mobile.appLock.settings.headline = "App lock"
 *   mobile.appLock.settings.toggleLabel = "Lock with biometrics or device credential"
 *   mobile.appLock.settings.disabledByDevice.caption = "No biometrics or screen lock set up on
 *     this device."
 *   mobile.appLock.settings.disabledByDevice.openSettings = "Open device settings"
 *   mobile.appLock.settings.requiredByPolicy.reason = "Required while this device can authorise
 *     spending"
 *   mobile.appLock.settings.gracePeriod.label = "Lock after"
 *   mobile.appLock.settings.gracePeriod.immediately = "Immediately"
 *   mobile.appLock.settings.gracePeriod.oneMinute = "1 minute"
 *   mobile.appLock.settings.gracePeriod.fiveMinutes = "5 minutes"
 *   mobile.appLock.settings.gracePeriod.immediately.help = "Locks the instant you leave the app"
 *   mobile.appLock.settings.gracePeriod.oneMinute.help = "Locks one minute after you leave"
 *   mobile.appLock.settings.gracePeriod.fiveMinutes.help = "Locks five minutes after you leave"
 *   mobile.appLock.lockScreen.unlock = "Unlock"
 *   mobile.appLock.lockScreen.prompting = "Waiting for biometric or device credential…"
 *   mobile.appLock.lockScreen.failed = "That didn't match. Try again."
 *   mobile.appLock.lockScreen.lockedOut = "Too many attempts. Try again in a moment, or use your
 *     device credential."
 *   mobile.appLock.lockScreen.fallbackToCredential = "Biometrics aren't set up on this device.
 *     Use your device credential instead."
 *   mobile.appLock.lockScreen.reconnecting = "Reconnecting to your node…"
 *   mobile.appLock.lockScreen.contentDescription = "App locked. Double-tap Unlock to continue."
 *   mobile.appLock.enrolment.headline = "Lock the app when you're not using it"
 *   mobile.appLock.enrolment.body = "Use your device's biometrics or screen lock to hide Bisq's
 *     content when you switch away."
 *   mobile.appLock.enrolment.enable = "Enable app lock"
 *   mobile.appLock.enrolment.dismiss = "Not now"
 *   mobile.appLock.stepUp.headline = "Confirm it's you"
 *   mobile.appLock.stepUp.message = "This screen needs a fresh check, even if the app is already
 *     unlocked."
 *   mobile.appLock.stepUp.continue = "Continue"
 *   mobile.appLock.stepUp.cancel = "Cancel"
 *   mobile.appLock.stepUp.failed = "That didn't match. Try again."
 *   mobile.appLock.stepUp.action.backupView = "before viewing your backup"
 *   mobile.appLock.stepUp.action.backupExport = "before exporting your backup"
 *   mobile.appLock.stepUp.action.restore = "before restoring from a backup"
 *   mobile.appLock.stepUp.action.pairing = "before pairing with a trusted node"
 *   mobile.appLock.stepUp.action.paymentAccounts = "before opening your payment accounts"
 *
 * ------------------------------------------------------------------------------------
 * 14. TESTS TO ADD
 * ------------------------------------------------------------------------------------
 * Mirroring the fixtures already listed in the plan's codebase facts:
 * - Policy/effective-state test mirroring `AnimationSettingsTest`: `Required` overrides the stored
 *   preference without mutating it; `DisabledByDevice` likewise; both clear when their condition
 *   clears, restoring the previous preference unchanged.
 * - `DeviceInfoProviderTest`-style test for "no credential enrolled" detection feeding
 *   `AppLockControlState.DisabledByDevice`.
 * - `SettingsPresenterTest` locked-device-fixture-style test: the toggle's `disabled`/
 *   `onDisabledTap` wiring for all three [AppLockControlState] values.
 * - `SecureScreenEffectUiTest`-style test asserting the effect is active app-wide while locked, and
 *   newly active on the two Connect MuSig payment-account screens.
 * - `SettingsRepositoryImplTest` / `DataStoreMigrationTest`-style test: new lock fields round-trip
 *   and default correctly when absent from an older stored record.
 * - `FakeForegroundDetector`-style test: cold start locks regardless of the platform's initial
 *   `isForeground` value; backgrounding past the grace period locks; backgrounding under the grace
 *   period does not.
 * - `MainActivityDeepLinkTest`-style test: a deep link received while locked routes through unlock
 *   first, then resumes navigation to the original destination.
 * - Step-up test per call site: [StepUpGate] blocks the underlying screen until `Granted`; a
 *   cancelled or failed step-up leaves the screen non-interactive; a step-up already granted this
 *   process does not re-prompt (or does, per the call-site's own freshness rule — confirm per call
 *   site during implementation, since "fresh check" (`8`) implies no caching by default).
 *
 * ------------------------------------------------------------------------------------
 * 15. IMPLEMENTATION NOTES FOR THE DEVELOPER
 * ------------------------------------------------------------------------------------
 * - `shared/presentation/.../main/App.kt`: insert `if (isLocked) AppLockScreen(...)` as a new
 *   sibling inside `SafeInsetsContainer`'s Box, after the `SwipeBackIOSNavigationHandler` block
 *   (which contains the `Column` of banners + nav content) and before `GenericErrorOverlay()` —
 *   see "5. Overlay precedence" for exactly why that position, not simply "last."
 * - New `AppLockPresenter`/use-case owns `isLocked`, the cold-start check, the grace-period
 *   comparison (using the `currentTimeMillis()` idiom, see "3. Triggers") and reads
 *   `ForegroundDetector.isForeground` — wire it the same two-place way as `AnimationSettings`:
 *   `single { AppLockPolicyProvider(get(), get(), ...) }` in both `ClientPresentationModule.kt:31`
 *   and `NodePresentationModule.kt:49`'s neighbourhood, since both apps need the policy but the
 *   node additionally needs the future MuSig-permission-flip hook.
 * - `SettingsRepositoryImpl.kt` / `Settings.kt`: add `appLockEnabled: Boolean = false`,
 *   `appLockGracePeriod: GracePeriod = GracePeriod.ONE_MINUTE`,
 *   `appLockEnrolmentCardDismissed: Boolean = false` fields — see "10. Persistence."
 * - `rememberDeviceAuthPrompt` expect/actual: Android side backed by `androidx.biometric
 *   .BiometricPrompt`; iOS side backed by `LocalAuthentication.LAContext` — see "9. Platform
 *   prerequisites" for the manifest/Info.plist prerequisites.
 * - `BackupPresenter.kt` (`apps/nodeApp/...`), `TrustedNodeSetupPresenter.kt`
 *   (`apps/clientApp/...`), `PaymentAccountsPresenter.kt` (`shared/presentation/...`),
 *   `PaymentAccountsMusigPresenter.kt` and `PaymentAccountMusigDetailPresenter.kt`
 *   (`apps/clientApp/...`): each gains a `stepUpUiState`/`StepUpUiAction` pair and dispatches
 *   through the shared [StepUpGate] rather than a bespoke dialog — see "8."
 * - `PaymentAccountsMusigScreen.kt` and `PaymentAccountMusigDetailScreen.kt`: add
 *   `SecureScreenEffect()` at the top of each screen composable, matching every other sensitive
 *   screen's own first line (e.g. `BackupScreen.kt`'s `SecureScreenEffect()` right after
 *   `RememberPresenterLifecycle(presenter)`).
 * - `SettingsScreen.kt`: insert `AppLockSettingsSection` as a new block after the display section
 *   (`settings.display.headline`, ends at `BisqHDivider()` following the "reset don't-show-again"
 *   button, `SettingsScreen.kt` lines ~267-276) — see "7."
 * - `DashboardScreen.kt`: insert [AppLockEnrolmentCard] conditionally on
 *   `!appLockEnrolmentCardDismissed && policy == Off`, positioned among the existing dashboard
 *   cards (`DashBoardCard`/`HomeInfoCard`) — see "6."
 */
package network.bisq.mobile.presentation.design.app_lock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqCard
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqSwitch
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// -------------------------------------------------------------------------------------
// Policy model — see "2. Policy model"
// -------------------------------------------------------------------------------------

/** `off | enabled | required(reason)` — see "2. Policy model" for the effective-override rule. */
internal sealed interface AppLockPolicy {
    data object Off : AppLockPolicy

    data object Enabled : AppLockPolicy

    data class Required(
        val reason: String,
    ) : AppLockPolicy
}

/** Derived, three-way render state a settings screen reads — see "2. Policy model." */
internal enum class AppLockControlState {
    EDITABLE,
    DISABLED_BY_DEVICE,
    REQUIRED_BY_POLICY,
}

internal enum class GracePeriod {
    IMMEDIATELY,
    ONE_MINUTE,
    FIVE_MINUTES,
}

private fun GracePeriod.label(): String =
    when (this) {
        GracePeriod.IMMEDIATELY -> "Immediately"
        GracePeriod.ONE_MINUTE -> "1 minute"
        GracePeriod.FIVE_MINUTES -> "5 minutes"
    }

private fun GracePeriod.helpText(): String =
    when (this) {
        GracePeriod.IMMEDIATELY -> "Locks the instant you leave the app"
        GracePeriod.ONE_MINUTE -> "Locks one minute after you leave"
        GracePeriod.FIVE_MINUTES -> "Locks five minutes after you leave"
    }

internal enum class AppVariant { NODE, CONNECT }

// -------------------------------------------------------------------------------------
// Lock screen — see "4. Lock screen rules"
// -------------------------------------------------------------------------------------

internal enum class LockScreenPhase {
    IDLE,
    PROMPTING,
    FAILED,
    LOCKED_OUT,
    FALLBACK_TO_CREDENTIAL,
}

internal data class AppLockScreenUiState(
    val phase: LockScreenPhase = LockScreenPhase.PROMPTING,
    val appVariant: AppVariant = AppVariant.NODE,
    val showConnectReconnectingIndicator: Boolean = false,
)

internal sealed interface AppLockScreenUiAction {
    data object OnUnlockTap : AppLockScreenUiAction
}

/** Builds an [AppLockScreenUiState] from primitives with realistic defaults. */
internal fun simulatedAppLockScreenUiState(
    phase: LockScreenPhase = LockScreenPhase.PROMPTING,
    appVariant: AppVariant = AppVariant.NODE,
    showConnectReconnectingIndicator: Boolean = false,
): AppLockScreenUiState = AppLockScreenUiState(phase, appVariant, showConnectReconnectingIndicator)

@Composable
internal fun AppLockScreen(
    uiState: AppLockScreenUiState,
    onAction: (AppLockScreenUiAction) -> Unit,
    appName: String = if (uiState.appVariant == AppVariant.NODE) "Bisq Easy" else "Bisq Connect",
    unlockLabel: String = "Unlock",
    promptingLabel: String = "Waiting for biometric or device credential…",
    failedMessage: String = "That didn't match. Try again.",
    lockedOutMessage: String = "Too many attempts. Try again in a moment, or use your device credential.",
    fallbackMessage: String = "Biometrics aren't set up on this device. Use your device credential instead.",
    reconnectingLabel: String = "Reconnecting to your node…",
    contentDescription: String = "App locked. Double-tap Unlock to continue.",
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BisqTheme.colors.backgroundColor)
                // Consumes every touch and would consume the back gesture in production — see "4."
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { /* consume touches */ }
                .semantics { this.contentDescription = contentDescription }
                .testTag("app_lock_screen"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding4X),
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = BisqTheme.colors.primary,
                modifier = Modifier.size(64.dp),
            )
            BisqGap.V2()
            BisqText.H3Light(text = appName, color = BisqTheme.colors.white, textAlign = TextAlign.Center)
            BisqGap.V4()

            when (uiState.phase) {
                LockScreenPhase.IDLE ->
                    BisqButton(
                        text = unlockLabel,
                        fullWidth = true,
                        onClick = { onAction(AppLockScreenUiAction.OnUnlockTap) },
                        modifier = Modifier.testTag("app_lock_unlock_button"),
                    )

                LockScreenPhase.PROMPTING -> {
                    CircularProgressIndicator(color = BisqTheme.colors.primary, modifier = Modifier.size(40.dp))
                    BisqGap.V1()
                    BisqText.BaseLight(text = promptingLabel, color = BisqTheme.colors.light_grey50, textAlign = TextAlign.Center)
                }

                LockScreenPhase.FAILED -> {
                    BisqText.BaseLight(text = failedMessage, color = BisqTheme.colors.danger, textAlign = TextAlign.Center)
                    BisqGap.V2()
                    BisqButton(
                        text = unlockLabel,
                        fullWidth = true,
                        onClick = { onAction(AppLockScreenUiAction.OnUnlockTap) },
                        modifier = Modifier.testTag("app_lock_unlock_button"),
                    )
                }

                LockScreenPhase.LOCKED_OUT -> {
                    BisqText.BaseLight(text = lockedOutMessage, color = BisqTheme.colors.warning, textAlign = TextAlign.Center)
                    BisqGap.V2()
                    BisqButton(
                        text = unlockLabel,
                        fullWidth = true,
                        onClick = { onAction(AppLockScreenUiAction.OnUnlockTap) },
                        modifier = Modifier.testTag("app_lock_unlock_button"),
                    )
                }

                LockScreenPhase.FALLBACK_TO_CREDENTIAL -> {
                    BisqText.BaseLight(text = fallbackMessage, color = BisqTheme.colors.light_grey50, textAlign = TextAlign.Center)
                    BisqGap.V2()
                    BisqButton(
                        text = "Use device credential",
                        fullWidth = true,
                        onClick = { onAction(AppLockScreenUiAction.OnUnlockTap) },
                        modifier = Modifier.testTag("app_lock_unlock_button"),
                    )
                }
            }

            if (uiState.showConnectReconnectingIndicator) {
                BisqGap.V2()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = BisqTheme.colors.mid_grey20,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                    BisqGap.HHalf()
                    BisqText.SmallLight(text = reconnectingLabel, color = BisqTheme.colors.mid_grey20)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// Dashboard enrolment card — see "6. Enrolment"
// -------------------------------------------------------------------------------------

internal sealed interface AppLockEnrolmentUiAction {
    data object OnEnable : AppLockEnrolmentUiAction

    data object OnDismiss : AppLockEnrolmentUiAction
}

@Composable
internal fun AppLockEnrolmentCard(
    onAction: (AppLockEnrolmentUiAction) -> Unit,
    headline: String = "Lock the app when you're not using it",
    body: String = "Use your device's biometrics or screen lock to hide Bisq's content when you switch away.",
    enableLabel: String = "Enable app lock",
    dismissLabel: String = "Not now",
) {
    BisqCard(
        padding = BisqUIConstants.ScreenPadding2X,
        modifier = Modifier.testTag("app_lock_enrolment_card"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = BisqTheme.colors.primary,
                    modifier = Modifier.size(24.dp),
                )
                BisqText.H4Light(text = headline, color = BisqTheme.colors.white)
            }
            IconButton(
                onClick = { onAction(AppLockEnrolmentUiAction.OnDismiss) },
                modifier = Modifier.testTag("app_lock_enrolment_dismiss_icon"),
            ) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = dismissLabel, tint = BisqTheme.colors.mid_grey20)
            }
        }

        BisqGap.V1()
        BisqText.SmallLight(text = body, color = BisqTheme.colors.mid_grey20)
        BisqGap.V2()

        BisqButton(
            text = enableLabel,
            fullWidth = true,
            onClick = { onAction(AppLockEnrolmentUiAction.OnEnable) },
            modifier = Modifier.testTag("app_lock_enrolment_enable_button"),
        )
        BisqGap.VHalf()
        BisqButton(
            text = dismissLabel,
            type = BisqButtonType.Clear,
            fullWidth = true,
            onClick = { onAction(AppLockEnrolmentUiAction.OnDismiss) },
            modifier = Modifier.testTag("app_lock_enrolment_dismiss_button"),
        )
    }
}

/** Lightweight reproduction of a dashboard card neighbour, for "in context" previews. */
@Composable
private fun SimulatedMarketPriceCard() {
    BisqCard {
        BisqText.H5Light("Market price")
        BisqGap.V1()
        BisqText.BaseLight("111247.40 BTC/USD", color = BisqTheme.colors.mid_grey20)
    }
}

// -------------------------------------------------------------------------------------
// Settings section — see "7. Settings section"
// -------------------------------------------------------------------------------------

internal data class AppLockSettingsUiState(
    val controlState: AppLockControlState = AppLockControlState.EDITABLE,
    val enabled: Boolean = false,
    val gracePeriod: GracePeriod = GracePeriod.ONE_MINUTE,
    val requiredReason: String = "Required while this device can authorise spending",
    val isGracePickerVisible: Boolean = false,
)

internal sealed interface AppLockSettingsUiAction {
    data class OnToggle(
        val enabled: Boolean,
    ) : AppLockSettingsUiAction

    data object OnDisabledTap : AppLockSettingsUiAction

    data object OnOpenGracePicker : AppLockSettingsUiAction

    data object OnDismissGracePicker : AppLockSettingsUiAction

    data class OnGracePeriodSelect(
        val gracePeriod: GracePeriod,
    ) : AppLockSettingsUiAction

    data object OnOpenDeviceCredentialSettings : AppLockSettingsUiAction
}

/** Builds an [AppLockSettingsUiState] from primitives with realistic defaults. */
internal fun simulatedAppLockSettingsUiState(
    controlState: AppLockControlState = AppLockControlState.EDITABLE,
    enabled: Boolean = false,
    gracePeriod: GracePeriod = GracePeriod.ONE_MINUTE,
    requiredReason: String = "Required while this device can authorise spending",
    isGracePickerVisible: Boolean = false,
): AppLockSettingsUiState = AppLockSettingsUiState(controlState, enabled, gracePeriod, requiredReason, isGracePickerVisible)

@Composable
internal fun AppLockSettingsSection(
    uiState: AppLockSettingsUiState,
    onAction: (AppLockSettingsUiAction) -> Unit,
    headline: String = "App lock",
    toggleLabel: String = "Lock with biometrics or device credential",
    disabledByDeviceCaption: String = "No biometrics or screen lock set up on this device.",
    disabledByDeviceLink: String = "Open device settings",
    graceRowLabel: String = "Lock after",
) {
    val isSwitchOn = uiState.enabled || uiState.controlState == AppLockControlState.REQUIRED_BY_POLICY
    val showGraceRow =
        uiState.controlState != AppLockControlState.DISABLED_BY_DEVICE &&
            (uiState.enabled || uiState.controlState == AppLockControlState.REQUIRED_BY_POLICY)

    Column(modifier = Modifier.testTag("app_lock_settings_section")) {
        BisqText.H4Light(headline)
        BisqGap.V1()

        BisqSwitch(
            label = toggleLabel,
            checked = isSwitchOn,
            disabled = uiState.controlState != AppLockControlState.EDITABLE,
            onSwitch = { onAction(AppLockSettingsUiAction.OnToggle(it)) },
            onDisabledTap = { onAction(AppLockSettingsUiAction.OnDisabledTap) },
        )

        when (uiState.controlState) {
            AppLockControlState.DISABLED_BY_DEVICE -> {
                BisqGap.VQuarter()
                BisqText.SmallLight(text = disabledByDeviceCaption, color = BisqTheme.colors.mid_grey20)
                BisqGap.VQuarter()
                BisqButton(
                    text = disabledByDeviceLink,
                    type = BisqButtonType.Underline,
                    onClick = { onAction(AppLockSettingsUiAction.OnOpenDeviceCredentialSettings) },
                    modifier = Modifier.testTag("app_lock_open_device_settings_link"),
                )
            }

            AppLockControlState.REQUIRED_BY_POLICY -> {
                BisqGap.VQuarter()
                BisqText.SmallLight(text = uiState.requiredReason, color = BisqTheme.colors.warning)
            }

            AppLockControlState.EDITABLE -> Unit
        }

        if (showGraceRow) {
            BisqGap.V1()
            GracePeriodRow(
                label = graceRowLabel,
                selected = uiState.gracePeriod,
                onClick = { onAction(AppLockSettingsUiAction.OnOpenGracePicker) },
            )
        }

        if (uiState.isGracePickerVisible) {
            GracePeriodPickerSheet(
                selected = uiState.gracePeriod,
                onSelect = { onAction(AppLockSettingsUiAction.OnGracePeriodSelect(it)) },
                onDismiss = { onAction(AppLockSettingsUiAction.OnDismissGracePicker) },
            )
        }
    }
}

@Composable
private fun GracePeriodRow(
    label: String,
    selected: GracePeriod,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = BisqUIConstants.ScreenPaddingHalf)
                .testTag("app_lock_grace_period_row"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BisqText.BaseLight(label)
        BisqText.BaseLight(text = "${selected.label()}  ›", color = BisqTheme.colors.primary)
    }
}

@Composable
internal fun GracePeriodPickerSheet(
    selected: GracePeriod,
    onSelect: (GracePeriod) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Lock after",
) {
    BisqBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding2X)) {
            BisqText.H4Light(title)
            BisqGap.V2()
            GracePeriod.entries.forEach { option ->
                GracePeriodOptionRow(
                    option = option,
                    isSelected = option == selected,
                    onClick = { onSelect(option) },
                )
                BisqGap.V1()
            }
        }
    }
}

@Composable
private fun GracePeriodOptionRow(
    option: GracePeriod,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = BisqUIConstants.ScreenPaddingHalf)
                .testTag("app_lock_grace_period_option_${option.name}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) BisqTheme.colors.primary else BisqTheme.colors.dark_grey30),
        )
        BisqGap.H1()
        Column {
            BisqText.BaseLight(option.label())
            BisqText.SmallLight(text = option.helpText(), color = BisqTheme.colors.mid_grey20)
        }
    }
}

// -------------------------------------------------------------------------------------
// Step-up primitive and its call sites — see "8. Step-up primitive and its call sites"
// -------------------------------------------------------------------------------------

internal enum class StepUpPhase {
    GATE,
    PROMPTING,
    FAILED,
    GRANTED,
}

internal data class StepUpUiState(
    val phase: StepUpPhase = StepUpPhase.GATE,
)

internal sealed interface StepUpUiAction {
    data object OnTrigger : StepUpUiAction

    data object OnRetry : StepUpUiAction

    data object OnCancel : StepUpUiAction
}

/** Builds a [StepUpUiState] from primitives with realistic defaults. */
internal fun simulatedStepUpUiState(phase: StepUpPhase = StepUpPhase.GATE): StepUpUiState = StepUpUiState(phase)

/**
 * Reusable step-up gate — renders above the target screen until [StepUpUiState.phase] reaches
 * [StepUpPhase.GRANTED], then renders nothing, letting the screen underneath show through fully
 * interactive. [actionContext] names the specific action being protected — see "12. Accessibility."
 */
@Composable
internal fun StepUpGate(
    uiState: StepUpUiState,
    onAction: (StepUpUiAction) -> Unit,
    actionContext: String,
    headline: String = "Confirm it's you",
    message: String = "This screen needs a fresh check, even if the app is already unlocked.",
    continueLabel: String = "Continue",
    cancelLabel: String = "Cancel",
    failedMessage: String = "That didn't match. Try again.",
) {
    if (uiState.phase == StepUpPhase.GRANTED) return

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BisqTheme.colors.backgroundColor.copy(alpha = 0.85f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { /* consume touches to the screen underneath */ }
                .semantics { contentDescription = "Confirm it's you, $actionContext." }
                .testTag("step_up_gate"),
        contentAlignment = Alignment.Center,
    ) {
        BisqCard(
            padding = BisqUIConstants.ScreenPadding2X,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding2X),
        ) {
            Icon(
                imageVector = Icons.Filled.Fingerprint,
                contentDescription = null,
                tint = BisqTheme.colors.primary,
                modifier = Modifier.size(40.dp),
            )
            BisqGap.V1()
            BisqText.H4Regular(text = headline, color = BisqTheme.colors.white, textAlign = TextAlign.Center)
            BisqGap.VHalf()
            BisqText.SmallLight(
                text = "$message ($actionContext)",
                color = BisqTheme.colors.mid_grey20,
                textAlign = TextAlign.Center,
            )
            BisqGap.V2()

            when (uiState.phase) {
                StepUpPhase.GATE ->
                    BisqButton(
                        text = continueLabel,
                        fullWidth = true,
                        onClick = { onAction(StepUpUiAction.OnTrigger) },
                        modifier = Modifier.testTag("step_up_continue_button"),
                    )

                StepUpPhase.PROMPTING ->
                    CircularProgressIndicator(color = BisqTheme.colors.primary, modifier = Modifier.size(32.dp))

                StepUpPhase.FAILED -> {
                    BisqText.SmallLight(text = failedMessage, color = BisqTheme.colors.danger, textAlign = TextAlign.Center)
                    BisqGap.V1()
                    BisqButton(
                        text = continueLabel,
                        fullWidth = true,
                        onClick = { onAction(StepUpUiAction.OnRetry) },
                        modifier = Modifier.testTag("step_up_retry_button"),
                    )
                }

                StepUpPhase.GRANTED -> Unit
            }

            BisqGap.VHalf()
            BisqButton(
                text = cancelLabel,
                type = BisqButtonType.Clear,
                fullWidth = true,
                onClick = { onAction(StepUpUiAction.OnCancel) },
                modifier = Modifier.testTag("step_up_cancel_button"),
            )
        }
    }
}

// Lightweight mocks of the guarded screens, for "before/after" previews only — see "8."

@Composable
private fun SimulatedBackupScreenMock() {
    Column(modifier = Modifier.fillMaxWidth().padding(BisqUIConstants.ScreenPadding2X)) {
        BisqText.H4Light("Backup & restore")
        BisqGap.V1()
        BisqText.SmallLight("Export an encrypted copy of your data.", color = BisqTheme.colors.mid_grey20)
        BisqGap.V1()
        BisqButton(text = "Back up now", type = BisqButtonType.Outline, fullWidth = true, onClick = {})
    }
}

@Composable
private fun SimulatedRestoreConfirmMock() {
    Column(modifier = Modifier.fillMaxWidth().padding(BisqUIConstants.ScreenPadding2X)) {
        BisqText.H4Light("Restore from backup")
        BisqGap.V1()
        BisqText.SmallLight("This replaces the app's current local data.", color = BisqTheme.colors.mid_grey20)
        BisqGap.V1()
        BisqButton(text = "Restore", type = BisqButtonType.Danger, fullWidth = true, onClick = {})
    }
}

@Composable
private fun SimulatedTrustedNodePairingMock() {
    Column(modifier = Modifier.fillMaxWidth().padding(BisqUIConstants.ScreenPadding2X)) {
        BisqText.H4Light("Pair with a trusted node")
        BisqGap.V1()
        BisqText.SmallLight("Scan a pairing code or enter it manually.", color = BisqTheme.colors.mid_grey20)
        BisqGap.V1()
        BisqButton(text = "Scan QR code", type = BisqButtonType.Outline, fullWidth = true, onClick = {})
    }
}

@Composable
private fun SimulatedPaymentAccountsMock() {
    Column(modifier = Modifier.fillMaxWidth().padding(BisqUIConstants.ScreenPadding2X)) {
        BisqText.H4Light("Payment accounts")
        BisqGap.V1()
        BisqText.SmallLight("SEPA · IBAN ending 4471", color = BisqTheme.colors.mid_grey20)
        BisqGap.VHalf()
        BisqText.SmallLight("Revolut · @handle", color = BisqTheme.colors.mid_grey20)
    }
}

// -------------------------------------------------------------------------------------
// Previews
// -------------------------------------------------------------------------------------

/** Lock screen — idle, waiting for the user to re-trigger the prompt. */
@ExcludeFromCoverage
@Preview(name = "1. Lock screen — idle")
@Composable
private fun AppLockScreen_Idle_Preview() {
    BisqTheme.Preview {
        AppLockScreen(uiState = simulatedAppLockScreenUiState(phase = LockScreenPhase.IDLE), onAction = {})
    }
}

/** Lock screen — the OS prompt is open, waiting on the user's biometric/credential. */
@ExcludeFromCoverage
@Preview(name = "2. Lock screen — prompt in progress")
@Composable
private fun AppLockScreen_Prompting_Preview() {
    BisqTheme.Preview {
        AppLockScreen(uiState = simulatedAppLockScreenUiState(phase = LockScreenPhase.PROMPTING), onAction = {})
    }
}

/** Lock screen — a failed attempt, with a retry affordance; the OS owns the failure copy itself. */
@ExcludeFromCoverage
@Preview(name = "3. Lock screen — failed attempt with retry")
@Composable
private fun AppLockScreen_Failed_Preview() {
    BisqTheme.Preview {
        AppLockScreen(uiState = simulatedAppLockScreenUiState(phase = LockScreenPhase.FAILED), onAction = {})
    }
}

/** Lock screen — OS-level lockout/cooldown message; no custom attempt counter — see "4." */
@ExcludeFromCoverage
@Preview(name = "4. Lock screen — OS lockout message")
@Composable
private fun AppLockScreen_LockedOut_Preview() {
    BisqTheme.Preview {
        AppLockScreen(uiState = simulatedAppLockScreenUiState(phase = LockScreenPhase.LOCKED_OUT), onAction = {})
    }
}

/** Lock screen — no biometrics enrolled; falls back to device credential automatically. */
@ExcludeFromCoverage
@Preview(name = "5. Lock screen — biometrics unavailable, falls back to device credential")
@Composable
private fun AppLockScreen_Fallback_Preview() {
    BisqTheme.Preview {
        AppLockScreen(uiState = simulatedAppLockScreenUiState(phase = LockScreenPhase.FALLBACK_TO_CREDENTIAL), onAction = {})
    }
}

/** Lock screen — Connect variant, with the small reconnecting indicator, not the full overlay. */
@ExcludeFromCoverage
@Preview(name = "6. Lock screen — Connect, reconnecting indicator")
@Composable
private fun AppLockScreen_ConnectReconnecting_Preview() {
    BisqTheme.Preview {
        AppLockScreen(
            uiState =
                simulatedAppLockScreenUiState(
                    phase = LockScreenPhase.PROMPTING,
                    appVariant = AppVariant.CONNECT,
                    showConnectReconnectingIndicator = true,
                ),
            onAction = {},
        )
    }
}

/** Lock screen — node variant, default idle state. */
@ExcludeFromCoverage
@Preview(name = "7. Lock screen — node variant")
@Composable
private fun AppLockScreen_Node_Preview() {
    BisqTheme.Preview {
        AppLockScreen(
            uiState = simulatedAppLockScreenUiState(phase = LockScreenPhase.IDLE, appVariant = AppVariant.NODE),
            onAction = {},
        )
    }
}

/** Dashboard enrolment card, among other dashboard cards. */
@ExcludeFromCoverage
@Preview(name = "8. Enrolment card — on the dashboard")
@Composable
private fun AppLockEnrolmentCard_InContext_Preview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            SimulatedMarketPriceCard()
            AppLockEnrolmentCard(onAction = {})
        }
    }
}

/** Dismissed state — the card is gone permanently; Settings is the only remaining entry point. */
@ExcludeFromCoverage
@Preview(name = "9. Enrolment card — dismissed, gone from the dashboard")
@Composable
private fun AppLockEnrolmentCard_Dismissed_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            BisqText.SmallLight(
                "Card permanently dismissed — no longer rendered here on this or any future visit:",
                color = BisqTheme.colors.mid_grey20,
            )
            BisqGap.V1()
            SimulatedMarketPriceCard()
        }
    }
}

/** Settings section — off. */
@ExcludeFromCoverage
@Preview(name = "10. Settings section — off")
@Composable
private fun AppLockSettingsSection_Off_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            AppLockSettingsSection(uiState = simulatedAppLockSettingsUiState(enabled = false), onAction = {})
        }
    }
}

/** Settings section — on, with the grace-period row visible. */
@ExcludeFromCoverage
@Preview(name = "11. Settings section — on, with grace selector")
@Composable
private fun AppLockSettingsSection_On_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            AppLockSettingsSection(uiState = simulatedAppLockSettingsUiState(enabled = true), onAction = {})
        }
    }
}

/** Settings section — disabled by device: no credential enrolled, with the OS-settings link. */
@ExcludeFromCoverage
@Preview(name = "12. Settings section — disabled by device, with caption")
@Composable
private fun AppLockSettingsSection_DisabledByDevice_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            AppLockSettingsSection(
                uiState = simulatedAppLockSettingsUiState(controlState = AppLockControlState.DISABLED_BY_DEVICE),
                onAction = {},
            )
        }
    }
}

/** Settings section — required by policy: greyed-on switch with the generic explanation. */
@ExcludeFromCoverage
@Preview(name = "13. Settings section — required by policy, greyed-on with explanation")
@Composable
private fun AppLockSettingsSection_RequiredByPolicy_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            AppLockSettingsSection(
                uiState = simulatedAppLockSettingsUiState(controlState = AppLockControlState.REQUIRED_BY_POLICY),
                onAction = {},
            )
        }
    }
}

/** Grace-period picker sheet, opened from the settings row. */
@ExcludeFromCoverage
@Preview(name = "14. Settings section — grace-period picker sheet")
@Composable
private fun GracePeriodPickerSheet_Preview() {
    BisqTheme.Preview {
        GracePeriodPickerSheet(selected = GracePeriod.ONE_MINUTE, onSelect = {}, onDismiss = {})
    }
}

/** Step-up gate alone — the initial gate state, before the OS prompt is triggered. */
@ExcludeFromCoverage
@Preview(name = "15. Step-up gate — initial state")
@Composable
private fun StepUpGate_Gate_Preview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.fillMaxSize()) {
            StepUpGate(uiState = simulatedStepUpUiState(StepUpPhase.GATE), onAction = {}, actionContext = "before viewing your backup")
        }
    }
}

/** Step-up gate alone — prompting. */
@ExcludeFromCoverage
@Preview(name = "16. Step-up gate — prompt in progress")
@Composable
private fun StepUpGate_Prompting_Preview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.fillMaxSize()) {
            StepUpGate(
                uiState = simulatedStepUpUiState(StepUpPhase.PROMPTING),
                onAction = {},
                actionContext = "before viewing your backup",
            )
        }
    }
}

/** Step-up gate alone — failed, with retry. */
@ExcludeFromCoverage
@Preview(name = "17. Step-up gate — failed attempt with retry")
@Composable
private fun StepUpGate_Failed_Preview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.fillMaxSize()) {
            StepUpGate(
                uiState = simulatedStepUpUiState(StepUpPhase.FAILED),
                onAction = {},
                actionContext = "before viewing your backup",
            )
        }
    }
}

/** Node backup view/export — gate before, screen after. */
@ExcludeFromCoverage
@Preview(name = "18. Step-up — node backup, before and after")
@Composable
private fun StepUp_Backup_BeforeAndAfter_Preview() {
    BisqTheme.Preview {
        Column {
            BisqText.SmallLight("Before — gate blocks the screen:", color = BisqTheme.colors.mid_grey20, modifier = Modifier.padding(BisqUIConstants.ScreenPadding))
            Box {
                SimulatedBackupScreenMock()
                StepUpGate(uiState = simulatedStepUpUiState(StepUpPhase.GATE), onAction = {}, actionContext = "before exporting your backup")
            }
            BisqGap.V2()
            BisqText.SmallLight("After — granted, screen fully interactive:", color = BisqTheme.colors.mid_grey20, modifier = Modifier.padding(BisqUIConstants.ScreenPadding))
            SimulatedBackupScreenMock()
        }
    }
}

/** Node restore — gate before, confirm screen after. */
@ExcludeFromCoverage
@Preview(name = "19. Step-up — node restore, before and after")
@Composable
private fun StepUp_Restore_BeforeAndAfter_Preview() {
    BisqTheme.Preview {
        Column {
            BisqText.SmallLight("Before — gate blocks the screen:", color = BisqTheme.colors.mid_grey20, modifier = Modifier.padding(BisqUIConstants.ScreenPadding))
            Box {
                SimulatedRestoreConfirmMock()
                StepUpGate(uiState = simulatedStepUpUiState(StepUpPhase.GATE), onAction = {}, actionContext = "before restoring from a backup")
            }
            BisqGap.V2()
            BisqText.SmallLight("After — granted, screen fully interactive:", color = BisqTheme.colors.mid_grey20, modifier = Modifier.padding(BisqUIConstants.ScreenPadding))
            SimulatedRestoreConfirmMock()
        }
    }
}

/** Connect trusted-node pairing — gate before, pairing screen after. */
@ExcludeFromCoverage
@Preview(name = "20. Step-up — Connect pairing, before and after")
@Composable
private fun StepUp_Pairing_BeforeAndAfter_Preview() {
    BisqTheme.Preview {
        Column {
            BisqText.SmallLight("Before — gate blocks the screen:", color = BisqTheme.colors.mid_grey20, modifier = Modifier.padding(BisqUIConstants.ScreenPadding))
            Box {
                SimulatedTrustedNodePairingMock()
                StepUpGate(uiState = simulatedStepUpUiState(StepUpPhase.GATE), onAction = {}, actionContext = "before pairing with a trusted node")
            }
            BisqGap.V2()
            BisqText.SmallLight("After — granted, screen fully interactive:", color = BisqTheme.colors.mid_grey20, modifier = Modifier.padding(BisqUIConstants.ScreenPadding))
            SimulatedTrustedNodePairingMock()
        }
    }
}

/** Payment accounts (shared and Connect MuSig) — gate before, account list after. */
@ExcludeFromCoverage
@Preview(name = "21. Step-up — payment accounts, before and after")
@Composable
private fun StepUp_PaymentAccounts_BeforeAndAfter_Preview() {
    BisqTheme.Preview {
        Column {
            BisqText.SmallLight("Before — gate blocks the screen:", color = BisqTheme.colors.mid_grey20, modifier = Modifier.padding(BisqUIConstants.ScreenPadding))
            Box {
                SimulatedPaymentAccountsMock()
                StepUpGate(uiState = simulatedStepUpUiState(StepUpPhase.GATE), onAction = {}, actionContext = "before opening your payment accounts")
            }
            BisqGap.V2()
            BisqText.SmallLight("After — granted, screen fully interactive:", color = BisqTheme.colors.mid_grey20, modifier = Modifier.padding(BisqUIConstants.ScreenPadding))
            SimulatedPaymentAccountsMock()
        }
    }
}

/** Long-locale simulation — lock screen, German text runs longer than English. */
@ExcludeFromCoverage
@Preview(name = "22. Lock screen — simulated long-locale text")
@Composable
private fun AppLockScreen_LongLocaleText_Preview() {
    BisqTheme.Preview {
        AppLockScreen(
            uiState = simulatedAppLockScreenUiState(phase = LockScreenPhase.LOCKED_OUT),
            onAction = {},
            appName = "Bisq Easy",
            lockedOutMessage =
                "Zu viele fehlgeschlagene Versuche. Versuchen Sie es gleich noch einmal oder " +
                    "verwenden Sie Ihre Gerätezugangsdaten.",
        )
    }
}

/** Long-locale simulation — settings section, German text runs longer than English. */
@ExcludeFromCoverage
@Preview(name = "23. Settings section — simulated long-locale text")
@Composable
private fun AppLockSettingsSection_LongLocaleText_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            AppLockSettingsSection(
                uiState = simulatedAppLockSettingsUiState(controlState = AppLockControlState.REQUIRED_BY_POLICY),
                onAction = {},
                headline = "App-Sperre",
                toggleLabel = "Mit Biometrie oder Gerätezugangsdaten sperren",
                disabledByDeviceCaption = "Auf diesem Gerät sind keine Biometrie oder Bildschirmsperre eingerichtet.",
            )
        }
    }
}
