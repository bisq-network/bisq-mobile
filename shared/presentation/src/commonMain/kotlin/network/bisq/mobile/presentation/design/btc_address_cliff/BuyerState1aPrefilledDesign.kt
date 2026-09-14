/**
 * BuyerState1aPrefilledDesign.kt — Design PoC (Issue #1816)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * BuyerState1a variant for when the address field arrives PRE-FILLED from the take-offer
 * wizard's optional address step, instead of starting blank as it does today. The value
 * is never auto-sent — the user still has to review it and tap "Send to seller" exactly
 * as they do now. This file is only about how the screen communicates "this was
 * pre-filled, please actually look at it" without the pre-fill turning into a rubber
 * stamp.
 *
 * ======================================================================================
 * THE RUBBER-STAMP RISK, AND WHY A BADGE ALONE DOESN'T FIX IT
 * ======================================================================================
 * A pre-filled field with a normal Send button invites exactly the failure mode this
 * design exists to prevent: a user who took the offer days ago, half-remembers entering
 * "some address," and taps Send without really re-reading it. A one-line badge like
 * "Pre-filled from when you took the offer" raises awareness but does not change
 * behaviour — awareness copy is passive, and the whole point of this PoC's parent issue
 * is that passive copy is what got us here in the first place (announce-only Phase 1
 * exists for cheap cases; this is not one — it is the step where funds actually move).
 *
 * So pre-fill gets one additional, active gate: a checkbox — "I've checked this is my
 * correct address" — that must be ticked before Send enables, ONLY when the value on
 * screen still matches what the wizard pre-filled. This forces a distinct motor action
 * (tapping a checkbox) that is separate from the act of having typed the address
 * days earlier, which is the actual gap this design needs to close.
 *
 * The gate lifts itself the moment the user edits the value at all: editing IS an active
 * re-engagement with the address, so re-requiring the checkbox on top of that would be
 * pure friction with no safety benefit. This also means the gate never applies to the
 * CURRENT production behaviour (user types fresh, mid-trade) — it activates only for the
 * new pre-filled path, so it adds zero friction for anyone not touched by this feature.
 *
 * ======================================================================================
 * VISUAL TREATMENT
 * ======================================================================================
 * - A small badge row above the field (info icon + "Pre-filled from when you took the
 *   offer") sets context before the user's eye reaches the field itself.
 * - The field is otherwise IDENTICAL to today's BuyerState1a — same BitcoinLnAddressField,
 *   same QR/paste affordances, fully editable. Pre-fill is not a locked or read-only
 *   state; the user can scan a different code or paste over it just as easily as typing
 *   from scratch.
 * - The checkbox appears only while wasPrefilled is true, directly above the action row,
 *   so it reads as the last thing standing between the user and Send — not as a distant
 *   disclaimer.
 * - Send stays disabled (existing behaviour: empty or invalid address) AND gains one more
 *   condition: disabled while wasPrefilled && !hasConfirmedPrefill.
 *
 * ======================================================================================
 * PRESENTER CONTRACT (for implementation)
 * ======================================================================================
 * BuyerState1aPresenter gains:
 *   - wasPrefilled: StateFlow<Boolean> — true if the trade's address was seeded from the
 *     take-offer model and the current field value has not been edited since
 *   - hasConfirmedPrefill: StateFlow<Boolean> — the checkbox state, reset to false
 *     whenever a new trade attaches
 *   - onBitcoinPaymentDataInput(value, isValid) additionally sets wasPrefilled = false
 *     the moment value != the originally-seeded value
 *   - onConfirmPrefillChange(Boolean) toggles hasConfirmedPrefill
 *   - isSendBitcoinPaymentDataEnabled's existing guard gains:
 *     `&& (!wasPrefilled || hasConfirmedPrefill)`
 *
 * ======================================================================================
 * I18N KEYS NEEDED
 * ======================================================================================
 * All copy below is hardcoded English for the PoC. Production keys (mobile.properties):
 *   bisqEasy.tradeState.info.buyer.phase1a.prefilled.badge
 *       → "Pre-filled from when you took the offer"
 *   bisqEasy.tradeState.info.buyer.phase1a.prefilled.confirmCheckbox
 *       → "I've checked this is my correct address"
 */
package network.bisq.mobile.presentation.design.btc_address_cliff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqCheckbox
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.InfoGreenIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BitcoinLnAddressField
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BitcoinLnAddressFieldType
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.spaceBetweenWithMin

@Composable
private fun PrefilledBadge() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InfoGreenIcon()
        BisqText.SmallRegular("Pre-filled from when you took the offer")
    }
}

/**
 * Stateless content mirroring BuyerState1a's structure, with the prefill badge and the
 * conditional confirmation checkbox layered in. `wasPrefilled` and `hasConfirmedPrefill`
 * are hoisted so a future presenter can own them exactly like every other flag here.
 */
@Composable
private fun BuyerState1aPrefilledContent(
    bitcoinPaymentData: String,
    wasPrefilled: Boolean,
    hasConfirmedPrefill: Boolean,
    onValueChange: (String, Boolean) -> Unit,
    onConfirmPrefillChange: (Boolean) -> Unit,
    onBarcodeClick: () -> Unit,
    onOpenWalletGuide: () -> Unit,
    onSendClick: () -> Unit,
    isValid: Boolean,
) {
    val sendDisabled =
        bitcoinPaymentData.isEmpty() ||
            !isValid ||
            (wasPrefilled && !hasConfirmedPrefill)

    Column {
        BisqGap.V1()
        BisqText.H5Light("Fill in your Bitcoin address")

        if (wasPrefilled) {
            BisqGap.V1()
            PrefilledBadge()
        }

        BisqGap.V1()
        BitcoinLnAddressField(
            label = "Bitcoin address",
            value = bitcoinPaymentData,
            onValueChange = onValueChange,
            type = BitcoinLnAddressFieldType.Bitcoin,
            onBarcodeClick = onBarcodeClick,
        )

        if (wasPrefilled) {
            BisqGap.V1()
            BisqCheckbox(
                checked = hasConfirmedPrefill,
                label = "I've checked this is my correct address",
                onCheckedChange = onConfirmPrefillChange,
            )
        }

        BisqGap.V1()

        Row(
            horizontalArrangement = Arrangement.spaceBetweenWithMin(BisqUIConstants.ScreenPadding),
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        ) {
            BisqButton(
                text = "Send to seller",
                onClick = onSendClick,
                disabled = sendDisabled,
                modifier = Modifier.fillMaxHeight(),
            )
            BisqButton(
                text = "Open wallet guide",
                onClick = onOpenWalletGuide,
                type = BisqButtonType.Outline,
                padding =
                    PaddingValues(
                        horizontal = BisqUIConstants.ScreenPadding,
                        vertical = BisqUIConstants.ScreenPaddingHalf,
                    ),
                modifier = Modifier.fillMaxHeight(),
            )
        }
    }
}

/**
 * Interactive shell: tracks the original pre-filled value so editing it can drop
 * `wasPrefilled` automatically, exactly as the presenter contract above describes.
 */
@Composable
private fun BuyerState1aPrefilledScreen(
    seededAddress: String,
    initiallyConfirmed: Boolean = false,
) {
    var value by remember { mutableStateOf(seededAddress) }
    var isValid by remember { mutableStateOf(true) }
    var wasPrefilled by remember { mutableStateOf(seededAddress.isNotBlank()) }
    var hasConfirmedPrefill by remember { mutableStateOf(initiallyConfirmed) }

    BuyerState1aPrefilledContent(
        bitcoinPaymentData = value,
        wasPrefilled = wasPrefilled,
        hasConfirmedPrefill = hasConfirmedPrefill,
        onValueChange = { newValue, valid ->
            if (newValue != seededAddress) {
                wasPrefilled = false
            }
            value = newValue
            isValid = valid
        },
        onConfirmPrefillChange = { hasConfirmedPrefill = it },
        onBarcodeClick = { /* preview no-op: would open QR scanner */ },
        onOpenWalletGuide = { /* preview no-op: would navigateTo(WalletGuideIntro) */ },
        onSendClick = { /* preview no-op: would call buyerSendBitcoinPaymentData() */ },
        isValid = isValid,
    )
}

// -------------------------------------------------------------------------------------
// @Preview functions
// -------------------------------------------------------------------------------------

@ExcludeFromCoverage
@Preview
@Composable
private fun BuyerState1aPrefilled_UnconfirmedPreview() {
    BisqTheme.Preview {
        BuyerState1aPrefilledScreen(
            seededAddress = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun BuyerState1aPrefilled_ConfirmedPreview() {
    BisqTheme.Preview {
        BuyerState1aPrefilledScreen(
            seededAddress = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
            initiallyConfirmed = true,
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun BuyerState1aPrefilled_NotPrefilledPreview() {
    // Baseline: no address was collected at take-offer time, so this renders exactly
    // like today's production BuyerState1a — no badge, no checkbox, no added friction.
    BisqTheme.Preview {
        BuyerState1aPrefilledScreen(seededAddress = "")
    }
}
