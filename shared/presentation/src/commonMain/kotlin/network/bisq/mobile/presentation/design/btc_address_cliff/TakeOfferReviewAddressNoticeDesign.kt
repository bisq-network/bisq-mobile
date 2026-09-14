/**
 * TakeOfferReviewAddressNoticeDesign.kt — Design PoC (Issue #1816)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * Phase 1 "announce" treatment: a notice card on the take-offer REVIEW step, shown only
 * when the taker is the BUYER, that sets expectations about the upcoming BTC/LN address
 * requirement before the trade is committed. Three content variants depending on what
 * the wizard already knows at review time:
 *
 *   1. MAINCHAIN, no address collected  → forward-looking announcement + wallet-guide CTA
 *   2. MAINCHAIN, address collected     → confirmation, not a warning — the buyer already
 *                                          did the work in the new address step, so this
 *                                          reassures rather than repeats the ask
 *   3. LIGHTNING                        → announcement only; Lightning never gets early
 *                                          collection (invoices carry amount + expiry),
 *                                          so this variant always reads like #1's tone,
 *                                          never #2's
 *
 * Insertion point in TakeOfferReviewContent (see review/TakeOfferReviewScreen.kt): between
 * the top amount InfoBox/InfoRowContainer block and the BisqHDivider that precedes the
 * price/fee section. That is the first thing a buyer's eye reaches after confirming the
 * numbers are right, and before the divider that visually closes off "the deal" — a
 * natural place for "one more thing before you commit" without competing with the
 * amounts for primary attention.
 *
 * ======================================================================================
 * VETERAN VISIBILITY (recommendation)
 * ======================================================================================
 * Mainchain: HIDE this notice entirely for a profile that has completed a trade before.
 * They have been through BuyerState1a already, they know an address is coming, and by
 * construction they never saw the early-entry step either — showing a card here with
 * nothing new to say would just be clutter on a screen whose whole job is "does this
 * offer look right." Zero added friction, per the same rule that gates the new wizard
 * step to first-timers only.
 *
 * Lightning: keep a notice for EVERYONE, veterans included, but only the compact form —
 * see LightningAddressNotice's doc comment below for why LN is treated differently from
 * mainchain veterans.
 *
 * ======================================================================================
 * I18N KEYS NEEDED
 * ======================================================================================
 * All copy below is hardcoded English for the PoC. Production keys (mobile.properties):
 *   mobile.takeOffer.review.addressNotice.mainchain.announce.headline
 *       → "You'll need a Bitcoin address"
 *   mobile.takeOffer.review.addressNotice.mainchain.announce.body
 *       → "Once the seller confirms, you'll be asked for a receiving address to get
 *          your bitcoin."
 *   mobile.takeOffer.review.addressNotice.mainchain.confirmed.headline
 *       → "Your payout address is ready"
 *   mobile.takeOffer.review.addressNotice.mainchain.confirmed.body
 *       → "You entered this when you took the offer. You'll get a chance to review it
 *          again before it's sent."
 *   mobile.takeOffer.review.addressNotice.mainchain.confirmed.editAction → "Edit"
 *   mobile.takeOffer.review.addressNotice.lightning.headline
 *       → "You'll need a Lightning invoice"
 *   mobile.takeOffer.review.addressNotice.lightning.body
 *       → "Lightning invoices expire quickly, so you'll create yours once the seller
 *          confirms and you're ready to receive payment."
 *   mobile.takeOffer.review.addressNotice.walletGuideAction → "Open the wallet guide"
 */
package network.bisq.mobile.presentation.design.btc_address_cliff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqCard
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.CheckCircleIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.InfoGreenIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

/**
 * Variant 1 — mainchain, no address collected yet. Forward-looking, non-alarming: this
 * is information the buyer needs, not a problem they caused. The wallet-guide CTA is the
 * same "Open the wallet guide" button used in the new address step and in BuyerState1a,
 * so a buyer who skipped the early step still has one more, low-pressure chance to go
 * prepare before committing.
 */
@Composable
private fun MainchainAnnounceNotice(onOpenWalletGuide: () -> Unit) {
    BisqCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            InfoGreenIcon()
            Column(modifier = Modifier.weight(1f)) {
                BisqText.H6Light("You'll need a Bitcoin address")
                BisqGap.VHalf()
                BisqText.BaseLightGrey(
                    "Once the seller confirms, you'll be asked for a receiving address " +
                        "to get your bitcoin.",
                )
                BisqGap.V1()
                BisqButton(
                    text = "Open the wallet guide",
                    onClick = onOpenWalletGuide,
                    type = BisqButtonType.Outline,
                )
            }
        }
    }
}

/**
 * Variant 2 — mainchain, address already collected in the new wizard step. This is a
 * CONFIRMATION, not a repeat of the ask: the buyer already did the work, so the copy and
 * icon (green check, not the info dot used above) reward that instead of nagging again.
 * The truncated address is shown so the buyer can eyeball it a second time here, and
 * "Edit" is a lightweight escape hatch back to the address step — since that step
 * immediately precedes Review whenever it was shown, this can be wired to the same
 * onBack() the wizard already uses; no new navigation primitive needed.
 */
@Composable
private fun MainchainConfirmedNotice(
    truncatedAddress: String,
    onEditAddress: () -> Unit,
) {
    BisqCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            CheckCircleIcon()
            Column(modifier = Modifier.weight(1f)) {
                BisqText.H6Light("Your payout address is ready")
                BisqGap.VHalf()
                BisqText.BaseLightGrey(
                    "You entered this when you took the offer. You'll get a chance to " +
                        "review it again before it's sent.",
                )
                BisqGap.VHalf()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BisqText.SmallRegular(truncatedAddress, color = BisqTheme.colors.mid_grey20)
                    BisqButton(
                        text = "Edit",
                        onClick = onEditAddress,
                        type = BisqButtonType.Underline,
                    )
                }
            }
        }
    }
}

/**
 * Variant 3 — Lightning. Always announce-only (LN never gets early collection), and
 * unlike the mainchain announce card, this one stays visible for veterans too — kept
 * deliberately compact (single row, no CTA) rather than hidden outright. Reasoning:
 * mainchain veterans get nothing here because there is nothing new to tell them, but the
 * LN timing constraint (invoice expiry) is operationally relevant EVERY time, not just
 * the first time — it is a reminder about the trade's mechanics, not a beginner
 * explainer, so it earns a permanent but unobtrusive spot rather than full hiding.
 */
@Composable
private fun LightningAddressNotice() {
    BisqCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InfoGreenIcon()
            Column {
                BisqText.H6Light("You'll need a Lightning invoice")
                BisqGap.VHalf()
                BisqText.BaseLightGrey(
                    "Lightning invoices expire quickly, so you'll create yours once the " +
                        "seller confirms and you're ready to receive payment.",
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// @Preview functions — standalone cards, shown at the width/padding they'd have inside
// TakeOfferReviewContent's Column (BisqUIConstants.ScreenPadding2X gaps).
// -------------------------------------------------------------------------------------

@ExcludeFromCoverage
@Preview
@Composable
private fun ReviewAddressNotice_MainchainWithoutAddressPreview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            MainchainAnnounceNotice(onOpenWalletGuide = {})
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun ReviewAddressNotice_MainchainWithAddressPreview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            MainchainConfirmedNotice(
                truncatedAddress = "bc1qw50…f3t4",
                onEditAddress = {},
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun ReviewAddressNotice_LightningPreview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            LightningAddressNotice()
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun ReviewAddressNotice_AllVariantsPreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X),
        ) {
            MainchainAnnounceNotice(onOpenWalletGuide = {})
            MainchainConfirmedNotice(truncatedAddress = "bc1qw50…f3t4", onEditAddress = {})
            LightningAddressNotice()
        }
    }
}
