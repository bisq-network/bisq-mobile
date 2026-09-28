/**
 * ReleaseNotificationDesign.kt
 *
 * Compose specification for the dashboard's release-update card. The card surfaces the release
 * manager's signed release notification and, on Google Play installs, drives an in-app update.
 * It is shared by the node (Bisq Easy) and Connect (Client) apps. Not wired to a presenter; state
 * shown in the previews below is produced entirely by the `simulatedXxx` helpers, which take only
 * primitives.
 *
 * ------------------------------------------------------------------------------------
 * 1. PURPOSE
 * ------------------------------------------------------------------------------------
 * The card does two things:
 *   - Surfaces the release manager's signed [bisq.bonded_roles.release.ReleaseNotification] for
 *     the running app's `AppType`, so every distribution — including GitHub and F-Droid builds —
 *     learns when a newer version exists.
 *   - On a Google Play install, drives the Play In-App Updates flexible flow from the card's own
 *     primary action instead of sending the user to a browser. Everywhere else (GitHub, F-Droid,
 *     sideloaded, iOS TestFlight/AltStore PAL) the primary action opens an external release page.
 *
 * Node (Bisq Easy) and Connect (Client) share one card. Its primary action and body branch on
 * install source, the Play download phase, and whether the security manager's minimum-version
 * alert makes this update mandatory (see "7. Mandatory updates"). The minimum-version trading
 * block itself is a pre-existing, separate alert
 * (`shared/presentation/.../common/ui/alert/`, `AlertType.EMERGENCY` + `requiresUpdate`) and is
 * unchanged by this card.
 *
 * ------------------------------------------------------------------------------------
 * 2. DATA SOURCE AND TRUST
 * ------------------------------------------------------------------------------------
 * `ReleaseNotification` (`bisq2 bonded-roles/.../release/ReleaseNotification.java`) carries: `id`,
 * `date`, `isPreRelease`, `isLauncherUpdate`, `releaseNotes`, `versionString`,
 * `releaseManagerProfileId`, `appType` (`DESKTOP` / `MOBILE_NODE` / `MOBILE_CLIENT`). It has no
 * URL field.
 *
 * `releaseNotes` is free text, authored per `AppType` in a plain multi-line field
 * (`MaterialTextArea`, no rich-text controls) — `ReleaseNotificationView.java`'s `releaseNotes`
 * field, one tab per `AppType` (`ReleaseManagerTabController.java`: `DESKTOP_RELEASE_MANAGER` /
 * `MOBILE_NODE_RELEASE_MANAGER` / `MOBILE_CLIENT_RELEASE_MANAGER`). Node and Connect can carry
 * different release-notes text for the same release. The field is capped at
 * `ReleaseNotification.MAX_MESSAGE_LENGTH = 10_000` characters and otherwise unconstrained in
 * shape or formatting. [ReleaseNotesSheet] renders it as plain text with preserved line breaks and
 * no markdown parsing, matching how desktop's own `TextArea` renders the same field — see
 * `UpdaterView.java`, whose `TextArea` is also `setWrapText(true)` with no markdown support.
 *
 * Invariant: the destination the card's primary action opens is always resolved app-side, from
 * `AppUpdateUrls` (extended with `InstallSource`, see "9. Play flexible flow"), never from network
 * data. `ReleaseNotification` must never gain a URL field — doing so would let the release-manager
 * role point the update button anywhere.
 *
 * Trust boundary: the object is signed (`AuthorizedDistributedData`) and delivered over the P2P
 * network; the node verifies that signature. Connect does not re-verify it itself — it trusts what
 * its paired node relays, the same model Connect already applies to security-manager alerts. A
 * compromised or malicious paired node can fabricate this card's text (version string, release
 * notes, the mandatory flag) but cannot redirect the primary action, because the destination is
 * always locally resolved (see the invariant above). The worst case is a misleading card, not a
 * hijacked update path.
 *
 * ------------------------------------------------------------------------------------
 * 3. DATA PATH PER APP
 * ------------------------------------------------------------------------------------
 * Node: reads bisq2's `ReleaseNotificationsService` directly in-process; no network hop.
 *
 * Connect: no existing transport carries `ReleaseNotification` to the client. bisq2's `api/`
 * module has no `ReleaseNotification` reference, and the client's `Topic.kt`
 * (`apps/clientApp/.../common/domain/websocket/subscription/Topic.kt`) has `ALERT_NOTIFICATIONS`
 * but no release-notification topic. This requires a bisq2-side addition: a
 * `ReleaseNotificationsWebSocketService` + REST endpoint + DTO, mirroring the existing
 * `AlertNotificationsWebSocketService` / `AlertNotificationsRestApi`
 * (bisq2 `api/src/main/java/bisq/api/web_socket/domain/alert_notifications/`), plus a new
 * `Topic.RELEASE_NOTIFICATIONS` entry and a client-side facade subscribing to it. This Compose
 * design does not depend on the transport, but Connect cannot ship this card until that addition
 * lands (see "16. Implementation notes").
 *
 * ------------------------------------------------------------------------------------
 * 4. VERSION SELECTION
 * ------------------------------------------------------------------------------------
 * Exactly one `ReleaseNotification` is selected before [ReleaseUpdateUiState] is constructed, per
 * the same rule desktop's `UpdaterService.updateReleaseNotificationState()`
 * (`bisq2/evolution/src/main/java/bisq/evolution/updater/UpdaterService.java`, ~lines 207-232)
 * applies:
 *   - only notifications whose version is strictly above the running app's version are eligible
 *     (`ApplicationVersion.getVersion().below(notification.getReleaseVersion())`);
 *   - among eligible notifications for this `AppType`, the highest version wins via `Version`
 *     comparison (`.max(Comparator.comparing(ReleaseNotification::getReleaseVersion))`), never the
 *     most recently received one — P2P delivery order is not a trustworthy ordering, and the
 *     object carries a 100-day TTL, so a stale, replayed notification for an older version could
 *     otherwise win a "most recently seen" comparison;
 *   - mobile version strings are semver-like ("0.11.0"), compared the same way desktop compares
 *     `Version` objects. Android's separate integer `versionCode` plays no part in this
 *     comparison.
 * The UI never performs this comparison — [ReleaseUpdateUiState] always holds the one notification
 * already selected upstream, so no running-version field is part of this state.
 *
 * ------------------------------------------------------------------------------------
 * 5. PRE-RELEASES
 * ------------------------------------------------------------------------------------
 * Pre-release notifications are filtered out unconditionally before [ReleaseUpdateUiState] is
 * constructed, because mobile publishes no pre-release builds; `ReleaseNotification.isPreRelease`
 * still exists on the wire, but mobile never surfaces it.
 *
 * ------------------------------------------------------------------------------------
 * 6. CARD PLACEMENT AND ANATOMY
 * ------------------------------------------------------------------------------------
 * Placement: `shared/presentation/.../tabs/dashboard/DashboardScreen.kt`, directly below the top
 * info-card row (price / offers-online / connections) and above the hero `DashBoardCard`. This
 * keeps the card visible on first paint without pushing the stat row down, sits below the hero CTA
 * in visual weight, and — only while mandatory — approaches [AlertNotificationBanner]'s own
 * EMERGENCY treatment (see "7. Mandatory updates"). [ReleaseUpdateCard_OnDashboard_Preview] renders
 * the card in that context using the real `HomeInfoCard`/`DashBoardCard` composables imported from
 * `DashboardScreen.kt`.
 *
 * [ReleaseUpdateCard] renders, top to bottom: headline ("Version {0} available", replaced by a red
 * "Required update" badge next to it when `isMandatory`) with the dismiss icon top-right; a
 * one-line, tappable release-notes preview ("What's new ›") that opens [ReleaseNotesSheet]; a
 * state-dependent body (mandatory notice first if applicable, then the phase-specific content);
 * one full-width primary button.
 *
 * [PlayUpdatePhase] only branches when `installSource == InstallSource.PLAY`; every other install
 * source stays at `NOT_APPLICABLE` and shows the external-link variant:
 *
 * | installSource        | playPhase        | Primary button        | Tapping it does                          |
 * |-----------------------|------------------|------------------------|-------------------------------------------|
 * | PLAY                  | AVAILABLE        | "Update"               | Starts the Play flexible in-app update    |
 * | PLAY                  | DOWNLOADING      | (no button; progress)  | Download runs in the background           |
 * | PLAY                  | FAILED           | "Try again"            | Retries the Play download                 |
 * | PLAY                  | READY_TO_RESTART | "Restart now"          | Applies the update; Connect restarts the  |
 * |                       |                  |                        | app, the node app restarts the node       |
 * | GITHUB                | NOT_APPLICABLE   | "View on GitHub"       | Opens `AppUpdateUrls.GITHUB_RELEASES`     |
 * | FDROID                | NOT_APPLICABLE   | "Open F-Droid"         | Opens `GITHUB_RELEASES` (see the TODO     |
 * |                       |                  |                        | near [externalLinkLabel])                 |
 * | IOS_TESTFLIGHT        | NOT_APPLICABLE   | "Open TestFlight"      | Opens the iOS install page                |
 * | IOS_ALTSTORE_PAL       | NOT_APPLICABLE   | "Open AltStore"        | Opens the iOS install page                |
 * | PLAY (sideloaded APK)  | NOT_APPLICABLE   | "View on GitHub"       | Same as GITHUB                            |
 *
 * `isMandatory` does not change the primary button in this table — the action is the same, only
 * the badge, dismiss affordance, and body copy change.
 *
 * A sideloaded APK of the google flavor resolves to `GITHUB` (or whichever channel it was actually
 * installed from) at detection time, never to `PLAY` — [InstallSource] is a build-flavor-
 * independent, runtime-detected value, not a build-time constant; see "9. Play flexible flow" for
 * detection.
 *
 * ------------------------------------------------------------------------------------
 * 7. MANDATORY UPDATES
 * ------------------------------------------------------------------------------------
 * `isMandatory` mirrors desktop's `UpdaterController.updateIgnoreVersionState()` (lines ~172-196):
 * ```
 * boolean requireVersionForTradingAboveAppVersion = isRequireVersionForTradingAboveAppVersion();
 * model.getIgnoreVersion().set(getIgnoreVersionFromCookie() && !downloadStarted && !requireVersionForTradingAboveAppVersion);
 * model.getIgnoreVersionSwitchVisible().set(!requireVersionForTradingAboveAppVersion);
 * ```
 * The "Ignore this version" control disappears whenever `isRequireVersionForTradingAboveAppVersion()`
 * is true — the same condition the security manager's minimum-version alert already uses to block
 * trading.
 *
 * Derivation: `AuthorizedAlertData.requireVersionForTrading` + `.minVersion`
 * (`shared/domain/.../domain/model/alert/AuthorizedAlertData.kt`), already surfaced as
 * `AlertNotificationUiState.requiresUpdate` + `.minVersion`
 * (`shared/presentation/.../common/ui/alert/AlertNotificationUiState.kt`), is the same signal
 * `isMandatory` reads — scoped to this build's `AppType`: an active EMERGENCY alert,
 * `requiresUpdate == true`, `minVersion` above the running app's version.
 * [ReleaseUpdateUiState.minVersionString] carries that required version into the card so the
 * notice can name it.
 *
 * Visual treatment: `BisqTheme.colors.danger` (red), matching
 * `AlertNotificationCommonUi.alertAccentColor`'s existing `AlertType.EMERGENCY` → `danger` mapping
 * across the app — this state is that same EMERGENCY alert surfaced a second time, so it uses the
 * color the app already associates with that severity. [RequiredUpdateBadge] (a danger-tinted
 * pill) sits next to the headline in place of where a pre-release chip would otherwise go; the
 * card gets a 2dp danger border; [ReleaseUpdateCardBody] prepends a danger-colored notice line
 * ahead of the phase-specific body. The badge text and the notice text both carry the meaning in
 * words, not only in color.
 *
 * Dismiss (the X icon, and the sheet's "Ignore this version" link) is hidden whenever
 * `isMandatory` is true, in addition to the existing hide-during-`DOWNLOADING`/`READY_TO_RESTART`
 * rule (see "8. Dismiss model") — the two conditions are independent and both suppress the same
 * control.
 *
 * A version already dismissed as optional must resurface, non-dismissible, once this same alert
 * makes it mandatory: the per-version dismissed-set is consulted only when `!isMandatory`.
 * Desktop's own filter skips its ignore-cookie check in exactly that branch; the mobile
 * dismissed-set lookup must do the same (see "16. Implementation notes").
 * [ReleaseUpdateCard_MandatoryPlay_Preview] and [ReleaseUpdateCard_MandatoryExternalLink_Preview]
 * render identically whether or not the version was previously dismissed, because `dismissVisible`
 * derives from `isMandatory` alone and the composable never holds a dismissed-set.
 *
 * When `isMandatory` is true, [AlertNotificationBanner] (the security-manager EMERGENCY banner)
 * and this card are visible on the dashboard at the same time — they read the same alert, so one
 * never shows without the other. Both stay visible: the banner is the network-wide reason trading
 * is blocked, shown on every screen; the card is the update path, reachable from the dashboard
 * without reopening the banner. [ReleaseUpdateCardBody]'s mandatory notice reuses the banner's own
 * string verbatim — `mobile.alert.update.minimum` = "Trading requires app version {0} or newer.
 * Please update to continue trading." (`AlertNotificationDialog.kt`) — substituting
 * [ReleaseUpdateUiState.minVersionString] via [mandatoryNoticeText], so the two surfaces read as
 * one message. The primary button keeps its phase-specific copy ("Update" / "Try again" /
 * "Restart now") rather than the banner's generic "Update now" (`mobile.alert.update.button`),
 * because that extra precision — e.g. "Restart now" only once the download is ready — is this
 * card's reason to exist alongside the banner.
 *
 * ------------------------------------------------------------------------------------
 * 8. DISMISS MODEL
 * ------------------------------------------------------------------------------------
 * Dismiss is a per-version, persisted ignore — mirroring desktop's `IGNORE_VERSION` cookie — not a
 * snooze: the card must remain visible until the user dismisses this exact version or updates. It
 * stays on the dashboard permanently, so leaving it alone already reproduces "remind me later"
 * with no separate control needed. The X icon top-right (content description reuses
 * `updater.ignore` = "Ignore this version") and a matching tertiary "Ignore this version" link
 * inside [ReleaseNotesSheet] both dispatch [OnDismissVersion].
 *
 * Dismiss is hidden while `playPhase` is `DOWNLOADING` or `READY_TO_RESTART`: the Play flexible
 * flow surfaces no system-level "update ready" notification outside the app, so hiding the card
 * mid-flow would strand a finished download with no visible next step. It stays hidden once
 * `isMandatory` is true regardless of phase (see "7. Mandatory updates").
 *
 * [DismissedUpdateResourcesEntry] adds one conditional line under the existing `Version()` section
 * of `ResourcesScreen.kt`, shown only while a dismissed-but-still-current update exists; tapping it
 * reopens [ReleaseNotesSheet] through the same action the card would dispatch. A mandatory update
 * is never dismissible, so this entry point only ever applies to an optional update the user chose
 * to ignore.
 *
 * ------------------------------------------------------------------------------------
 * 9. PLAY FLEXIBLE FLOW
 * ------------------------------------------------------------------------------------
 * `DOWNLOADING` has no cancel control: the Android In-App Updates API surface
 * (`AppUpdateManager`) exposes only `getAppUpdateInfo()` / `startUpdateFlowForResult()` /
 * `registerListener()` / `unregisterListener()` / `completeUpdate()` — no cancel-in-progress
 * method. `RESULT_CANCELED` from `startUpdateFlowForResult()` only fires when the user declines
 * the Play system dialog before the download starts, not after.
 *
 * Install-source detection reuses the existing `AppUpdateLinker`/`AndroidAppUpdateLinker`
 * (`shared/domain/.../data/utils/PlatformDomainAbstractions.kt` + `.android.kt`, ~lines 160-192),
 * already wired to the alert banner's "Update now" action and already resolving Play vs. non-Play
 * via `getInstallSourceInfo`/`getInstallerPackageName`. It must be extended with an
 * `InstallSource`-aware method rather than built again as a separate resolver: add `FDROID` via
 * the fdroid build flavor, add the two iOS values in `PlatformDomainAbstractions.ios.kt`.
 *
 * The Play `InAppUpdateProvider` implementation must live in a flavor-specific Android source set
 * — never behind a runtime `if` in shared `androidMain` — because a runtime branch still compiles
 * Play Core classes into the fdroid APK, which the F-Droid verify scripts check for.
 * `apps/nodeApp/build.gradle.kts` has no product flavors today, unlike
 * `apps/clientApp/build.gradle.kts`'s `google`/`fdroid` `distribution` dimension; the node app
 * needs the same flavor split added before the Play provider can be isolated (see
 * "16. Implementation notes").
 *
 * ------------------------------------------------------------------------------------
 * 10. PER-INSTALL-SOURCE BEHAVIOUR
 * ------------------------------------------------------------------------------------
 * | InstallSource     | Platform      | Runtime detection                     | In-app upgrade |
 * |-------------------|---------------|----------------------------------------|----------------|
 * | PLAY              | Android       | installer package == Play Store       | Yes (flexible) |
 * | GITHUB            | Android       | non-Play installer, non-fdroid flavor | No — link      |
 * | FDROID            | Android       | fdroid build flavor                   | No — link      |
 * | IOS_TESTFLIGHT    | iOS (Connect) | n/a — no in-app flow on iOS            | No — link      |
 * | IOS_ALTSTORE_PAL  | iOS (Connect) | n/a — no in-app flow on iOS            | No — link      |
 *
 * Only an installer package that genuinely reports Google Play drives the Play flow; a sideloaded
 * APK of the google flavor is `GITHUB` at detection time, never `PLAY`. Detection reuses
 * `AppUpdateLinker`/`AndroidAppUpdateLinker` (see "9. Play flexible flow").
 *
 * The node app (Bisq Easy) is Android-only, so `isNode == true` never pairs with an iOS
 * `InstallSource`; both node builds (Play google flavor, F-Droid flavor) are covered above.
 *
 * ------------------------------------------------------------------------------------
 * 11. NODE RESTART
 * ------------------------------------------------------------------------------------
 * Only `READY_TO_RESTART`'s body text branches on [ReleaseUpdateUiState.isNode] — every other
 * state's copy is identical between the two apps. Connect has no local trade state, so its copy
 * ([DEFAULT_CONNECT_RESTART_NOTICE]) is a single line: "Bisq Connect will restart to finish
 * updating."
 *
 * The node's copy ([DEFAULT_NODE_RESTART_TITLE] + [DEFAULT_NODE_RESTART_NOTICE]) does not claim
 * trades and offers are unconditionally safe, because two things are true today: offers are public
 * P2P data with a 10-day TTL and do not depend on the node staying up, but trade state is
 * persisted through a rate-limited write path — writes within 1 second of each other can be
 * dropped, and only a JVM shutdown hook flushes reliably. Play's update flow kills the process to
 * install the update without running that hook.
 *
 * TODO(wodoro): before calling Play's `completeUpdate()` on a node build, force an unthrottled
 * flush of the trade and identity stores — no such API exists on the current bisq2 branch. The
 * existing deactivate/shutdown path does not persist the trade store either.
 *
 * TODO(wodoro): wire bisq2's `MigrationService` into the Android node's init sequence before
 * shipping this restart flow — `AndroidApplicationService.initialize()` overrides the base
 * `initialize()` and skips it today, and a version bump is exactly when a migration is needed.
 *
 * ------------------------------------------------------------------------------------
 * 12. SHEET VS. DIALOG
 * ------------------------------------------------------------------------------------
 * [ReleaseNotesSheet] is a [BisqBottomSheet], not a `ConfirmationDialog`/`BisqDialog`. Release
 * notes are optional reading, not a decision the user must resolve, matching this codebase's
 * existing sheet pattern for browsable content (`MarketFilters`) rather than its dialog pattern
 * for confirmations and warnings; a mandatory update does not change this — the sheet only
 * explains, the card's own primary button carries the actual action.
 * `ReleaseNotification.releaseNotes` can run up to `MAX_MESSAGE_LENGTH = 10_000` characters;
 * `BisqBottomSheet` already caps at `MAX_SHEET_HEIGHT = 500.dp` with an internally scrolling
 * `Column`, which fits that ceiling with no additional work. The sheet's primary action is
 * identical to the card's (see "6. Card placement and anatomy"), so a dialog's heavier,
 * must-dismiss framing would overstate what tapping "What's new" actually commits the user to.
 *
 * ------------------------------------------------------------------------------------
 * 13. ACCESSIBILITY AND LONG-LOCALE
 * ------------------------------------------------------------------------------------
 * - The dismiss icon's `contentDescription` is real, localized text (`updater.ignore`), not a
 *   `semantics {}` block used as a test-id substitute; `Modifier.testTag(...)` is layered on
 *   separately for automated tests, per repo convention.
 * - The release-notes preview line and headline use `BisqText.StyledText`/`AutoResizeText` with
 *   `TextOverflow.Ellipsis` rather than a fixed-size `BisqText`, so German/French version strings
 *   or long release-note first lines shrink/truncate gracefully instead of clipping mid-word — see
 *   [ReleaseUpdateCard_LongLocaleText_Preview].
 * - The sheet's release-notes body is not line-capped (unlike the card's one-line preview) — it is
 *   the surface that shows the full text, so nothing there truncates regardless of locale length.
 * - The download-progress percentage renders as a whole number
 *   (`"${(progress * 100).roundToInt()}%"`, `kotlin.math.roundToInt`): Play reports coarse,
 *   infrequent progress increments, so a fixed 2-decimal value would overstate the precision the
 *   source data has. See "14. Proposed i18n keys" for the production format key.
 * - The mandatory state does not rely on color alone: [RequiredUpdateBadge] carries the text label
 *   "Required update," and [ReleaseUpdateCardBody]'s mandatory notice spells out the consequence
 *   in words (see [mandatoryNoticeText]) — the red border/tint reinforces, but is never the sole
 *   carrier of, the meaning.
 *
 * ------------------------------------------------------------------------------------
 * 14. PROPOSED I18N KEYS
 * ------------------------------------------------------------------------------------
 * English base values only, per repo convention — nothing is added to `mobile.properties` by this
 * file; production implementation adds these.
 *
 * New:
 *   mobile.dashboard.releaseUpdate.headline = "Version {0} available"
 *   mobile.dashboard.releaseUpdate.notesPreviewLink = "What's new"
 *   mobile.dashboard.releaseUpdate.action.update = "Update"
 *   mobile.dashboard.releaseUpdate.downloading = "Downloading update…"
 *   mobile.dashboard.releaseUpdate.downloadProgressPercent = "{0}%" — production should route the
 *     whole-number percent through this key rather than a hand-built string, so translators can
 *     reposition the "%" symbol per locale (e.g. before the number in Persian).
 *   mobile.dashboard.releaseUpdate.downloadFailed = "Couldn't download the update"
 *   mobile.dashboard.releaseUpdate.action.retry = "Try again"
 *   mobile.dashboard.releaseUpdate.action.restart = "Restart now"
 *   mobile.dashboard.releaseUpdate.restartNotice.connect = "Bisq Connect will restart to finish updating."
 *   mobile.dashboard.releaseUpdate.restartNotice.node.title = "Restart to finish updating"
 *   mobile.dashboard.releaseUpdate.restartNotice.node.body = "Your offers stay live on the
 *     network. Trades in progress are kept - you may see a brief reconnect after the restart."
 *   mobile.dashboard.releaseUpdate.action.viewOnGithub = "View on GitHub"
 *   mobile.dashboard.releaseUpdate.action.openPlayStore = "Open Play Store"
 *   mobile.dashboard.releaseUpdate.action.openFdroid = "Open F-Droid"
 *   mobile.dashboard.releaseUpdate.action.openTestFlight = "Open TestFlight"
 *   mobile.dashboard.releaseUpdate.action.openAltStore = "Open AltStore"
 *   mobile.dashboard.releaseUpdate.mandatory.badge = "Required update"
 *   mobile.resources.version.updateAvailable = "Version {0} available — tap to view"
 *
 * Reused, already present (all 14 locales) in `shared/domain/.../GeneratedResourceBundles_*.kt` /
 * `mobile.properties`:
 *   updater.releaseNotesHeadline = "Release notes for version {0}:" (sheet headline)
 *   updater.ignore = "Ignore this version" (dismiss icon content description + sheet skip link;
 *     shown only when NOT mandatory)
 *   mobile.alert.update.minimum = "Trading requires app version {0} or newer. Please update to
 *     continue trading." (mandatory notice, card + sheet — see "7. Mandatory updates")
 *
 * ------------------------------------------------------------------------------------
 * 15. TESTS TO ADD
 * ------------------------------------------------------------------------------------
 * - Version-comparison unit test: version strictly above / equal to / below the running app
 *   version; multiple notifications for one `AppType` (highest version wins, never newest-by-
 *   date); a pre-release notification filtered out unconditionally regardless of version. See
 *   "4. Version selection."
 * - Presenter test: `AppType`/version/pre-release filtering; per-version dismiss persists across
 *   presenter restarts; a mandatory update overrides an existing persisted dismiss for that exact
 *   version (see "7. Mandatory updates"); install-source fallback resolution (Play → external link
 *   when the installer package is not Play, including a sideloaded google-flavor APK).
 * - Provider-factory test: one `InAppUpdateProvider` per flavor — `google` returns the real Play
 *   implementation, `fdroid` (and any non-Play flavor) returns "unsupported" — confirming no Play
 *   Core symbol needs resolving on the fdroid compile classpath.
 * - UI test for [ReleaseUpdateCard] across its states, following this repo's existing pattern
 *   (e.g. `AlertNotificationBannerUiTest`): dismiss visibility per state, primary button label and
 *   action per state, mandatory badge/border/notice presence.
 *
 * ------------------------------------------------------------------------------------
 * 16. IMPLEMENTATION NOTES FOR THE DEVELOPER
 * ------------------------------------------------------------------------------------
 * - The node app needs a `google`/`fdroid` flavor split (`apps/nodeApp/build.gradle.kts`) before
 *   the Play `InAppUpdateProvider` implementation can be isolated to a flavor-specific source set
 *   (see "9. Play flexible flow").
 * - Connect needs the bisq2-side `ReleaseNotificationsWebSocketService` + REST endpoint + DTO +
 *   client `Topic` described in "3. Data path per app"; none of it exists yet.
 * - New `ReleaseNotificationsServiceFacade` (node reads bisq2's `ReleaseNotificationsService`
 *   directly; Connect subscribes to the new topic once available — see "3. Data path per app")
 *   exposing the newest applicable `ReleaseNotification` for this `AppType` as a `StateFlow`,
 *   selected per "4. Version selection" (strictly-above, highest-version-wins, pre-release
 *   filtered unconditionally), plus the per-version dismissed-set (persisted, mirroring
 *   `CookieKey.IGNORE_VERSION`).
 * - Presenter: the dismissed-set lookup must be gated `if (!isMandatory)` — never suppress a
 *   mandatory notification because an earlier, optional pass of the same version was dismissed.
 *   See "7. Mandatory updates."
 * - `DashboardPresenter`: fold that flow, an `InAppUpdateProvider` (flavor-specific — see
 *   "9. Play flexible flow"), and `isMandatory` + `minVersionString` (re-derived from the same
 *   `AuthorizedAlertData`/`AlertNotificationUiState` EMERGENCY + `requiresUpdate` + `minVersion`
 *   signal that already gates trading — do not introduce a second alert source) into a
 *   [ReleaseUpdateUiState]-shaped `StateFlow`. `installSource` resolution reuses the existing
 *   `AppUpdateLinker`/`AndroidAppUpdateLinker`, extended with `InstallSource` (see
 *   "10. Per-install-source behaviour").
 * - `ResourcesPresenter`: expose the dismissed-but-still-current version string (or null) for
 *   [DismissedUpdateResourcesEntry]; tapping it routes through the same action the dashboard card
 *   would dispatch, not a separate code path.
 * - Node restart prerequisites: see the two `TODO:` items in "11. Node restart" (unthrottled store
 *   flush before `completeUpdate()`; `MigrationService` wiring into Android node init).
 */
package network.bisq.mobile.presentation.design.release_notification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.icon_chat_circle
import bisqapps.shared.presentation.generated.resources.icon_markets
import bisqapps.shared.presentation.generated.resources.reputation
import network.bisq.mobile.presentation.common.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqCard
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqProgressBar
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.AppLinkIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.tabs.dashboard.DashBoardCard
import network.bisq.mobile.presentation.tabs.dashboard.HomeInfoCard
import kotlin.math.roundToInt

// -------------------------------------------------------------------------------------
// State and actions (production shape lives in DashboardPresenter / ResourcesPresenter,
// see "16. Implementation notes for the developer" above)
// -------------------------------------------------------------------------------------

/** Where the running app was installed from — resolved at runtime, not per build flavor. */
internal enum class InstallSource {
    PLAY,
    GITHUB,
    FDROID,
    IOS_TESTFLIGHT,
    IOS_ALTSTORE_PAL,
}

/** Only meaningful while [ReleaseUpdateUiState.installSource] is [InstallSource.PLAY]. */
internal enum class PlayUpdatePhase {
    NOT_APPLICABLE,
    AVAILABLE,
    DOWNLOADING,
    FAILED,
    READY_TO_RESTART,
}

/**
 * Stands in for the slice of `DashboardPresenter` state this design touches.
 *
 * @param isMandatory True when the security manager's min-version-for-trading EMERGENCY alert
 * applies to this build (see "7. Mandatory updates") — the same alert that already halts trading,
 * not an independent signal. Hides both dismiss entry points and switches the card to the
 * danger-tinted "Required update" treatment.
 * @param minVersionString The EMERGENCY alert's required version (e.g. "2.1.8"), only meaningful
 * while [isMandatory] is true. Used to render the reused `mobile.alert.update.minimum` notice —
 * see [mandatoryNoticeText] and "7. Mandatory updates."
 */
internal data class ReleaseUpdateUiState(
    val versionString: String,
    val releaseNotes: String,
    val isNode: Boolean,
    val installSource: InstallSource,
    val playPhase: PlayUpdatePhase,
    val isMandatory: Boolean = false,
    val minVersionString: String = "",
    val downloadProgress: Float = 0f,
)

internal sealed interface ReleaseUpdateUiAction {
    /** Tapping the card's "What's new" preview line — opens [ReleaseNotesSheet]. */
    data object OnOpenReleaseNotes : ReleaseUpdateUiAction

    /** Card's dismiss (X) icon, or the sheet's "Ignore this version" link. Persists per version. */
    data object OnDismissVersion : ReleaseUpdateUiAction

    /** Play, [PlayUpdatePhase.AVAILABLE] → starts the flexible in-app update download. */
    data object OnStartPlayUpdate : ReleaseUpdateUiAction

    /** Play, [PlayUpdatePhase.FAILED] → retries the download. */
    data object OnRetryPlayDownload : ReleaseUpdateUiAction

    /** Play, [PlayUpdatePhase.READY_TO_RESTART] → applies the update (app or node restart). */
    data object OnRestartToApplyUpdate : ReleaseUpdateUiAction

    /** Any non-Play [InstallSource] → opens the external store/release page. */
    data object OnOpenExternalUpdateLink : ReleaseUpdateUiAction

    /** Sheet dismissal via swipe, tap-outside, or its own close affordance. */
    data object OnCloseReleaseNotesSheet : ReleaseUpdateUiAction
}

/** Builds a [ReleaseUpdateUiState] from primitives with realistic defaults. */
internal fun simulatedReleaseUpdateUiState(
    versionString: String = "0.11.0",
    releaseNotes: String = DEFAULT_RELEASE_NOTES,
    isNode: Boolean = false,
    installSource: InstallSource = InstallSource.PLAY,
    playPhase: PlayUpdatePhase = PlayUpdatePhase.AVAILABLE,
    isMandatory: Boolean = false,
    minVersionString: String = "",
    downloadProgress: Float = 0f,
): ReleaseUpdateUiState =
    ReleaseUpdateUiState(
        versionString = versionString,
        releaseNotes = releaseNotes,
        isNode = isNode,
        installSource = installSource,
        playPhase = playPhase,
        isMandatory = isMandatory,
        minVersionString = minVersionString,
        downloadProgress = downloadProgress,
    )

private const val DEFAULT_RELEASE_NOTES =
    "- Faster offerbook sync over Tor\n" +
        "- Fixed a crash when opening a trade from a push notification\n" +
        "- Reputation scores now refresh immediately after a burn/bond transaction confirms\n" +
        "- Various translation updates"

/**
 * Avoids absolute claims ("not lost," "guaranteed") because the trade-store write path is
 * rate-limited and the Play update flow kills the process without running the shutdown-hook
 * flush; see "11. Node restart" for the prerequisites this copy's accuracy depends on.
 */
private const val DEFAULT_NODE_RESTART_TITLE = "Restart to finish updating"

private const val DEFAULT_NODE_RESTART_NOTICE =
    "Your offers stay live on the network. Trades in progress are kept - you may see a brief " +
        "reconnect after the restart."

private const val DEFAULT_CONNECT_RESTART_NOTICE = "Bisq Connect will restart to finish updating."

/**
 * Reuses `mobile.alert.update.minimum` verbatim (the emergency alert banner's own string) — see
 * "7. Mandatory updates" for why.
 */
private fun mandatoryNoticeText(minVersionString: String): String = "Trading requires app version $minVersionString or newer. Please update to continue trading."

/**
 * A realistic ~10,000-character plain-text sample — see "2. Data source and trust." Nothing
 * constrains the length of `ReleaseNotification.releaseNotes` short of the 10,000-character cap,
 * so this sample exercises the sheet at that ceiling rather than a comfortable demo length.
 */
private fun longReleaseNotesSample(): String {
    val paragraph =
        "This release focuses on connection reliability over Tor, faster offerbook sync, and a " +
            "round of translation updates contributed by the community. We also fixed several " +
            "crashes reported through GlitchTip and tightened validation on payment account " +
            "forms. Thanks to everyone who tested the release candidates and filed issues.\n\n"
    val builder = StringBuilder()
    while (builder.length < 10_000) {
        builder.append(paragraph)
    }
    return builder.toString().take(10_000)
}

// -------------------------------------------------------------------------------------
// Dashboard card
// -------------------------------------------------------------------------------------

/**
 * The dashboard release-update card. See "6. Card placement and anatomy" for the full state
 * table, and "7. Mandatory updates" for the `isMandatory` treatment.
 */
@Composable
internal fun ReleaseUpdateCard(
    uiState: ReleaseUpdateUiState,
    onAction: (ReleaseUpdateUiAction) -> Unit,
) {
    val dismissVisible =
        !uiState.isMandatory &&
            uiState.playPhase != PlayUpdatePhase.DOWNLOADING &&
            uiState.playPhase != PlayUpdatePhase.READY_TO_RESTART

    BisqCard(
        modifier =
            if (uiState.isMandatory) {
                Modifier.border(
                    width = 2.dp,
                    color = BisqTheme.colors.danger,
                    shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                )
            } else {
                Modifier
            },
        padding = BisqUIConstants.ScreenPadding2X,
        backgroundColor = if (uiState.isMandatory) BisqTheme.colors.danger.copy(alpha = 0.10f) else BisqTheme.colors.dark_grey40,
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
                AutoResizeText(
                    text = "Version ${uiState.versionString} available",
                    maxLines = 1,
                    textStyle = BisqTheme.typography.h5Light,
                    color = BisqTheme.colors.white,
                    overflow = TextOverflow.Ellipsis,
                )
                if (uiState.isMandatory) {
                    RequiredUpdateBadge()
                }
            }
            if (dismissVisible) {
                IconButton(
                    onClick = { onAction(ReleaseUpdateUiAction.OnDismissVersion) },
                    modifier = Modifier.testTag("release_update_card_dismiss"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Ignore this version",
                        tint = BisqTheme.colors.light_grey20,
                        modifier = Modifier.height(20.dp),
                    )
                }
            }
        }

        BisqGap.VHalf()

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onAction(ReleaseUpdateUiAction.OnOpenReleaseNotes) }
                    .testTag("release_update_card_notes_preview"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BisqText.StyledText(
                text = uiState.releaseNotes.lineSequence().first(),
                style = BisqTheme.typography.smallLight,
                color = BisqTheme.colors.mid_grey20,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            BisqGap.H1()
            BisqText.SmallMedium(
                text = "What's new ›",
                color = BisqTheme.colors.primary,
            )
        }

        BisqGap.V1()
        ReleaseUpdateCardBody(uiState)
        BisqGap.V1()
        ReleaseUpdateCardPrimaryButton(uiState, onAction)
    }
}

/**
 * Danger-tinted pill, shown in place of where a pre-release chip would otherwise go — see
 * "7. Mandatory updates" for the color choice.
 */
@Composable
private fun RequiredUpdateBadge() {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.danger.copy(alpha = 0.20f))
                .padding(
                    horizontal = BisqUIConstants.ScreenPaddingHalf,
                    vertical = BisqUIConstants.ScreenPaddingQuarter,
                ).testTag("release_update_card_mandatory_badge"),
    ) {
        BisqText.XSmallMedium(
            text = "Required update",
            color = BisqTheme.colors.danger,
        )
    }
}

/**
 * State-dependent body content between the notes-preview line and the primary button. Prepends
 * the mandatory notice (if applicable) ahead of whatever phase-specific content would otherwise
 * show — see "7. Mandatory updates."
 */
@Composable
private fun ReleaseUpdateCardBody(uiState: ReleaseUpdateUiState) {
    Column {
        if (uiState.isMandatory) {
            BisqText.SmallMedium(
                text = mandatoryNoticeText(uiState.minVersionString),
                color = BisqTheme.colors.danger,
                modifier = Modifier.testTag("release_update_card_mandatory_notice"),
            )
        }

        when {
            uiState.installSource == InstallSource.PLAY && uiState.playPhase == PlayUpdatePhase.DOWNLOADING -> {
                Column(modifier = Modifier.testTag("release_update_card_downloading")) {
                    BisqText.SmallLight(
                        text = "Downloading update…",
                        color = BisqTheme.colors.mid_grey20,
                    )
                    BisqGap.VQuarter()
                    BisqProgressBar(
                        progress = uiState.downloadProgress,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                    )
                    BisqGap.VQuarter()
                    BisqText.SmallLight(
                        // Whole-number percent; a fixed 2-decimal value would overstate the
                        // precision the source data has — see "13. Accessibility and long-locale."
                        text = "${(uiState.downloadProgress * 100).roundToInt()}%",
                        color = BisqTheme.colors.mid_grey30,
                    )
                }
            }

            uiState.installSource == InstallSource.PLAY && uiState.playPhase == PlayUpdatePhase.FAILED -> {
                BisqText.SmallLight(
                    text = "Couldn't download the update",
                    color = BisqTheme.colors.warning,
                    modifier = Modifier.testTag("release_update_card_download_failed"),
                )
            }

            uiState.installSource == InstallSource.PLAY && uiState.playPhase == PlayUpdatePhase.READY_TO_RESTART -> {
                if (uiState.isNode) {
                    Column(modifier = Modifier.testTag("release_update_card_restart_notice")) {
                        BisqText.SmallMedium(
                            text = DEFAULT_NODE_RESTART_TITLE,
                            color = BisqTheme.colors.warning,
                        )
                        BisqGap.VQuarter()
                        BisqText.SmallLight(
                            text = DEFAULT_NODE_RESTART_NOTICE,
                            color = BisqTheme.colors.warning,
                        )
                    }
                } else {
                    BisqText.SmallLight(
                        text = DEFAULT_CONNECT_RESTART_NOTICE,
                        color = BisqTheme.colors.warning,
                        modifier = Modifier.testTag("release_update_card_restart_notice"),
                    )
                }
            }

            else -> {
                // AVAILABLE (Play, not started) and every non-Play fallback: no extra body copy
                // beyond the mandatory notice (if any) — the headline + notes preview + primary
                // button already say everything else needed.
            }
        }
    }
}

/** The single full-width primary button, or none while a Play download is in progress. */
@Composable
private fun ReleaseUpdateCardPrimaryButton(
    uiState: ReleaseUpdateUiState,
    onAction: (ReleaseUpdateUiAction) -> Unit,
) {
    if (uiState.installSource == InstallSource.PLAY && uiState.playPhase == PlayUpdatePhase.DOWNLOADING) {
        return
    }

    val (text, action, leftIcon) =
        when {
            uiState.installSource != InstallSource.PLAY ->
                Triple(externalLinkLabel(uiState.installSource), ReleaseUpdateUiAction.OnOpenExternalUpdateLink, true)

            uiState.playPhase == PlayUpdatePhase.FAILED ->
                Triple("Try again", ReleaseUpdateUiAction.OnRetryPlayDownload, false)

            uiState.playPhase == PlayUpdatePhase.READY_TO_RESTART ->
                Triple("Restart now", ReleaseUpdateUiAction.OnRestartToApplyUpdate, false)

            else ->
                Triple("Update", ReleaseUpdateUiAction.OnStartPlayUpdate, false)
        }

    BisqButton(
        text = text,
        fullWidth = true,
        leftIcon = if (leftIcon) ({ AppLinkIcon(modifier = Modifier.height(16.dp)) }) else null,
        onClick = { onAction(action) },
        modifier = Modifier.testTag("release_update_card_primary_action"),
    )
}

/**
 * F-Droid has no official listing URL yet; the destination falls back to
 * `AppUpdateUrls.GITHUB_RELEASES` until one is published (destination resolution is
 * presenter-side, this design layer only owns button copy).
 */
private fun externalLinkLabel(installSource: InstallSource): String =
    when (installSource) {
        InstallSource.PLAY -> "Open Play Store"
        InstallSource.GITHUB -> "View on GitHub"
        // TODO(wodoro): replace with the official F-Droid listing URL once published
        InstallSource.FDROID -> "Open F-Droid"
        InstallSource.IOS_TESTFLIGHT -> "Open TestFlight"
        InstallSource.IOS_ALTSTORE_PAL -> "Open AltStore"
    }

// -------------------------------------------------------------------------------------
// Release notes sheet
// -------------------------------------------------------------------------------------

/**
 * Read-only release-notes surface — see "12. Sheet vs. dialog" for why this is a
 * [BisqBottomSheet] rather than a dialog, and "2. Data source and trust" for why the body is
 * plain text. The "Ignore this version" link is omitted entirely when
 * [ReleaseUpdateUiState.isMandatory] — see "7. Mandatory updates."
 */
@Composable
internal fun ReleaseNotesSheet(
    uiState: ReleaseUpdateUiState,
    onAction: (ReleaseUpdateUiAction) -> Unit,
) {
    BisqBottomSheet(onDismissRequest = { onAction(ReleaseUpdateUiAction.OnCloseReleaseNotesSheet) }) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BisqUIConstants.ScreenPadding2X)
                    .padding(bottom = BisqUIConstants.ScreenPadding2X),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BisqText.H5Light(
                    text = "Release notes for version ${uiState.versionString}:",
                    modifier = Modifier.weight(1f),
                )
                if (uiState.isMandatory) {
                    RequiredUpdateBadge()
                }
            }

            if (uiState.isMandatory) {
                BisqGap.VHalf()
                BisqText.SmallMedium(
                    text = mandatoryNoticeText(uiState.minVersionString),
                    color = BisqTheme.colors.danger,
                    modifier = Modifier.testTag("release_notes_sheet_mandatory_notice"),
                )
            }

            BisqGap.V1()
            BisqText.SmallLight(
                text = uiState.releaseNotes,
                color = BisqTheme.colors.light_grey10,
            )

            BisqGap.V2()
            ReleaseUpdateCardPrimaryButton(uiState, onAction)

            if (!uiState.isMandatory) {
                BisqGap.VHalf()
                BisqButton(
                    text = "Ignore this version",
                    type = BisqButtonType.Underline,
                    onClick = { onAction(ReleaseUpdateUiAction.OnDismissVersion) },
                    modifier = Modifier.testTag("release_notes_sheet_ignore_version"),
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// Resources screen entry — reopening a dismissed card
// -------------------------------------------------------------------------------------

/**
 * One conditional line under `ResourcesScreen.kt`'s existing `Version()` section — see
 * "8. Dismiss model." Shown only while a dismissed-but-still-current update exists.
 */
@Composable
internal fun DismissedUpdateResourcesEntry(
    versionString: String,
    onReopen: () -> Unit,
) {
    BisqText.SmallMedium(
        text = "Version $versionString available — tap to view",
        color = BisqTheme.colors.primary,
        modifier =
            Modifier
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding2X,
                    vertical = BisqUIConstants.ScreenPaddingHalf,
                ).clickable { onReopen() }
                .testTag("resources_dismissed_update_entry"),
    )
}

// -------------------------------------------------------------------------------------
// Previews
// -------------------------------------------------------------------------------------

/** Card in context, sitting between the dashboard's top stat row and its hero CTA card. */
@ExcludeFromCoverage
@Preview(name = "1. Card — on the dashboard, in context", heightDp = 900)
@Composable
private fun ReleaseUpdateCard_OnDashboard_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            HomeInfoCard(price = "111247.40 BTC/USD", text = "Market price")
            ReleaseUpdateCard(
                uiState = simulatedReleaseUpdateUiState(),
                onAction = {},
            )
            DashBoardCard(
                title = "Start trading",
                bulletPoints =
                    listOf(
                        Pair("Browse the offerbook", Res.drawable.icon_markets),
                        Pair("Chat with your trade partner", Res.drawable.icon_chat_circle),
                        Pair("Build your reputation", Res.drawable.reputation),
                    ),
                buttonText = "Go to Offerbook",
                buttonHandler = {},
            )
        }
    }
}

/** Card — Play install, update available, not started. */
@ExcludeFromCoverage
@Preview(name = "2. Card — Play, available")
@Composable
private fun ReleaseUpdateCard_PlayAvailable_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            ReleaseUpdateCard(
                uiState = simulatedReleaseUpdateUiState(),
                onAction = {},
            )
        }
    }
}

/** Card — Play download in progress, whole-number percent, dismiss icon hidden. */
@ExcludeFromCoverage
@Preview(name = "3. Card — Play, downloading")
@Composable
private fun ReleaseUpdateCard_PlayDownloading_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            ReleaseUpdateCard(
                uiState =
                    simulatedReleaseUpdateUiState(
                        playPhase = PlayUpdatePhase.DOWNLOADING,
                        downloadProgress = 0.62f,
                    ),
                onAction = {},
            )
        }
    }
}

/** Card — Play download failed; dismiss is available again, "Try again" retries. */
@ExcludeFromCoverage
@Preview(name = "4. Card — Play, download failed")
@Composable
private fun ReleaseUpdateCard_PlayFailed_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            ReleaseUpdateCard(
                uiState = simulatedReleaseUpdateUiState(playPhase = PlayUpdatePhase.FAILED),
                onAction = {},
            )
        }
    }
}

/** Card — ready to restart, Bisq Easy (node): title + body, explicit that the node restarts. */
@ExcludeFromCoverage
@Preview(name = "5. Card — Play, ready to restart (node)")
@Composable
private fun ReleaseUpdateCard_PlayReadyNode_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            ReleaseUpdateCard(
                uiState = simulatedReleaseUpdateUiState(isNode = true, playPhase = PlayUpdatePhase.READY_TO_RESTART),
                onAction = {},
            )
        }
    }
}

/** Card — ready to restart, Bisq Connect: single-line, app-only restart. */
@ExcludeFromCoverage
@Preview(name = "6. Card — Play, ready to restart (Connect)")
@Composable
private fun ReleaseUpdateCard_PlayReadyConnect_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            ReleaseUpdateCard(
                uiState = simulatedReleaseUpdateUiState(isNode = false, playPhase = PlayUpdatePhase.READY_TO_RESTART),
                onAction = {},
            )
        }
    }
}

/**
 * Card — every external-link fallback install source stacked together (GitHub, F-Droid, iOS
 * TestFlight, iOS AltStore PAL); none of these show a Play progress/restart state.
 */
@ExcludeFromCoverage
@Preview(name = "7. Card — external-link fallback, all install sources", heightDp = 900)
@Composable
private fun ReleaseUpdateCard_ExternalLinkGallery_Preview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            listOf(
                InstallSource.GITHUB,
                InstallSource.FDROID,
                InstallSource.IOS_TESTFLIGHT,
                InstallSource.IOS_ALTSTORE_PAL,
            ).forEach { source ->
                BisqText.SmallLight(source.name, color = BisqTheme.colors.mid_grey30)
                ReleaseUpdateCard(
                    uiState =
                        simulatedReleaseUpdateUiState(
                            installSource = source,
                            playPhase = PlayUpdatePhase.NOT_APPLICABLE,
                        ),
                    onAction = {},
                )
                BisqGap.VHalf()
            }
        }
    }
}

/**
 * Card — mandatory update, Play install: dismiss hidden, danger border/badge/notice, primary
 * button unchanged ("Update"). Renders identically whether or not this exact version was
 * previously dismissed — see "7. Mandatory updates."
 */
@ExcludeFromCoverage
@Preview(name = "8. Card — mandatory, Play install")
@Composable
private fun ReleaseUpdateCard_MandatoryPlay_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            ReleaseUpdateCard(
                uiState = simulatedReleaseUpdateUiState(isMandatory = true, minVersionString = "0.11.0"),
                onAction = {},
            )
        }
    }
}

/** Card — mandatory update, external-link fallback (GitHub); same treatment regardless of install source. */
@ExcludeFromCoverage
@Preview(name = "9. Card — mandatory, external-link fallback (GitHub)")
@Composable
private fun ReleaseUpdateCard_MandatoryExternalLink_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            ReleaseUpdateCard(
                uiState =
                    simulatedReleaseUpdateUiState(
                        installSource = InstallSource.GITHUB,
                        playPhase = PlayUpdatePhase.NOT_APPLICABLE,
                        isMandatory = true,
                        minVersionString = "0.11.0",
                    ),
                onAction = {},
            )
        }
    }
}

/** Sheet — Play install, primary action starts the update. */
@ExcludeFromCoverage
@Preview(name = "10. Sheet — Play", heightDp = 700)
@Composable
private fun ReleaseNotesSheet_Stable_Preview() {
    BisqTheme.Preview {
        ReleaseNotesSheet(
            uiState = simulatedReleaseUpdateUiState(),
            onAction = {},
        )
    }
}

/** Sheet — external-link fallback (GitHub); primary action opens GitHub. */
@ExcludeFromCoverage
@Preview(name = "11. Sheet — external-link fallback (GitHub)", heightDp = 700)
@Composable
private fun ReleaseNotesSheet_ExternalLink_Preview() {
    BisqTheme.Preview {
        ReleaseNotesSheet(
            uiState =
                simulatedReleaseUpdateUiState(
                    installSource = InstallSource.GITHUB,
                    playPhase = PlayUpdatePhase.NOT_APPLICABLE,
                ),
            onAction = {},
        )
    }
}

/** Sheet — release notes at the maximum length the notification field allows (~10,000 characters). */
@ExcludeFromCoverage
@Preview(name = "12. Sheet — long release notes (~10k chars, real ceiling)", heightDp = 700)
@Composable
private fun ReleaseNotesSheet_LongNotes_Preview() {
    BisqTheme.Preview {
        ReleaseNotesSheet(
            uiState = simulatedReleaseUpdateUiState(releaseNotes = longReleaseNotesSample()),
            onAction = {},
        )
    }
}

/** Sheet — mandatory update: badge + notice shown, "Ignore this version" link omitted entirely. */
@ExcludeFromCoverage
@Preview(name = "13. Sheet — mandatory, ignore link hidden", heightDp = 700)
@Composable
private fun ReleaseNotesSheet_Mandatory_Preview() {
    BisqTheme.Preview {
        ReleaseNotesSheet(
            uiState = simulatedReleaseUpdateUiState(isMandatory = true, minVersionString = "0.11.0"),
            onAction = {},
        )
    }
}

/**
 * Long-locale simulation: German text runs longer than English. Headline still truncates
 * gracefully; notice text wraps rather than clipping.
 */
@ExcludeFromCoverage
@Preview(name = "14. Card — simulated long-locale text", heightDp = 900)
@Composable
private fun ReleaseUpdateCard_LongLocaleText_Preview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.SmallLight("Simulated German locale, ready-to-restart (node):", color = BisqTheme.colors.mid_grey20)
            ReleaseUpdateCard(
                uiState =
                    simulatedReleaseUpdateUiState(
                        versionString = "0.11.0",
                        isNode = true,
                        playPhase = PlayUpdatePhase.READY_TO_RESTART,
                    ),
                onAction = {},
            )
        }
    }
}

/** Resources screen — the dismissed-update line under the version string, tap to reopen the sheet. */
@ExcludeFromCoverage
@Preview(name = "15. Resources screen — re-finding a dismissed update")
@Composable
private fun DismissedUpdateResourcesEntry_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            BisqText.H3Light("Version", color = BisqTheme.colors.light_grey50)
            BisqText.BaseLight(
                text = "v0.10.0 (128)",
                color = BisqTheme.colors.mid_grey20,
                modifier =
                    Modifier.padding(
                        vertical = BisqUIConstants.ScreenPaddingHalf,
                        horizontal = BisqUIConstants.ScreenPadding2X,
                    ),
            )
            DismissedUpdateResourcesEntry(versionString = "0.11.0", onReopen = {})
        }
    }
}
