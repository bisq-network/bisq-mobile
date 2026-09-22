/**
 * BuyerState2aBannedAccountDesign.kt — Design PoC (Issue #1867)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code. All preview
 * data flows through [simulatedBannedAccountUiState], which takes only primitives.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * Bisq2 Desktop checks the seller's payment account data against the security manager's
 * banned-account-data list right when it is shown to the buyer, and blocks payment if it
 * matches — [bisq.bisq_easy.BisqEasyService.isAccountDataBanned]. Mobile maps the
 * `BANNED_ACCOUNT_DATA` alert type
 * (`apps/nodeApp/.../mapping/alert/AlertTypeBisq2Mapping.kt`) but nothing ever calls the
 * check, so a mobile buyer can pay a flagged account with no warning at all. This is not a
 * generic security alert either: `AlertType.kt`'s own KDoc says
 * "BAN and BANNED_ACCOUNT_DATA are not surfaced as UI alerts on mobile" — by design, this
 * needs a check at the point of decision (right before payment), not a banner in the
 * app-wide alert feed.
 *
 * Production reference: `shared/presentation/.../trade_detail/states/buyer_state_2/state_a/
 * BuyerState2a.kt` + `BuyerState2aPresenter.kt`. Desktop reference:
 * `bisq2/apps/desktop/desktop/.../trade_state/states/BuyerState2a.java`.
 *
 * ======================================================================================
 * DESKTOP BEHAVIOR — MIRRORED IN FULL (decided 2026-09-22)
 * ======================================================================================
 * Desktop's `Controller.onActivate()` does FOUR things when `isAccountDataBanned` is true:
 *   1. Disables the confirm-payment button (`confirmFiatSentButtonDisabled`).
 *   2. Marks the account-data field invalid via a `SettableErrorValidator`.
 *   3. Reports the peer to the moderator (`moderationRequestService.reportUserProfile`)
 *      with the message "Account data of <peer> is banned: <account data>".
 *   4. Cancels the trade (`bisqEasyTradeService.cancelTrade(trade)`), then shows a blocking
 *      `Popup.warning(...)` whose text states that 3 and 4 happened — see
 *      `bisqEasy.tradeState.info.buyer.phase2a.accountDataBanned.popup.warning` (already
 *      synced into mobile's `shared/domain/.../resources/mobile/bisq_easy.properties`,
 *      line 737, and translated into all 14 locales).
 *
 * Mobile does all four. Full behavioural parity was chosen over a warn-only variant: the
 * buyer is protected by the trade being cancelled, not by a warning they might ignore, and
 * the moderator report is what gets the banned peer removed from the network. Because the
 * side effects really happen, the dialog reuses the existing translated popup string
 * verbatim.
 *
 * ======================================================================================
 * WHERE THE CHECK RUNS AND WHAT IT TRIGGERS
 * ======================================================================================
 * [BannedAccountUiState.isAccountDataBanned] stands in for a new check in
 * `BuyerState2aPresenter`, run once `paymentAccountData` is non-null (Node app only — see
 * "CONNECT BUILDS"): `bisqEasyService.isAccountDataBanned(sellersAccountData)`, held in a
 * `StateFlow<Boolean>`. When it is `true` the presenter, once per trade:
 *   - forces `isConfirmFiatSentEnabled` to `false` (banned always wins over the existing
 *     in-flight-action guard; nothing re-enables it for this trade);
 *   - calls `UserProfileServiceFacade.reportUserProfile(peer, message)` with desktop's
 *     message format;
 *   - calls `TradesServiceFacade.cancelTrade(reason)`; add a dedicated
 *     `AnalyticsEvent.Trade.InterruptReason` (e.g. `BANNED_ACCOUNT_DATA`) so the analytics
 *     do not attribute an automatic cancel to one of the user-chosen reasons;
 *   - shows [BannedAccountWarningDialog].
 *
 * Once the cancel propagates, the trade-detail screen switches to the interrupted-trade
 * pane as it does for any cancelled trade, so layers 2 and 3 below are visible only in the
 * window before that switch, and as a fallback if the cancel call fails. They are kept
 * because that window is exactly when the buyer may still be copying the account data.
 * The side effects must be idempotent per trade: guard them with an "already handled"
 * flag so a re-activation (rotation, back navigation, app restart before the cancel
 * landed) does not report the peer twice or cancel an already-cancelled trade.
 * "Until acknowledged" in the issue's acceptance criteria describes the ENTRY gate (the
 * blocking dialog), not a re-enable path — acknowledging only dismisses the dialog.
 *
 * ======================================================================================
 * WARNING SURFACE — WHY THREE LAYERS, NOT ONE
 * ======================================================================================
 * The design brief asked to weigh "inline block vs. blocking dialog vs. both." This PoC
 * uses three, deliberately overlapping layers, because each catches a different way the
 * buyer could miss a single one on a screen this short (which does not itself scroll much,
 * but the buyer's attention is on copying account data into a banking app, not reading this
 * screen carefully):
 *
 *   1. [BannedAccountWarningDialog] — a blocking, non-dismiss-on-outside-tap dialog shown
 *      once per screen activation while banned (mirrors desktop's `Popup.warning()` on
 *      every `onActivate`). This is the interrupt: it cannot be scrolled past or missed on
 *      first arrival. Single "I understand" button — see "CHECKBOX VS. DIALOG" below.
 *
 *   2. Field-level error on the existing "Payment account of seller" [BisqTextFieldV0]:
 *      `isError = true` + `bottomMessage` = the existing, short
 *      `bisqEasy.tradeState.info.buyer.phase2a.accountDataBannedError` string. This is the
 *      mobile equivalent of desktop's `SettableErrorValidator` on the same field — same
 *      data, same field, same moment (the issue's "last moment the buyer can still back
 *      out" is reading this exact field before pasting it into their banking app).
 *      [BisqTextFieldV0]'s error styling (red bottom indicator, red label, red message) all
 *      persist even while `enabled = false`, so the disabled/read-only account field still
 *      reads as dangerous, not just inert.
 *
 *   3. [BannedAccountInlineBanner] — a persistent (non-dismissible) red banner placed
 *      directly above the confirm-payment button, i.e. the last thing the buyer's eye
 *      passes before tapping it. Unlike the dialog (shown once, then gone) or the field
 *      error (explains WHY), this is the ONE surface that also says WHAT TO DO: do not pay,
 *      cancel the trade or open mediation from the trade header menu above. Placing this
 *      right above the button — instead of, say, under the headline — means the button's
 *      own disabled state and its reason are read together, so "why can't I tap this" is
 *      never a mystery. No caption is added directly under the button itself: that would
 *      just repeat this banner one line later for zero benefit.
 *
 * All three persist for as long as the trade stays banned; only the dialog's visibility is
 * dismissible (via acknowledgement), and only until the next activation.
 *
 * ======================================================================================
 * CHECKBOX VS. DIALOG ACKNOWLEDGEMENT
 * ======================================================================================
 * The brief asked whether acknowledgement should be an explicit checkbox (as used
 * elsewhere in this codebase, e.g. the take-offer address pre-fill confirm-gate) or a
 * dialog "I understand" action. A checkbox is the right pattern when the user is
 * acknowledging a risk in order to still proceed (that's what it means everywhere else it
 * is used here). That is NOT this situation: there is no legitimate way to proceed — paying
 * a fraud-flagged account should never be unblocked by anything the buyer does. A single
 * non-optional "I understand" action on a dialog that cannot be dismissed by tapping
 * outside communicates the correct thing: acknowledge this, there is nothing to choose.
 * Reuses the existing [ConfirmationDialog] single-button convention already established by
 * [network.bisq.mobile.presentation.common.ui.components.organisms.dialogs.TradeFailureDialog]
 * and [network.bisq.mobile.presentation.trade.trade_detail.InterruptedTradePane]'s
 * `WarningConfirmationDialog` usage (`dismissButtonText = EMPTY_STRING`,
 * `dismissOnClickOutside = false`).
 *
 * ======================================================================================
 * COLOR AND ICON
 * ======================================================================================
 * `BisqTheme.colors.danger` (red), not `.warning` (orange). Per this codebase's existing
 * color semantics (see agent memory on the trade-status-out-of-sync work and
 * `AlertNotificationCommonUi.kt`'s `alertAccentColor`/`alertBannerBackground`): warning/
 * orange is for caution states, `danger`/red is reserved for the most severe tier
 * (`AlertType.EMERGENCY`'s halt-trading flag already uses it). A confirmed fraud flag on
 * the exact account the buyer is about to pay is that severe tier, not a caution. The
 * banner's background/border recipe (`danger.copy(alpha = 0.15f)` fill, 3dp `danger`
 * border, [BisqUIConstants.BorderRadius]) is copied verbatim from
 * `AlertNotificationBannerContent`'s `EMERGENCY` treatment for visual consistency with the
 * one other place this app already renders "this is the worst severity tier." Icon:
 * [ExclamationRedIcon] (existing atom, already red, already used by
 * [network.bisq.mobile.presentation.common.ui.components.organisms.dialogs.TradeFailureDialog])
 * — no new icon asset needed. No new color token needed.
 *
 * ======================================================================================
 * COPY
 * ======================================================================================
 * The dialog reuses desktop's popup string verbatim (already translated): it explains the
 * ban, says not to pay, and states that the peer was reported and the trade cancelled.
 * The field error and the banner headline reuse the existing short
 * `accountDataBannedError` string. The only new string is the banner's action line, and it
 * must not claim more than has happened at render time: the cancel is still in flight when
 * the banner first appears, so it says "is being cancelled", not "has been cancelled".
 *
 * ======================================================================================
 * ACCESSIBILITY
 * ======================================================================================
 * - [ExclamationRedIcon] carries its own (non-localized, pre-existing) content description
 *   from the shared icon atom; not something this PoC introduces or can fix here.
 * - Neither the banner nor the field error message is line-capped or ellipsized — unlike
 *   [network.bisq.mobile.presentation.common.ui.alert.banner.AlertNotificationBanner]'s
 *   2-line-ellipsis treatment for routine alerts, truncating a fraud warning would be
 *   actively harmful. Both wrap freely, so the ~30-40% longer German/Russian strings just
 *   take another line rather than losing words — see
 *   [BuyerState2aBanned_LongLocaleText_Preview].
 * - The banner itself is NOT a tap target (no `clickable`, no dismiss action) — it is
 *   informational only, so there is no touch-target sizing concern for it. The dialog's
 *   single button and the (disabled) confirm button both reuse the existing [BisqButton]
 *   atom, already touch-target compliant.
 * - `Modifier.testTag(...)` is used on the banner and the confirm button for automated
 *   tests, per repo convention — no `semantics { contentDescription = }` blocks are added.
 *
 * ======================================================================================
 * CONNECT BUILDS
 * ======================================================================================
 * `BisqEasyService.isAccountDataBanned` runs entirely in-process on the node — Connect has
 * no API for it yet. [BannedAccountUiState.isAccountDataBanned] must default to `false` on
 * `ClientTradesServiceFacade` until a Connect endpoint exists, so `BuyerState2a` behaves
 * exactly as it does today on Connect builds — this PoC's warning never fires there. Do not
 * gate the whole feature behind a `BackendCapabilitiesService` probe for v1: unlike
 * optional features, silently disabled security UI should not need a version check to be
 * safe — it already IS today's (unsafe) status quo on Connect, unchanged. `cancelTrade`
 * and `reportUserProfile` already exist on `ClientTradesServiceFacade` /
 * `ClientUserProfileServiceFacade`, so once the node API exposes the banned check the same
 * presenter code applies to Connect with no further UI work.
 *
 * ======================================================================================
 * PROPOSED I18N KEYS (English base only, per repo convention — nothing added to
 * mobile.properties by this PoC; production implementation adds these)
 * ======================================================================================
 * New:
 *   mobile.tradeState.info.buyer.phase2a.accountDataBanned.banner.action
 *     → "Do not send any payment. This trade is being cancelled for your protection and
 *        the seller has been reported to the moderator."
 *
 * Reused, already present in `shared/domain/.../resources/mobile/`:
 *   bisqEasy.tradeState.info.buyer.phase2a.accountDataBanned.popup.warning (dialog body)
 *   bisqEasy.tradeState.info.buyer.phase2a.accountDataBannedError
 *     ("The seller's account data has been used in fraudulent activities" — field
 *     bottomMessage + banner headline)
 *   popup.headline.warning ("Warning" — dialog headline)
 *   action.iUnderstand ("I understand" — dialog button)
 *
 * ======================================================================================
 * IMPLEMENTATION NOTES FOR THE DEVELOPER
 * ======================================================================================
 * - Add `isAccountDataBanned: StateFlow<Boolean>` (Node-backed, `false` on Client — see
 *   "CONNECT BUILDS") to `TradesServiceFacade`, populated once `paymentAccountData` is
 *   known, mirroring desktop's `Controller.onActivate()` check.
 * - `BuyerState2aPresenter`: fold that flow into `isConfirmFiatSentEnabled` (banned always
 *   wins over the existing in-flight-action guard); when it flips to `true` and the trade
 *   is not yet handled, run the side effects in order — `reportUserProfile(peer, message)`
 *   via `UserProfileServiceFacade`, then `cancelTrade(BANNED_ACCOUNT_DATA)` via
 *   `TradesServiceFacade` — and set `showBannedAccountWarningDialog = true`; a new
 *   `onAcknowledgeBannedAccountWarning()` sets it back to `false` and does NOT re-enable
 *   the confirm button. Log and surface a failure of either call; do not swallow it.
 * - `BuyerState2a.kt`: pass `isError`/`bottomMessage` into the existing sellers-account
 *   `BisqTextFieldV0` call when banned (replacing, not stacking with, today's
 *   `reasonForPaymentInfo` bottomMessage — the fraud warning takes priority over the
 *   reason-for-payment hint in that one shared slot); render [BannedAccountInlineBanner]
 *   between that field and the confirm button; render [BannedAccountWarningDialog]
 *   conditionally.
 * - Presenter test: fake a banned result and assert confirm disabled, report + cancel called
 *   exactly once across two activations, dialog shown then dismissed on acknowledge.
 * - No `TODO()` calls in this file; no Koin injection; no `IXxxPresenter` interface.
 */
package network.bisq.mobile.presentation.design.trade_banned_account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.button.CopyIconButton
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ExclamationRedIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// -------------------------------------------------------------------------------------
// MVIP sketch (design package only — production shape lives in BuyerState2aPresenter)
// -------------------------------------------------------------------------------------

/**
 * Stands in for the slice of `BuyerState2aPresenter` state this design touches. Everything
 * else on the real screen (e.g. in-flight-action guarding on the confirm button) is
 * unchanged and not modeled here.
 */
internal data class BannedAccountUiState(
    val quoteAmountWithCode: String,
    val sellersAccountData: String,
    val tradeId: String,
    val isAccountDataBanned: Boolean,
    val isWarningDialogVisible: Boolean,
)

internal sealed interface BannedAccountUiAction {
    data object AcknowledgeBannedAccountWarning : BannedAccountUiAction

    data object ConfirmFiatSent : BannedAccountUiAction
}

/** Builds a [BannedAccountUiState] from primitives with realistic defaults. */
internal fun simulatedBannedAccountUiState(
    quoteAmountWithCode: String = "250.00 EUR",
    sellersAccountData: String = "DE89 3704 0044 0532 0130 00\nAccount owner: Max Mustermann",
    tradeId: String = "8f3ac210",
    isAccountDataBanned: Boolean = false,
    isWarningDialogVisible: Boolean = false,
): BannedAccountUiState =
    BannedAccountUiState(
        quoteAmountWithCode = quoteAmountWithCode,
        sellersAccountData = sellersAccountData,
        tradeId = tradeId,
        isAccountDataBanned = isAccountDataBanned,
        isWarningDialogVisible = isWarningDialogVisible,
    )

// -------------------------------------------------------------------------------------
// Layer 1 — blocking entry dialog
// -------------------------------------------------------------------------------------

/**
 * Blocking warning shown once per screen activation while the seller's account data is
 * banned. Single "I understand" action, not dismissible by tapping outside — see
 * "CHECKBOX VS. DIALOG ACKNOWLEDGEMENT" in the file KDoc. [message] defaults to desktop's
 * existing, already-translated popup string, which is accurate because the report and
 * cancel side effects run before the dialog is shown.
 */
@Composable
internal fun BannedAccountWarningDialog(
    onAcknowledge: () -> Unit,
    message: String = DEFAULT_BANNED_ACCOUNT_DIALOG_MESSAGE,
    headline: String = "Warning",
    confirmButtonText: String = "I understand",
) {
    ConfirmationDialog(
        headline = headline,
        headlineColor = BisqTheme.colors.danger,
        headlineLeftIcon = { ExclamationRedIcon() },
        message = message,
        confirmButtonText = confirmButtonText,
        dismissButtonText = EMPTY_STRING,
        dismissOnClickOutside = false,
        onConfirm = onAcknowledge,
        onDismiss = { onAcknowledge() },
    )
}

/** Existing `bisqEasy.tradeState.info.buyer.phase2a.accountDataBanned.popup.warning`. */
private const val DEFAULT_BANNED_ACCOUNT_DIALOG_MESSAGE =
    "The seller's account data has been banned due to fraudulent activities.\n\nDo not " +
        "send any money to this account!\n\nThe peer got reported to the moderator and " +
        "will soon get banned from the network.\n\nFor your protection, this trade has " +
        "been marked as canceled. You may now safely close the trade.\n\nIf you have any " +
        "questions or need further assistance, please visit the support chat."

// -------------------------------------------------------------------------------------
// Layer 3 — persistent inline banner (placed directly above the confirm button)
// -------------------------------------------------------------------------------------

/**
 * Persistent, non-dismissible danger banner. Unlike the dialog (shown once) or the field
 * error (explains why), this is the surface that also says what is happening. [headline]
 * reuses the existing `accountDataBannedError` string; [action] is the proposed new
 * `banner.action` key.
 */
@Composable
internal fun BannedAccountInlineBanner(
    modifier: Modifier = Modifier,
    headline: String = DEFAULT_BANNED_ACCOUNT_BANNER_HEADLINE,
    action: String = DEFAULT_BANNED_ACCOUNT_BANNER_ACTION,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color = BisqTheme.colors.danger.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                ).border(
                    width = 3.dp,
                    color = BisqTheme.colors.danger,
                    shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                ).padding(BisqUIConstants.ScreenPadding)
                .testTag("banned_account_banner"),
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        verticalAlignment = Alignment.Top,
    ) {
        ExclamationRedIcon()
        Column {
            BisqText.SmallMedium(
                text = headline,
                color = BisqTheme.colors.danger,
            )
            BisqGap.VQuarter()
            BisqText.SmallLight(
                text = action,
                color = BisqTheme.colors.light_grey10,
            )
        }
    }
}

/** Existing `bisqEasy.tradeState.info.buyer.phase2a.accountDataBannedError`. */
private const val DEFAULT_BANNED_ACCOUNT_BANNER_HEADLINE =
    "The seller's account data has been used in fraudulent activities"

/** Proposed `mobile.tradeState.info.buyer.phase2a.accountDataBanned.banner.action`. */
private const val DEFAULT_BANNED_ACCOUNT_BANNER_ACTION =
    "Do not send any payment. This trade is being cancelled for your protection and the " +
        "seller has been reported to the moderator."

// -------------------------------------------------------------------------------------
// Full pane — redesigned BuyerState2a content
// -------------------------------------------------------------------------------------

/**
 * Redesigned BuyerState2a content. Identical to today's screen when
 * [BannedAccountUiState.isAccountDataBanned] is `false`. When `true`: Layer 2 (field
 * error) replaces the account field's normal `reasonForPaymentInfo` bottomMessage, Layer 3
 * ([BannedAccountInlineBanner]) sits between that field and the confirm button, the confirm
 * button is disabled unconditionally, and Layer 1 ([BannedAccountWarningDialog]) renders on
 * top while [BannedAccountUiState.isWarningDialogVisible] is true.
 */
@Composable
internal fun BuyerState2aBannedAccountPane(
    uiState: BannedAccountUiState,
    onAction: (BannedAccountUiAction) -> Unit,
) {
    val banned = uiState.isAccountDataBanned

    Column(horizontalAlignment = Alignment.Start) {
        BisqGap.V1()
        BisqText.H5Light("Send ${uiState.quoteAmountWithCode} to the seller's payment account")

        BisqGap.VHalf()
        BisqTextFieldV0(
            label = "Amount to transfer",
            value = uiState.quoteAmountWithCode,
            enabled = false,
            trailingIcon = { CopyIconButton(value = uiState.quoteAmountWithCode) },
        )

        BisqGap.VHalf()
        BisqTextFieldV0(
            label = "Payment account of seller",
            bottomMessage =
                if (banned) {
                    DEFAULT_BANNED_ACCOUNT_BANNER_HEADLINE
                } else {
                    "Use the trade ID ${uiState.tradeId} for the 'Reason for payment' field"
                },
            isError = banned,
            value = uiState.sellersAccountData,
            enabled = false,
            trailingIcon = { CopyIconButton(value = uiState.sellersAccountData) },
            maxLines = Int.MAX_VALUE,
            minLines = 2,
        )

        if (banned) {
            BisqGap.V1()
            BannedAccountInlineBanner()
        }

        BisqGap.V1()
        BisqButton(
            text = "Confirm payment of ${uiState.quoteAmountWithCode}",
            onClick = { onAction(BannedAccountUiAction.ConfirmFiatSent) },
            disabled = banned,
            modifier = Modifier.testTag("confirm_fiat_sent_button"),
        )
    }

    if (banned && uiState.isWarningDialogVisible) {
        BannedAccountWarningDialog(
            onAcknowledge = { onAction(BannedAccountUiAction.AcknowledgeBannedAccountWarning) },
        )
    }
}

// -------------------------------------------------------------------------------------
// Previews
// -------------------------------------------------------------------------------------

/** 1. Baseline — not banned, screen unchanged from today. */
@ExcludeFromCoverage
@Preview(name = "1. Not banned — unchanged")
@Composable
private fun BuyerState2aBanned_NotBanned_Preview() {
    BisqTheme.Preview {
        BuyerState2aBannedAccountPane(
            uiState = simulatedBannedAccountUiState(),
            onAction = {},
        )
    }
}

/** 2. Banned, first activation — blocking dialog on top, confirm already disabled behind it. */
@ExcludeFromCoverage
@Preview(name = "2. Banned — entry dialog, not yet acknowledged", heightDp = 900)
@Composable
private fun BuyerState2aBanned_DialogVisible_Preview() {
    BisqTheme.Preview {
        BuyerState2aBannedAccountPane(
            uiState =
                simulatedBannedAccountUiState(
                    isAccountDataBanned = true,
                    isWarningDialogVisible = true,
                ),
            onAction = {},
        )
    }
}

/** 3. Banned, acknowledged — dialog dismissed; field error + banner + disabled button remain. */
@ExcludeFromCoverage
@Preview(name = "3. Banned — acknowledged, confirm stays disabled")
@Composable
private fun BuyerState2aBanned_Acknowledged_Preview() {
    BisqTheme.Preview {
        BuyerState2aBannedAccountPane(
            uiState =
                simulatedBannedAccountUiState(
                    isAccountDataBanned = true,
                    isWarningDialogVisible = false,
                ),
            onAction = {},
        )
    }
}

/**
 * 4. Dialog in isolation, for quick review without the surrounding pane. A `Dialog` window
 * on its own gives the preview a zero-size root and Android Studio reports a render problem,
 * so a small host column sits underneath it (same trick as the push-notification PoC).
 */
@ExcludeFromCoverage
@Preview(name = "4. Dialog — standalone", heightDp = 900)
@Composable
private fun BuyerState2aBanned_DialogStandalone_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.SmallLight(
                "Blocking dialog shown once per activation while banned:",
                color = BisqTheme.colors.light_grey10,
            )
        }
        BannedAccountWarningDialog(onAcknowledge = {})
    }
}

/** 5. Inline banner in isolation. */
@ExcludeFromCoverage
@Preview(name = "5. Banner — standalone")
@Composable
private fun BuyerState2aBanned_BannerStandalone_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            BannedAccountInlineBanner()
        }
    }
}

/**
 * 6. Long-locale simulation — German/Russian strings run ~30-40% longer than English
 * (see file KDoc). Both the field error and the banner wrap onto extra lines instead of
 * truncating; nothing clips.
 */
@ExcludeFromCoverage
@Preview(name = "6. Banned — simulated long-locale text")
@Composable
private fun BuyerState2aBanned_LongLocaleText_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            BannedAccountInlineBanner(
                headline =
                    "Die Kontodaten des Verkäufers wurden im Zusammenhang mit " +
                        "betrügerischen Aktivitäten verwendet und gemeldet",
                action =
                    "Senden Sie keine Zahlung. Dieser Handel wird zu Ihrem Schutz abgebrochen " +
                        "und der Verkäufer wurde dem Moderator gemeldet.",
            )
        }
    }
}
