/**
 * TradePaymentAccountSelectionDesign.kt — Design PoC (Issue #315)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * Redesign of SellerState1: during a sell trade the seller must send their payment
 * account data to the buyer. The current screen shows a flat dropdown of ALL accounts
 * with no filtering. This PoC improves that with three contextual scenarios depending
 * on how many accounts match the trade's payment method and currency.
 *
 * Three scenarios:
 *   1. Exactly one matching account  → auto-fill with compact "Change" affordance
 *   2. Multiple matching accounts    → selectable compact account chips/cards
 *   3. No matching accounts          → inline prompt + "Create account" link
 *
 * ======================================================================================
 * SCENARIOS
 * ======================================================================================
 *
 * SCENARIO 1 — Single match (happy path):
 *   The data text field is pre-filled with the account's data. Above the field a compact
 *   info row shows: payment method icon + account name + "Change" link. Tapping "Change"
 *   replaces the row with Scenario 2's chip selector inline. This keeps the common case
 *   as frictionless as possible — one tap to send.
 *
 * SCENARIO 2 — Multiple matches:
 *   Compact account chips stacked vertically (up to ~4 visible before scrolling). Each
 *   chip shows: payment method icon + account name + masked account data. The selected
 *   chip gets a green border. The selected account's data fills the text field below.
 *   Vertical stacking rather than horizontal scrolling: payment data rows are text-heavy
 *   and need space; horizontal chips would be too narrow for the masked data snippet.
 *
 * SCENARIO 3 — No matches:
 *   An inline warning surface explains that no saved account matches the trade's payment
 *   method and currency. A "Create account" button navigates to payment account settings.
 *   The text field remains editable for manual entry so the trade is never blocked.
 *
 * ======================================================================================
 * TRADE CONTEXT HEADER
 * ======================================================================================
 * A compact context row at the top of the account selection block shows the payment
 * method icon + method name + currency. This gives the seller immediate confirmation
 * that the filtering is correct before they send data.
 *
 * ======================================================================================
 * MASKING
 * ======================================================================================
 * Account data is partially masked in the selection chips to protect privacy while
 * still letting the user distinguish between accounts. The masking rules:
 *   - Email-like strings: show first 2 chars + *** + @domain masked to @***.tld
 *   - IBAN-like strings: show first 4 + *** + last 4 chars
 *   - Everything else: show first 4 + *** + last 4 (or full if short)
 *
 * ======================================================================================
 * I18N CONSIDERATIONS
 * ======================================================================================
 * New keys needed:
 *   mobile.tradeState.seller.phase1.selectedAccount.change
 *       → "Change"
 *   mobile.tradeState.seller.phase1.noMatchingAccount.info
 *       → "No saved {0} account for {1}"
 *   mobile.tradeState.seller.phase1.noMatchingAccount.createAction
 *       → "Create account"
 *   mobile.tradeState.seller.phase1.methodContext
 *       → "{0} · {1}" (method name + currency)
 *   mobile.tradeState.seller.phase1.selectAccount.label
 *       → "Select account"
 *
 * Existing keys reused:
 *   bisqEasy.tradeState.info.seller.phase1.headline
 *   bisqEasy.tradeState.info.seller.phase1.accountData
 *   bisqEasy.tradeState.info.seller.phase1.buttonText
 *
 * ======================================================================================
 * IMPLEMENTATION NOTES
 * ======================================================================================
 * - Filtering matching accounts is done in the presenter. The view receives only the
 *   pre-filtered list and the scenario is derived from its size (0, 1, or 2+).
 * - The "Change" expanded state is local UI state (no presenter round-trip needed).
 * - During implementation, SimulatedAccountEntry maps to UserDefinedFiatAccount and
 *   its accountPayload.accountData field.
 * - The text field stays fully editable in all scenarios. The selection pre-fills it
 *   but the seller can always override (e.g. to add extra routing details).
 */
package network.bisq.mobile.presentation.design.payment_accounts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqCard
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.molecules.PaymentMethodIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// -------------------------------------------------------------------------------------
// Simulated domain types (primitives only — no presenter dependency)
// -------------------------------------------------------------------------------------

/**
 * Represents a single saved payment account that matches the trade's payment method.
 * During implementation, maps to UserDefinedFiatAccount + its accountPayload.accountData.
 */
data class SimulatedAccountEntry(
    val accountName: String,
    val methodId: String,
    val accountData: String,
)

// -------------------------------------------------------------------------------------
// Masking helper
// -------------------------------------------------------------------------------------

/**
 * Masks the middle portion of account data for display in selection chips.
 *
 * Examples:
 *   "user@example.com"        -> "us***@***.com"
 *   "DE89370400440532013000"  -> "DE89***3000"
 *   "+1234567890"             -> "+123***7890"
 */
fun simulatedMaskAccountData(data: String): String {
    if (data.length <= 8) return data

    val atIndex = data.indexOf('@')
    if (atIndex > 0) {
        // Email-like: show first 2 chars + *** + masked domain
        val localPart = data.take(2) + "***"
        val domainPart = data.substring(atIndex) // e.g. "@example.com"
        val dotIndex = domainPart.lastIndexOf('.')
        val maskedDomain =
            if (dotIndex > 1) {
                "@***" + domainPart.substring(dotIndex) // e.g. "@***.com"
            } else {
                "@***"
            }
        return localPart + maskedDomain
    }

    // IBAN / phone / default: show first 4 + *** + last 4
    return data.take(4) + "***" + data.takeLast(4)
}

// -------------------------------------------------------------------------------------
// Trade context header
// -------------------------------------------------------------------------------------

/**
 * Compact header showing the trade's payment method and currency for confirmation.
 * Prevents the seller from accidentally sending the wrong account data.
 */
@Composable
private fun TradeMethodContextRow(
    paymentMethodId: String,
    paymentMethodName: String,
    currency: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        PaymentMethodIcon(
            methodId = paymentMethodId,
            isPaymentMethod = true,
            size = 28.dp,
            contentDescription = paymentMethodName,
        )
        BisqText.BaseRegular(
            text = "$paymentMethodName · $currency",
            color = BisqTheme.colors.mid_grey30,
        )
    }
}

// -------------------------------------------------------------------------------------
// Scenario 1: Single account — auto-fill with "Change" affordance
// -------------------------------------------------------------------------------------

/**
 * Compact info row shown when exactly one account matches the trade's payment method.
 * Shows: method icon + account name + "Change" link.
 * Tapping "Change" calls [onChangeRequest] to reveal the multi-account selector.
 */
@Composable
private fun SingleAccountInfoRow(
    account: SimulatedAccountEntry,
    onChangeRequest: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        PaymentMethodIcon(
            methodId = account.methodId,
            isPaymentMethod = true,
            size = 22.dp,
            contentDescription = account.methodId,
        )
        BisqText.SmallRegular(
            text = account.accountName,
            color = BisqTheme.colors.mid_grey30,
        )
        BisqGap.HHalf()
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
            BisqButton(
                text = "Change",
                type = BisqButtonType.Underline,
                onClick = onChangeRequest,
                padding =
                    PaddingValues(
                        horizontal = BisqUIConstants.Zero,
                        vertical = BisqUIConstants.Zero,
                    ),
            )
        }
    }
}

// -------------------------------------------------------------------------------------
// Scenario 2: Multiple accounts — chip selector
// -------------------------------------------------------------------------------------

/**
 * Compact selectable chip for one account in the multi-account selector.
 *
 * Selected state: green border (BisqTheme.colors.primary) + slightly lighter background.
 * Unselected state: default card background, no border.
 *
 * Shows: method icon + account name + masked account data snippet.
 */
@Composable
private fun AccountSelectionChip(
    account: SimulatedAccountEntry,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val border =
        if (isSelected) {
            BorderStroke(width = 1.5.dp, color = BisqTheme.colors.primary)
        } else {
            null
        }
    val background =
        if (isSelected) {
            BisqTheme.colors.dark_grey50
        } else {
            BisqTheme.colors.dark_grey40
        }

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = background,
        border = border,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPaddingHalf,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            PaymentMethodIcon(
                methodId = account.methodId,
                isPaymentMethod = true,
                size = 28.dp,
                contentDescription = account.methodId,
            )
            Column(modifier = Modifier.weight(1f)) {
                BisqText.SmallMedium(
                    text = account.accountName,
                    color = if (isSelected) BisqTheme.colors.white else BisqTheme.colors.mid_grey30,
                )
                BisqGap.VQuarter()
                BisqText.XSmallLight(
                    text = simulatedMaskAccountData(account.accountData),
                    color = BisqTheme.colors.mid_grey20,
                )
            }
            if (isSelected) {
                BisqText.SmallRegular(
                    text = "✓",
                    color = BisqTheme.colors.primary,
                )
            }
        }
    }
}

/**
 * Stacked list of selectable account chips.
 * Used both for Scenario 2 and for the expanded selector revealed by Scenario 1's "Change".
 */
@Composable
private fun AccountChipSelector(
    accounts: List<SimulatedAccountEntry>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        BisqText.SmallLightGrey(text = "Select account")
        accounts.forEachIndexed { index, account ->
            AccountSelectionChip(
                account = account,
                isSelected = index == selectedIndex,
                onClick = { onSelect(index) },
            )
        }
    }
}

// -------------------------------------------------------------------------------------
// Scenario 3: No matching accounts
// -------------------------------------------------------------------------------------

/**
 * Inline prompt when no saved accounts match the trade's payment method and currency.
 * Shows a warning surface with explanatory text and a "Create account" CTA.
 * The text field below remains editable for manual entry.
 */
@Composable
private fun NoMatchingAccountPrompt(
    paymentMethodName: String,
    currency: String,
    onCreateAccount: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = BisqTheme.colors.warning.copy(alpha = 0.10f),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = BisqUIConstants.ScreenPadding,
                        vertical = BisqUIConstants.ScreenPadding,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BisqText.SmallRegular(
                    text = "No saved $paymentMethodName account for $currency",
                    color = BisqTheme.colors.warning,
                )
                BisqGap.VHalf()
                BisqText.XSmallLight(
                    text = "You can enter your account data manually below, or save an account first.",
                    color = BisqTheme.colors.mid_grey30,
                )
            }
        }
    }
    BisqGap.VHalf()
    BisqButton(
        text = "Create account",
        type = BisqButtonType.GreyOutline,
        onClick = onCreateAccount,
        fullWidth = true,
    )
}

// -------------------------------------------------------------------------------------
// Unified account selection block
// -------------------------------------------------------------------------------------

/**
 * The account selection block that adapts to three scenarios based on [matchingAccounts].
 *
 * Scenario derivation:
 *   size == 0  → Scenario 3: no match prompt
 *   size == 1  → Scenario 1: auto-fill with "Change" affordance
 *   size >= 2  → Scenario 2: chip selector
 *
 * [selectedIndex] is the index into [matchingAccounts] that is currently selected.
 * [onSelect] is called when the user picks a different account.
 * [onCreateAccount] is called when the user taps "Create account" in Scenario 3.
 */
@Composable
fun TradeAccountSelectionBlock(
    matchingAccounts: List<SimulatedAccountEntry>,
    selectedIndex: Int,
    paymentMethodId: String,
    paymentMethodName: String,
    currency: String,
    onSelect: (Int) -> Unit,
    onCreateAccount: () -> Unit,
) {
    // Local state: whether Scenario 1's "Change" has been tapped to reveal selector
    var changeExpanded by remember { mutableStateOf(false) }

    Column {
        TradeMethodContextRow(
            paymentMethodId = paymentMethodId,
            paymentMethodName = paymentMethodName,
            currency = currency,
        )

        BisqGap.V1()

        when {
            matchingAccounts.isEmpty() -> {
                NoMatchingAccountPrompt(
                    paymentMethodName = paymentMethodName,
                    currency = currency,
                    onCreateAccount = onCreateAccount,
                )
            }

            matchingAccounts.size == 1 && !changeExpanded -> {
                SingleAccountInfoRow(
                    account = matchingAccounts[0],
                    onChangeRequest = { changeExpanded = true },
                )
            }

            else -> {
                // size >= 2, or size == 1 with "Change" tapped
                AccountChipSelector(
                    accounts = matchingAccounts,
                    selectedIndex = selectedIndex,
                    onSelect = onSelect,
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// Full redesigned SellerState1 screen content
// -------------------------------------------------------------------------------------

/**
 * Redesigned SellerState1 content. Replaces the flat dropdown with the contextual
 * [TradeAccountSelectionBlock]. The layout otherwise mirrors the current screen:
 * headline → context/selection → data field → send button.
 *
 * [accountData] is the current text field value (pre-filled or manually entered).
 * [onAccountDataChange] is called on every keystroke.
 * [onSendPaymentData] is called when the seller confirms.
 */
@Composable
fun TradePaymentAccountSelectionContent(
    matchingAccounts: List<SimulatedAccountEntry>,
    selectedIndex: Int,
    accountData: String,
    paymentMethodId: String,
    paymentMethodName: String,
    currency: String,
    onSelect: (Int) -> Unit,
    onAccountDataChange: (String) -> Unit,
    onCreateAccount: () -> Unit,
    onSendPaymentData: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = BisqUIConstants.ScreenPadding),
    ) {
        BisqGap.V1()
        BisqText.H5Light(text = "Send your payment account data to the buyer")

        BisqGap.V1()
        BisqText.BaseLightGrey(
            text = "Fill in your payment account data. E.g. IBAN, BIC and account owner name",
        )

        BisqHDivider(verticalPadding = BisqUIConstants.ScreenPadding)

        TradeAccountSelectionBlock(
            matchingAccounts = matchingAccounts,
            selectedIndex = selectedIndex,
            paymentMethodId = paymentMethodId,
            paymentMethodName = paymentMethodName,
            currency = currency,
            onSelect = onSelect,
            onCreateAccount = onCreateAccount,
        )

        BisqGap.V1()

        BisqTextFieldV0(
            label = "My payment account data",
            value = accountData,
            onValueChange = onAccountDataChange,
            minLines = 3,
        )

        BisqGap.V1()

        BisqButton(
            text = "Send account data",
            onClick = onSendPaymentData,
            fullWidth = true,
            disabled = accountData.isBlank(),
        )
    }
}

// -------------------------------------------------------------------------------------
// Preview data
// -------------------------------------------------------------------------------------

private val wiseAccount =
    SimulatedAccountEntry(
        accountName = "Wise Multi-Currency",
        methodId = "WISE",
        accountData = "seller@gmail.com",
    )

private val sepaAccount1 =
    SimulatedAccountEntry(
        accountName = "My SEPA Account",
        methodId = "SEPA",
        accountData = "DE89370400440532013000",
    )

private val sepaAccount2 =
    SimulatedAccountEntry(
        accountName = "Sparkasse EUR",
        methodId = "SEPA",
        accountData = "DE02200400600093340300",
    )

private val sepaAccount3 =
    SimulatedAccountEntry(
        accountName = "N26 EUR",
        methodId = "SEPA",
        accountData = "DE75512108001245126199",
    )

private val previewOnSelect: (Int) -> Unit = {}
private val previewOnDataChange: (String) -> Unit = {}
private val previewOnCreate: () -> Unit = {}
private val previewOnSend: () -> Unit = {}

// -------------------------------------------------------------------------------------
// Previews
// -------------------------------------------------------------------------------------

/**
 * Preview 1: Scenario 1 — single matching account (Wise + EUR).
 * The data field is pre-filled; seller sees their account name with "Change" link.
 */
@ExcludeFromCoverage
@Preview(name = "1. Single account auto-fill (Wise + EUR)")
@Composable
private fun SingleAccount_AutoFill_Preview() {
    BisqTheme.Preview {
        TradePaymentAccountSelectionContent(
            matchingAccounts = listOf(wiseAccount),
            selectedIndex = 0,
            accountData = wiseAccount.accountData,
            paymentMethodId = "WISE",
            paymentMethodName = "Wise",
            currency = "EUR",
            onSelect = previewOnSelect,
            onAccountDataChange = previewOnDataChange,
            onCreateAccount = previewOnCreate,
            onSendPaymentData = previewOnSend,
        )
    }
}

/**
 * Preview 2: Scenario 1 with "Change" expanded — single account but selector revealed.
 * Simulated by passing a list with one account rendered via the multi-account path.
 * In production, this state is driven by local var changeExpanded = true.
 */
@ExcludeFromCoverage
@Preview(name = "2. Single account — Change expanded (selector revealed)")
@Composable
private fun SingleAccount_ChangeExpanded_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqGap.V1()
            BisqText.H5Light(text = "Send your payment account data to the buyer")
            BisqHDivider(verticalPadding = BisqUIConstants.ScreenPadding)
            // Simulating changeExpanded = true by calling AccountChipSelector directly
            BisqCard {
                Column {
                    TradeMethodContextRow(
                        paymentMethodId = "WISE",
                        paymentMethodName = "Wise",
                        currency = "EUR",
                    )
                    BisqGap.V1()
                    AccountChipSelector(
                        accounts = listOf(wiseAccount),
                        selectedIndex = 0,
                        onSelect = previewOnSelect,
                    )
                }
            }
            BisqGap.V1()
            BisqTextFieldV0(
                label = "My payment account data",
                value = wiseAccount.accountData,
                onValueChange = previewOnDataChange,
                minLines = 3,
            )
            BisqGap.V1()
            BisqButton(
                text = "Send account data",
                onClick = previewOnSend,
                fullWidth = true,
            )
        }
    }
}

/**
 * Preview 3: Scenario 2 — multiple SEPA accounts, none selected.
 * Shows all three chips in deselected state.
 */
@ExcludeFromCoverage
@Preview(name = "3. Multiple accounts — none selected (SEPA + EUR)")
@Composable
private fun MultipleAccounts_NoneSelected_Preview() {
    BisqTheme.Preview {
        TradePaymentAccountSelectionContent(
            matchingAccounts = listOf(sepaAccount1, sepaAccount2, sepaAccount3),
            selectedIndex = -1,
            accountData = "",
            paymentMethodId = "SEPA",
            paymentMethodName = "SEPA",
            currency = "EUR",
            onSelect = previewOnSelect,
            onAccountDataChange = previewOnDataChange,
            onCreateAccount = previewOnCreate,
            onSendPaymentData = previewOnSend,
        )
    }
}

/**
 * Preview 4: Scenario 2 — multiple SEPA accounts, second one selected.
 * Green border on selected chip, data field pre-filled with masked data shown unmasked.
 */
@ExcludeFromCoverage
@Preview(name = "4. Multiple accounts — one selected (SEPA + EUR)")
@Composable
private fun MultipleAccounts_OneSelected_Preview() {
    BisqTheme.Preview {
        TradePaymentAccountSelectionContent(
            matchingAccounts = listOf(sepaAccount1, sepaAccount2, sepaAccount3),
            selectedIndex = 1,
            accountData = sepaAccount2.accountData,
            paymentMethodId = "SEPA",
            paymentMethodName = "SEPA",
            currency = "EUR",
            onSelect = previewOnSelect,
            onAccountDataChange = previewOnDataChange,
            onCreateAccount = previewOnCreate,
            onSendPaymentData = previewOnSend,
        )
    }
}

/**
 * Preview 5: Scenario 3 — no matching accounts for Zelle + USD.
 * Shows warning surface + "Create account" button; text field is empty and editable.
 */
@ExcludeFromCoverage
@Preview(name = "5. No matching accounts (Zelle + USD)")
@Composable
private fun NoMatchingAccounts_Preview() {
    BisqTheme.Preview {
        TradePaymentAccountSelectionContent(
            matchingAccounts = emptyList(),
            selectedIndex = -1,
            accountData = "",
            paymentMethodId = "ZELLE",
            paymentMethodName = "Zelle",
            currency = "USD",
            onSelect = previewOnSelect,
            onAccountDataChange = previewOnDataChange,
            onCreateAccount = previewOnCreate,
            onSendPaymentData = previewOnSend,
        )
    }
}

/**
 * Preview 6: Full screen — single Wise account, ready to send.
 * Shows the complete redesigned SellerState1 flow from headline to button.
 */
@ExcludeFromCoverage
@Preview(name = "6. Full screen — single Wise account, ready to send")
@Composable
private fun FullScreen_SingleAccount_ReadyToSend_Preview() {
    BisqTheme.Preview {
        TradePaymentAccountSelectionContent(
            matchingAccounts = listOf(wiseAccount),
            selectedIndex = 0,
            accountData = wiseAccount.accountData,
            paymentMethodId = "WISE",
            paymentMethodName = "Wise",
            currency = "EUR",
            onSelect = previewOnSelect,
            onAccountDataChange = previewOnDataChange,
            onCreateAccount = previewOnCreate,
            onSendPaymentData = previewOnSend,
        )
    }
}

/**
 * Preview 7: Trade method context header variations.
 * Shows how the context row looks across different payment methods.
 */
@ExcludeFromCoverage
@Preview(name = "7. Trade method context header variations")
@Composable
private fun TradeMethodContext_Variations_Preview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            TradeMethodContextRow(
                paymentMethodId = "WISE",
                paymentMethodName = "Wise",
                currency = "EUR",
            )
            TradeMethodContextRow(
                paymentMethodId = "SEPA",
                paymentMethodName = "SEPA",
                currency = "EUR",
            )
            TradeMethodContextRow(
                paymentMethodId = "ZELLE",
                paymentMethodName = "Zelle",
                currency = "USD",
            )
            TradeMethodContextRow(
                paymentMethodId = "REVOLUT",
                paymentMethodName = "Revolut",
                currency = "GBP",
            )
            TradeMethodContextRow(
                paymentMethodId = "NATIONAL_BANK_TRANSFER",
                paymentMethodName = "National Bank Transfer",
                currency = "BRL",
            )
        }
    }
}

/**
 * Preview 8: Masking helper output verification.
 * Shows masked variants side-by-side to validate the masking logic.
 */
@ExcludeFromCoverage
@Preview(name = "8. Masking helper output")
@Composable
private fun MaskingOutputs_Preview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            BisqText.SmallMedium(text = "Masking examples")
            BisqGap.VHalf()
            listOf(
                "user@example.com" to simulatedMaskAccountData("user@example.com"),
                "seller@gmail.com" to simulatedMaskAccountData("seller@gmail.com"),
                "DE89370400440532013000" to simulatedMaskAccountData("DE89370400440532013000"),
                "+1234567890" to simulatedMaskAccountData("+1234567890"),
                "short" to simulatedMaskAccountData("short"),
            ).forEach { (original, masked) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                ) {
                    BisqText.XSmallLight(
                        text = original,
                        color = BisqTheme.colors.mid_grey20,
                        modifier = Modifier.weight(1f),
                    )
                    BisqText.XSmallRegular(
                        text = masked,
                        color = BisqTheme.colors.white,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
