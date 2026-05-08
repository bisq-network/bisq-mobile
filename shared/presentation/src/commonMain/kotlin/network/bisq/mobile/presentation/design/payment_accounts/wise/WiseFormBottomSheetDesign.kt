/**
 * WiseFormBottomSheetDesign.kt — Design PoC, Variant A
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * VARIANT: Bottom Sheet Picker
 * ======================================================================================
 * The main form is compact: holder name, email, and a single tappable summary row
 * showing "X of Y currencies selected". Tapping the row opens a full-height bottom
 * sheet with a search field, "Select All / Clear All" action row, and a scrollable
 * checklist of all 43+ Wise currencies.
 *
 * TRADEOFFS:
 *   + Search + checklist is the strongest pattern for "find a specific currency" (O(log n))
 *   + Select All / Clear All satisfies both power cases in two taps
 *   + Main form stays vertically compact — no scrolling needed for name + email
 *   - Modal indirection: users must open/close a sheet to change currencies
 *   - The sheet content is a flat list — no grouping cue for popular vs exotic
 *
 * BEST WHEN:
 *   - The implementer wants a proven, simple pattern
 *   - Most users are expected to use the default (all) or clear to a few specific ones
 *   - Screen real estate on Step 2 is already crowded (e.g., future fields added)
 *
 * ======================================================================================
 * PREVIEWS (4 states)
 * ======================================================================================
 *  1. Empty form — initial state, all currencies pre-selected, picker closed
 *  2. Partially filled — holder name entered, email empty, 3 currencies deselected
 *  3. Validation error — Next tapped, holder name missing, email invalid, 0 currencies
 *  4. Picker open — bottom sheet visible, partially filled, search with "dol" filter
 *
 * ======================================================================================
 * I18N KEYS NEEDED (sentence-case, matching project convention)
 * ======================================================================================
 *  mobile.paymentAccounts.wise.holderName            = "Account holder name"
 *  mobile.paymentAccounts.wise.holderName.prompt     = "Full legal name (as on Wise account)"
 *  mobile.paymentAccounts.wise.email                 = "Wise email address"
 *  mobile.paymentAccounts.wise.email.prompt          = "e.g. satoshi@example.com"
 *  mobile.paymentAccounts.wise.currencies.summary    = "{0} of {1} currencies selected"
 *  mobile.paymentAccounts.wise.currencies.allSelected= "All {0} currencies selected"
 *  mobile.paymentAccounts.wise.currencies.editLabel  = "Tap to change"
 *  mobile.paymentAccounts.wise.currencies.error      = "Select at least one currency"
 *  mobile.paymentAccounts.wise.picker.title          = "Supported currencies"
 *  mobile.paymentAccounts.wise.picker.selectAll      = "Select all"
 *  mobile.paymentAccounts.wise.picker.clearAll       = "Clear all"
 *  mobile.paymentAccounts.wise.picker.searchHint     = "Search currencies"
 *  mobile.paymentAccounts.wise.picker.noResults      = "No currencies match your search"
 */

@file:Suppress("MagicNumber")

package network.bisq.mobile.presentation.design.payment_accounts.wise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqCheckbox
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BisqSearchField
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// -------------------------------------------------------------------------------------
// Main form content
// -------------------------------------------------------------------------------------

/**
 * Wise account data form — Variant A: Bottom Sheet Picker.
 *
 * Renders holder name + email text fields, then a tappable currency summary row.
 * The currency picker is kept out of the main scroll so Step 2 stays compact.
 *
 * @param state Current form state
 * @param onAction Callback for all user interactions
 */
@Composable
fun WiseForm_BottomSheet(
    state: WiseFormUiState,
    onAction: (WiseFormUiAction) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        BisqText.H3Light("Account details")
        BisqText.SmallLight(
            "Enter the details for your Wise account. " +
                "These will be shared with your trading partner when you confirm a trade.",
            color = BisqTheme.colors.mid_grey20,
        )

        BisqGap.VHalf()

        // Holder name field
        BisqTextFieldV0(
            modifier = Modifier.fillMaxWidth(),
            value = state.holderName,
            onValueChange = { onAction(WiseFormUiAction.HolderNameChanged(it)) },
            label = "Account holder name",
            placeholder = "Full legal name (as on Wise account)",
            isError = state.holderNameError != null,
            bottomMessage = state.holderNameError,
            singleLine = true,
        )

        // Email field
        BisqTextFieldV0(
            modifier = Modifier.fillMaxWidth(),
            value = state.email,
            onValueChange = { onAction(WiseFormUiAction.EmailChanged(it)) },
            label = "Wise email address",
            placeholder = "e.g. satoshi@example.com",
            isError = state.emailError != null,
            bottomMessage = state.emailError,
            singleLine = true,
            keyboardOptions =
                androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                ),
        )

        BisqGap.VHalf()

        // Currency summary row — tappable, opens the bottom sheet picker
        WiseCurrencySummaryRow(
            selectedCount = state.selectedCurrencyCodes.size,
            totalCount = WISE_CURRENCIES.size,
            hasError = state.currencyError != null,
            onTap = { onAction(WiseFormUiAction.OpenCurrencyPicker) },
        )

        // Error message below summary row
        if (state.currencyError != null) {
            BisqText.SmallLight(
                text = state.currencyError,
                color = BisqTheme.colors.danger,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        BisqGap.V1()

        BisqText.SmallLight(
            "Your account details are stored locally on your device and encrypted at rest. " +
                "They are only shared with your trading partner after you initiate a trade.",
            color = BisqTheme.colors.mid_grey30,
        )
    }

    // Currency picker bottom sheet — rendered outside the Column so it overlays correctly
    if (state.isCurrencyPickerOpen) {
        WiseCurrencyPickerBottomSheet(
            selectedCodes = state.selectedCurrencyCodes,
            searchQuery = state.currencySearchQuery,
            onSearchChange = { onAction(WiseFormUiAction.CurrencySearchChanged(it)) },
            onToggle = { onAction(WiseFormUiAction.CurrencyToggled(it)) },
            onSelectAll = { onAction(WiseFormUiAction.SelectAllCurrencies) },
            onClearAll = { onAction(WiseFormUiAction.ClearAllCurrencies) },
            onDismiss = { onAction(WiseFormUiAction.CloseCurrencyPicker) },
        )
    }
}

// -------------------------------------------------------------------------------------
// Currency summary row
// -------------------------------------------------------------------------------------

/**
 * Compact tappable row showing the current currency selection count.
 *
 * Visual states:
 *   - All selected: "All 43 currencies selected" + grey "Tap to change" link
 *   - Partial: "12 of 43 currencies selected" + grey "Tap to change" link
 *   - Error: danger-colored border + error indicator text
 *
 * Uses dark_grey50 surface with a green left accent bar when valid, danger-red when invalid.
 * This treatment mirrors how BisqTextFieldV0 signals error state via its bottom indicator.
 */
@Composable
private fun WiseCurrencySummaryRow(
    selectedCount: Int,
    totalCount: Int,
    hasError: Boolean,
    onTap: () -> Unit,
) {
    val borderColor = if (hasError) BisqTheme.colors.danger else BisqTheme.colors.mid_grey20

    Surface(
        onClick = onTap,
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = BisqTheme.colors.dark_grey50,
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
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                BisqText.SmallLight(
                    text = "Supported currencies",
                    color = if (hasError) BisqTheme.colors.danger else BisqTheme.colors.white,
                )
                BisqGap.VHalf()
                val summaryText =
                    if (selectedCount == totalCount) {
                        "All $totalCount currencies selected"
                    } else {
                        "$selectedCount of $totalCount currencies selected"
                    }
                BisqText.BaseRegular(
                    text = summaryText,
                    color = if (hasError) BisqTheme.colors.danger else BisqTheme.colors.white,
                )
            }

            // Edit affordance — right-aligned
            BisqText.SmallLight(
                text = "Tap to change",
                color = borderColor,
            )
        }
    }
}

// -------------------------------------------------------------------------------------
// Currency picker bottom sheet
// -------------------------------------------------------------------------------------

/**
 * Full-height bottom sheet for selecting Wise currencies.
 *
 * Layout (top to bottom inside the sheet):
 *   Drag handle (from BisqBottomSheet)
 *   "Supported currencies" title
 *   "Select all" | "Clear all" action row
 *   BisqSearchField
 *   Scrollable checklist via LazyColumn
 *
 * The checklist shows display name (primary) + code (secondary grey).
 * The search filters by both code and display name for multilingual users.
 *
 * Select All / Clear All are text-buttons using BisqText primary color for prominence.
 * They reset the full set without closing the sheet so the user can then fine-tune.
 */
@Composable
private fun WiseCurrencyPickerBottomSheet(
    selectedCodes: Set<String>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val filtered = filteredWiseCurrencies(searchQuery)

    BisqBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = BisqUIConstants.ScreenPadding),
        ) {
            BisqText.H5Regular("Supported currencies")
            BisqGap.VHalf()

            // Select All / Clear All shortcut row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BisqText.SmallLight(
                    "${selectedCodes.size} of ${WISE_CURRENCIES.size} selected",
                    color = BisqTheme.colors.mid_grey20,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)) {
                    Surface(
                        onClick = onSelectAll,
                        color = BisqTheme.colors.transparent,
                    ) {
                        BisqText.SmallRegular(
                            "Select all",
                            color = BisqTheme.colors.primary,
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                    Surface(
                        onClick = onClearAll,
                        color = BisqTheme.colors.transparent,
                    ) {
                        BisqText.SmallRegular(
                            "Clear all",
                            color = BisqTheme.colors.mid_grey20,
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                }
            }

            BisqGap.V1()

            // Search field
            BisqSearchField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = "Search currencies",
            )

            BisqGap.V1()

            // Scrollable checklist
            if (filtered.isEmpty()) {
                BisqText.BaseLight(
                    "No currencies match your search.",
                    color = BisqTheme.colors.mid_grey20,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter),
                ) {
                    items(filtered) { (code, name) ->
                        WiseCurrencyCheckRow(
                            code = code,
                            name = name,
                            isSelected = selectedCodes.contains(code),
                            onToggle = { onToggle(code) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single currency row in the picker checklist.
 *
 * Left: BisqCheckbox (48dp touch target via Checkbox padding)
 * Center: display name (primary) with currency code below in mid_grey20
 *
 * The entire row is tappable, not just the checkbox nib, for comfortable thumb use.
 */
@Composable
private fun WiseCurrencyCheckRow(
    code: String,
    name: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = BisqUIConstants.ScreenPaddingQuarter),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        BisqCheckbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
        )
        Column(modifier = Modifier.weight(1f)) {
            BisqText.BaseRegular(name)
            BisqText.SmallLight(code, color = BisqTheme.colors.mid_grey20)
        }
    }
}

// -------------------------------------------------------------------------------------
// Wizard shell composables (for @Preview)
// -------------------------------------------------------------------------------------

/** Wizard step 2 with Wise bottom-sheet form. Manages local state for previews. */
@Composable
private fun WiseBottomSheet_WizardShell(initialState: WiseFormUiState) {
    var state by remember { mutableStateOf(initialState) }

    MultiScreenWizardScaffold(
        title = "Add Fiat Account",
        stepIndex = 2,
        stepsLength = 3,
        nextOnClick = { /* preview no-op */ },
        prevOnClick = { /* preview no-op */ },
        closeAction = true,
    ) {
        WiseForm_BottomSheet(
            state = state,
            onAction = { action ->
                state =
                    when (action) {
                        is WiseFormUiAction.HolderNameChanged ->
                            state.copy(holderName = action.value, holderNameError = null)
                        is WiseFormUiAction.EmailChanged ->
                            state.copy(email = action.value, emailError = null)
                        is WiseFormUiAction.CurrencyToggled ->
                            state.copy(
                                selectedCurrencyCodes =
                                    if (state.selectedCurrencyCodes.contains(action.code)) {
                                        state.selectedCurrencyCodes - action.code
                                    } else {
                                        state.selectedCurrencyCodes + action.code
                                    },
                                currencyError = null,
                            )
                        WiseFormUiAction.SelectAllCurrencies ->
                            state.copy(
                                selectedCurrencyCodes = WISE_CURRENCIES.map { it.first }.toSet(),
                                currencyError = null,
                            )
                        WiseFormUiAction.ClearAllCurrencies ->
                            state.copy(selectedCurrencyCodes = emptySet())
                        WiseFormUiAction.OpenCurrencyPicker ->
                            state.copy(isCurrencyPickerOpen = true)
                        WiseFormUiAction.CloseCurrencyPicker ->
                            state.copy(isCurrencyPickerOpen = false, currencySearchQuery = "")
                        is WiseFormUiAction.CurrencySearchChanged ->
                            state.copy(currencySearchQuery = action.query)
                        WiseFormUiAction.NextTapped -> state
                    }
            },
        )
    }
}

/** Picker-open shell — shows the bottom sheet inline for previews. */
@Composable
private fun WiseBottomSheet_PickerOpen_Shell(initialState: WiseFormUiState) {
    var state by remember { mutableStateOf(initialState) }

    // In preview mode BisqBottomSheet renders inline, so we show both form and sheet
    Column {
        BisqHDivider()
        BisqText.SmallLight(
            "[ Main form above / Sheet below — inline preview only ]",
            color = BisqTheme.colors.mid_grey20,
            modifier = Modifier.padding(BisqUIConstants.ScreenPaddingHalf),
        )
        BisqHDivider()

        WiseCurrencyPickerBottomSheet(
            selectedCodes = state.selectedCurrencyCodes,
            searchQuery = state.currencySearchQuery,
            onSearchChange = { state = state.copy(currencySearchQuery = it) },
            onToggle = { code ->
                state =
                    state.copy(
                        selectedCurrencyCodes =
                            if (state.selectedCurrencyCodes.contains(code)) {
                                state.selectedCurrencyCodes - code
                            } else {
                                state.selectedCurrencyCodes + code
                            },
                    )
            },
            onSelectAll = {
                state =
                    state.copy(
                        selectedCurrencyCodes = WISE_CURRENCIES.map { it.first }.toSet(),
                    )
            },
            onClearAll = { state = state.copy(selectedCurrencyCodes = emptySet()) },
            onDismiss = { state = state.copy(isCurrencyPickerOpen = false) },
        )
    }
}

// -------------------------------------------------------------------------------------
// @Preview functions (4 states)
// -------------------------------------------------------------------------------------

/** State 1: Initial empty form — all currencies pre-selected, no errors */
@ExcludeFromCoverage
@Preview
@Composable
private fun WiseBottomSheet_EmptyFormPreview() {
    BisqTheme.Preview {
        WiseBottomSheet_WizardShell(
            initialState = WiseFormUiState(),
        )
    }
}

/** State 2: Partially filled — name entered, email blank, a few currencies deselected */
@ExcludeFromCoverage
@Preview
@Composable
private fun WiseBottomSheet_PartiallyFilledPreview() {
    BisqTheme.Preview {
        WiseBottomSheet_WizardShell(
            initialState =
                WiseFormUiState(
                    holderName = "Satoshi Nakamoto",
                    email = "",
                    selectedCurrencyCodes = WISE_CURRENCIES.map { it.first }.toSet() - setOf("ARS", "BDT", "BWP"),
                ),
        )
    }
}

/** State 3: Validation error — Next tapped, all three fields invalid */
@ExcludeFromCoverage
@Preview
@Composable
private fun WiseBottomSheet_ValidationErrorPreview() {
    BisqTheme.Preview {
        WiseBottomSheet_WizardShell(
            initialState =
                WiseFormUiState(
                    holderName = "",
                    email = "not-an-email",
                    selectedCurrencyCodes = emptySet(),
                    holderNameError = "Account holder name is required",
                    emailError = "Enter a valid email address",
                    currencyError = "Select at least one currency",
                ),
        )
    }
}

/** State 4: Currency picker open — bottom sheet with "dol" search filter active */
@ExcludeFromCoverage
@Preview
@Composable
private fun WiseBottomSheet_PickerOpenPreview() {
    BisqTheme.Preview {
        WiseBottomSheet_PickerOpen_Shell(
            initialState =
                WiseFormUiState(
                    holderName = "Satoshi Nakamoto",
                    email = "satoshi@example.com",
                    selectedCurrencyCodes = WISE_CURRENCIES.map { it.first }.toSet() - setOf("ARS", "BDT"),
                    isCurrencyPickerOpen = true,
                    currencySearchQuery = "dol",
                ),
        )
    }
}
