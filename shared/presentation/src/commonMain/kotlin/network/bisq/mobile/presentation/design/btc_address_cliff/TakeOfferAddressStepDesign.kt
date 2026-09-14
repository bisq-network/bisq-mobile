/**
 * TakeOfferAddressStepDesign.kt — Design PoC (Issue #1816)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * Reduces the BTC-address cliff: 64% of taken trades (Aug 2026 funnel) reached
 * btc_address_confirmed, meaning over a third of buyers stalled or bailed at the
 * mid-trade address prompt. This is Phase 2 of the two-phase fix — an OPTIONAL,
 * skippable step inserted into the take-offer wizard, right after the settlement-method
 * step, so a first-time buyer can prepare their receiving address (or realize they need
 * a wallet and go get one) BEFORE they are mid-trade with a counterparty waiting.
 *
 * Shown only when ALL of the following hold:
 *   - the taker is the BUYER (only buyers ever need to provide a BTC/LN address)
 *   - the resolved base-side settlement method is MAINCHAIN, not Lightning (LN invoices
 *     carry amount + expiry — collecting one here would let it expire before the trade
 *     even starts, so LN never gets this step; see Phase 1 announce-only treatment)
 *   - the profile has never completed a trade before (veterans already know the drill;
 *     zero added friction for them)
 *
 * The collected address is committed to the take-offer model and used to pre-fill
 * BuyerState1a mid-trade — it is NEVER auto-sent. The user still reviews and taps
 * "Send to seller" there. See BuyerState1aPrefilledDesign.kt for that half.
 *
 * ======================================================================================
 * WHY NO SEPARATE "SKIP" BUTTON
 * ======================================================================================
 * A dedicated "I'll do it later" button competing for attention with "I have a wallet —
 * enter address" would (a) add a decision novices have to parse before they've even seen
 * the field, and (b) risk reading as a fork where skipping feels like the "wrong" branch.
 * Instead this step reuses the wizard's existing Next button, which already means
 * "continue" everywhere else in the flow:
 *
 *   - Field empty            → Next reads "Skip for now", always enabled, always advances
 *   - Field filled + valid   → Next reads "Continue", advances and commits the address
 *   - Field filled + invalid → Next reads "Continue" but is DISABLED; the field already
 *                              shows its own inline error. The user has two obvious ways
 *                              forward: fix the address, or clear the field to skip.
 *
 * Skip is therefore always exactly one tap away and never buried — it just IS the Next
 * button when there is nothing to submit. This directly serves the "skip must be
 * frictionless" constraint: a mandatory-feeling step would only move the abandonment
 * cliff from mid-trade to here.
 *
 * Deliberately NOT done: blocking progress entirely on invalid input the way
 * BuyerState1a's "proceed anyway" dialog does for the real protocol send. That dialog
 * exists because sending a bad address to the peer has real consequences. Here the data
 * never leaves the device, so the bar is just "don't let obviously-garbage input coast
 * through into the pre-fill" — a disabled button plus the field's existing red state is
 * enough, no interruptive modal needed.
 *
 * ======================================================================================
 * COMPONENT REUSE
 * ======================================================================================
 * Reuses BitcoinLnAddressField exactly as BuyerState1a does (same QR scan button, same
 * paste affordance, same validation, same "you can find help at the wallet guide" helper
 * copy baked into the field). Type is hard-pinned to Bitcoin — this step never renders
 * for Lightning trades, so BitcoinLnAddressFieldType.Lightning never applies here.
 *
 * The "Open wallet guide" button below the field mirrors BuyerState1a's outline button
 * verbatim (same label key, same in-app destination: NavRoute.WalletGuideIntro, the
 * existing 4-step in-app guide — not an external URL). Reusing the same button in the
 * same visual slot means a buyer who saw this step and a buyer who first meets the
 * wallet guide mid-trade land on an identical, already-familiar affordance.
 *
 * ======================================================================================
 * WIZARD STEP-COUNT NUANCE (flag for engineering)
 * ======================================================================================
 * TakeOfferCoordinator currently fixes totalSteps once, in selectOfferToTake(), before
 * any wizard screen renders — every existing conditional step (amount range, multiple
 * quote/base payment methods) is knowable from the OFFER alone. This step is different:
 * whether it appears depends on which settlement method the user PICKS, which in the
 * rare case of an offer supporting both mainchain and Lightning is only known once the
 * settlement screen's Next is tapped. Recommendation: resolve this exactly like the
 * other show*Screen() flags for the common case (single settlement method resolved
 * up-front, no dynamic step), and add a late "+1 to totalSteps" the instant the
 * settlement choice commits to mainchain, mirroring how commitSettlementMethod() already
 * runs at that seam. The step-progress bar's segment count changing between the
 * settlement and address screens (e.g. "3 of 4" render finishing, "4 of 5" starting) is
 * a minor, one-time visual seam, not a navigation bug — no back-and-forth crosses it more
 * than once per trade.
 *
 * ======================================================================================
 * I18N KEYS NEEDED
 * ======================================================================================
 * All copy below is hardcoded English for the PoC. Production keys (mobile.properties):
 *   mobile.takeOffer.addressStep.progress.title       → "Bitcoin address"
 *   mobile.takeOffer.addressStep.headline             → "Where should the bitcoin go?"
 *   mobile.takeOffer.addressStep.body                 → "Optional — add your receiving
 *                                                         address now so it's ready when
 *                                                         your trade starts. You can
 *                                                         always do this later from the
 *                                                         trade screen."
 *   mobile.takeOffer.addressStep.fieldLabel           → "Your Bitcoin address"
 *   mobile.takeOffer.addressStep.next.skip            → "Skip for now"
 *   mobile.takeOffer.addressStep.next.continue        → "Continue"
 *   (wallet guide button reuses bisqEasy.tradeState.info.buyer.phase1a.walletHelpButton)
 *
 * Note for translators: German/Russian run 30-40% longer than English — the body copy
 * above is intentionally short (two sentences) to leave room before it wraps to a third
 * line on compact widths.
 */
package network.bisq.mobile.presentation.design.btc_address_cliff

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BitcoinLnAddressField
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BitcoinLnAddressFieldType
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

/**
 * Stateless content for the new step, hoisted so it can be dropped straight into a real
 * presenter later (state would live in a TakeOfferAddressPresenter mirroring
 * TakeOfferPaymentMethodPresenter's shape: value + isValid StateFlows, an onNext() that
 * commits to TakeOfferCoordinator only when isValid, an onSkip() identical to onNext()
 * when the field is blank).
 */
@Composable
private fun TakeOfferAddressStepContent(
    value: String,
    onValueChange: (String, Boolean) -> Unit,
    onBarcodeClick: () -> Unit,
    onOpenWalletGuide: () -> Unit,
) {
    Column {
        BisqGap.V1()
        BisqText.H3Light("Where should the bitcoin go?")

        BisqGap.V1()
        BisqText.BaseLight(
            "Optional — add your receiving address now so it's ready when your trade " +
                "starts. You can always do this later from the trade screen.",
        )

        BisqGap.V2()
        BitcoinLnAddressField(
            label = "Your Bitcoin address",
            value = value,
            onValueChange = onValueChange,
            type = BitcoinLnAddressFieldType.Bitcoin,
            onBarcodeClick = onBarcodeClick,
        )

        BisqGap.V2()
        BisqButton(
            text = "Open the wallet guide",
            onClick = onOpenWalletGuide,
            type = BisqButtonType.Outline,
            fullWidth = true,
        )
    }
}

/**
 * Full wizard shell, mirroring how other take-offer steps host MultiScreenWizardScaffold.
 * The Next button's label and enabled-state are entirely driven by the field's value —
 * see the class doc comment for the empty / valid / invalid state table.
 */
@Composable
private fun TakeOfferAddressStepScreen(
    initialValue: String = "",
) {
    var value by remember { mutableStateOf(initialValue) }
    var isValid by remember { mutableStateOf(false) }

    val nextLabel = if (value.isBlank()) "Skip for now" else "Continue"
    val nextDisabled = value.isNotBlank() && !isValid

    MultiScreenWizardScaffold(
        title = "Bitcoin address",
        stepIndex = 4,
        stepsLength = 5,
        nextButtonText = nextLabel,
        nextDisabled = nextDisabled,
        nextOnClick = { /* preview no-op: would commit address + navigateTo(Review) */ },
        prevOnClick = { /* preview no-op: would navigateBack() */ },
        closeAction = true,
    ) {
        TakeOfferAddressStepContent(
            value = value,
            onValueChange = { newValue, valid ->
                value = newValue
                isValid = valid
            },
            onBarcodeClick = { /* preview no-op: would open QR scanner */ },
            onOpenWalletGuide = { /* preview no-op: would navigateTo(WalletGuideIntro) */ },
        )
    }
}

// -------------------------------------------------------------------------------------
// @Preview functions
// -------------------------------------------------------------------------------------

@ExcludeFromCoverage
@Preview
@Composable
private fun TakeOfferAddressStep_EmptyPreview() {
    BisqTheme.Preview {
        TakeOfferAddressStepScreen(initialValue = "")
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun TakeOfferAddressStep_FilledValidPreview() {
    BisqTheme.Preview {
        TakeOfferAddressStepScreen(initialValue = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4")
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun TakeOfferAddressStep_ValidationErrorPreview() {
    BisqTheme.Preview {
        TakeOfferAddressStepScreen(initialValue = "not-a-real-address")
    }
}
