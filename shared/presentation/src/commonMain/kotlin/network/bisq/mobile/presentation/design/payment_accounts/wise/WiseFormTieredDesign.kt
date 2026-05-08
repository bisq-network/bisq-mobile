/**
 * WiseFormTieredDesign.kt — Design PoC, Variant B
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * VARIANT: Tiered Selection (Popular + All)
 * ======================================================================================
 * Currencies are split into two tiers rendered inline on the form:
 *
 *   TIER 1 — "Popular" (10 currencies: USD, EUR, GBP, JPY, BRL, AUD, CAD, INR, MXN, ARS)
 *     Always visible. Each currency is a toggleable chip with a checkmark icon.
 *     Selecting "All" pre-checks every tier-1 chip.
 *
 *   TIER 2 — "More currencies" (remaining 33+)
 *     Hidden behind a disclosure row: "+ 33 more currencies". Tapping expands inline.
 *     Inside the expanded section: a BisqSearchField + scrollable checklist.
 *     Collapsing hides the list again but preserves selections.
 *
 * At the top of the currency section: a "Select all / Deselect all" toggle row.
 * A live count badge shows how many are currently selected.
 *
 * TRADEOFFS:
 *   + Popular tier is permanently visible — zero taps for the most common cases
 *   + No modal indirection for the happy path (USD, EUR, GBP)
 *   + The disclosure row handles the long tail without cluttering the form
 *   - More vertical real estate than the bottom-sheet variant when expanded
 *   - Two-tier mental model requires brief visual explanation (section labels do this)
 *
 * BEST WHEN:
 *   - The team believes most traders will use top-10 currencies only
 *   - A full-screen modal feels heavy relative to this settings task
 *   - The form can tolerate some vertical expansion (Step 2 has no other complex fields)
 *
 * ======================================================================================
 * PREVIEWS (4 states)
 * ======================================================================================
 *  1. Empty form — all currencies pre-selected, popular chips shown, "All" tier collapsed
 *  2. Partially filled — name + email entered, USD/EUR only selected in popular, rest cleared
 *  3. Validation error — Next tapped, no currencies selected, all fields invalid
 *  4. Expanded "more" section — tier-2 visible with "dol" search active
 *
 * ======================================================================================
 * I18N KEYS NEEDED (sentence-case, matching project convention)
 * ======================================================================================
 *  mobile.paymentAccounts.wise.holderName                   = "Account holder name"
 *  mobile.paymentAccounts.wise.holderName.prompt            = "Full legal name (as on Wise account)"
 *  mobile.paymentAccounts.wise.email                        = "Wise email address"
 *  mobile.paymentAccounts.wise.email.prompt                 = "e.g. satoshi@example.com"
 *  mobile.paymentAccounts.wise.currencies.header            = "Supported currencies"
 *  mobile.paymentAccounts.wise.currencies.selectedCount     = "{0} selected"
 *  mobile.paymentAccounts.wise.currencies.selectAll         = "Select all"
 *  mobile.paymentAccounts.wise.currencies.clearAll          = "Clear all"
 *  mobile.paymentAccounts.wise.currencies.popularLabel      = "Popular"
 *  mobile.paymentAccounts.wise.currencies.moreLabel         = "+ {0} more currencies"
 *  mobile.paymentAccounts.wise.currencies.allLabel          = "Show fewer"
 *  mobile.paymentAccounts.wise.currencies.error             = "Select at least one currency"
 *  mobile.paymentAccounts.wise.currencies.searchHint        = "Search currencies"
 *  mobile.paymentAccounts.wise.currencies.noResults         = "No currencies match your search"
 */

@file:Suppress("MagicNumber")

package network.bisq.mobile.presentation.design.payment_accounts.wise

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BisqSearchField
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// -------------------------------------------------------------------------------------
// Extended UiState for tiered variant (adds "more section expanded" flag)
// -------------------------------------------------------------------------------------

/**
 * Extends [WiseFormUiState] with a flag for whether the "more currencies" section
 * is expanded. We track this separately in local preview state below.
 *
 * In production the presenter owns this flag as part of its screen state.
 */
private data class WiseTieredViewState(
    val form: WiseFormUiState = WiseFormUiState(),
    val isMoreExpanded: Boolean = false,
)

// -------------------------------------------------------------------------------------
// Main form content
// -------------------------------------------------------------------------------------

/**
 * Wise account data form — Variant B: Tiered Selection.
 *
 * Renders holder name, email, then the tiered currency section inline.
 * No modal required for the popular-currencies case (USD, EUR, GBP etc.).
 *
 * @param state Form data state
 * @param isMoreExpanded Whether the "More currencies" disclosure section is open
 * @param onAction Callback for all user interactions
 * @param onExpandMore Callback to expand/collapse the tier-2 section
 */
@Composable
fun WiseForm_Tiered(
    state: WiseFormUiState,
    isMoreExpanded: Boolean,
    onAction: (WiseFormUiAction) -> Unit,
    onExpandMore: () -> Unit,
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

        // Tiered currency section
        WiseTieredCurrencySection(
            selectedCodes = state.selectedCurrencyCodes,
            currencyError = state.currencyError,
            isMoreExpanded = isMoreExpanded,
            searchQuery = state.currencySearchQuery,
            onToggle = { onAction(WiseFormUiAction.CurrencyToggled(it)) },
            onSelectAll = { onAction(WiseFormUiAction.SelectAllCurrencies) },
            onClearAll = { onAction(WiseFormUiAction.ClearAllCurrencies) },
            onExpandMore = onExpandMore,
            onSearchChange = { onAction(WiseFormUiAction.CurrencySearchChanged(it)) },
        )

        BisqGap.V1()

        BisqText.SmallLight(
            "Your account details are stored locally on your device and encrypted at rest. " +
                "They are only shared with your trading partner after you initiate a trade.",
            color = BisqTheme.colors.mid_grey30,
        )
    }
}

// -------------------------------------------------------------------------------------
// Tiered currency section
// -------------------------------------------------------------------------------------

/**
 * Inline two-tier currency selection section.
 *
 * Layout:
 *   Section header row: "Supported currencies" + count badge + Select all / Clear all
 *   Error message (if any)
 *   Divider
 *   "Popular" label + chip grid (10 currencies, always visible)
 *   Divider
 *   Disclosure row: "+ 33 more currencies" / "Show fewer" (toggleable)
 *   AnimatedVisibility: tier-2 search + checklist
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WiseTieredCurrencySection(
    selectedCodes: Set<String>,
    currencyError: String?,
    isMoreExpanded: Boolean,
    searchQuery: String,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onExpandMore: () -> Unit,
    onSearchChange: (String) -> Unit,
) {
    val popularCurrencies =
        WISE_CURRENCIES.filter { (code, _) ->
            WISE_POPULAR_CURRENCY_CODES.contains(code)
        }
    val moreCurrencies =
        WISE_CURRENCIES.filter { (code, _) ->
            !WISE_POPULAR_CURRENCY_CODES.contains(code)
        }
    val filteredMore =
        filteredWiseCurrencies(searchQuery).filter { (code, _) ->
            !WISE_POPULAR_CURRENCY_CODES.contains(code)
        }

    Column {
        // Section header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                BisqText.BaseRegular(
                    "Supported currencies",
                    color = if (currencyError != null) BisqTheme.colors.danger else BisqTheme.colors.white,
                )
                BisqText.SmallLight(
                    "${selectedCodes.size} of ${WISE_CURRENCIES.size} selected",
                    color = BisqTheme.colors.mid_grey20,
                )
            }

            // Select All / Clear All text buttons
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

        // Validation error message
        if (currencyError != null) {
            BisqGap.VHalf()
            BisqText.SmallLight(
                text = currencyError,
                color = BisqTheme.colors.danger,
            )
        }

        BisqHDivider(verticalPadding = BisqUIConstants.ScreenPadding)

        // Popular tier label
        BisqText.SmallLight(
            "Popular",
            color = BisqTheme.colors.mid_grey20,
        )

        BisqGap.VHalf()

        // Popular tier chip grid
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            popularCurrencies.forEach { (code, name) ->
                WiseCurrencyToggleChip(
                    code = code,
                    name = name,
                    isSelected = selectedCodes.contains(code),
                    onToggle = { onToggle(code) },
                )
            }
        }

        BisqHDivider(verticalPadding = BisqUIConstants.ScreenPadding)

        // Disclosure row for tier-2
        Surface(
            onClick = onExpandMore,
            color = BisqTheme.colors.transparent,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = BisqUIConstants.ScreenPaddingHalf),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val selectedInMore = selectedCodes.count { it in moreCurrencies.map { c -> c.first } }
                val disclosureLabel =
                    if (isMoreExpanded) {
                        "Show fewer currencies"
                    } else {
                        "+ ${moreCurrencies.size} more currencies"
                    }
                BisqText.BaseRegular(
                    disclosureLabel,
                    color = BisqTheme.colors.primary,
                )
                if (!isMoreExpanded && selectedInMore > 0) {
                    BisqText.SmallLight(
                        "$selectedInMore selected",
                        color = BisqTheme.colors.mid_grey20,
                    )
                }
            }
        }

        // Animated tier-2 expansion: search + checklist
        AnimatedVisibility(
            visible = isMoreExpanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column {
                BisqGap.V1()

                BisqSearchField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = "Search currencies",
                )

                BisqGap.V1()

                if (searchQuery.isNotBlank() && filteredMore.isEmpty()) {
                    BisqText.BaseLight(
                        "No currencies match your search.",
                        color = BisqTheme.colors.mid_grey20,
                    )
                } else {
                    // Non-lazy column for AnimatedVisibility compatibility.
                    // 33 items render acceptably without LazyColumn.
                    // Production: wrap in a fixed-height scrollable container
                    // (e.g., Box with verticalScroll + heightIn(max = 320.dp)).
                    val displayList = filteredMore.take(33)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter),
                    ) {
                        displayList.forEach { (code, name) ->
                            WiseCurrencyCheckRowTiered(
                                code = code,
                                name = name,
                                isSelected = selectedCodes.contains(code),
                                onToggle = { onToggle(code) },
                            )
                        }
                        // Show count for any items hidden by the take(33) preview cap
                        if (filteredMore.size > displayList.size) {
                            BisqGap.VHalf()
                            BisqText.SmallLight(
                                "+ ${filteredMore.size - displayList.size} more not shown in preview",
                                color = BisqTheme.colors.mid_grey20,
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// Currency chip (Tier 1 — popular)
// -------------------------------------------------------------------------------------

/**
 * Toggleable currency chip for the popular tier.
 *
 * Selected state: primary-green background (15% alpha) + primary text + checkmark prefix.
 * Unselected state: dark_grey40 background + mid_grey20 text.
 *
 * Uses the same visual vocabulary as the risk filter chips in CreateFiatAccountWizard
 * for pattern consistency. Minimum touch target is met by chip padding (≥ 48dp tall
 * with BisqUIConstants.ScreenPadding vertical padding on a 24sp-line text = ~52dp).
 */
@Composable
private fun WiseCurrencyToggleChip(
    code: String,
    name: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color =
            if (isSelected) {
                BisqTheme.colors.primary.copy(alpha = 0.15f)
            } else {
                BisqTheme.colors.dark_grey40
            },
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPaddingHalf,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (isSelected) {
                BisqText.SmallRegular(
                    text = "✓",
                    color = BisqTheme.colors.primary,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BisqText.SmallRegular(
                    text = code,
                    color = if (isSelected) BisqTheme.colors.primary else BisqTheme.colors.white,
                )
                BisqText.SmallLight(
                    text = name,
                    color = if (isSelected) BisqTheme.colors.primary else BisqTheme.colors.mid_grey20,
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// Currency check row (Tier 2 — "more" section)
// -------------------------------------------------------------------------------------

/**
 * Checkbox row for tier-2 currencies inside the expanded disclosure section.
 * Same layout as [WiseCurrencyCheckRow] in the bottom-sheet variant for
 * visual consistency between the two tier levels.
 */
@Composable
private fun WiseCurrencyCheckRowTiered(
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

@Composable
private fun WiseTiered_WizardShell(initialViewState: WiseTieredViewState) {
    var viewState by remember { mutableStateOf(initialViewState) }

    MultiScreenWizardScaffold(
        title = "Add Fiat Account",
        stepIndex = 2,
        stepsLength = 3,
        nextOnClick = { /* preview no-op */ },
        prevOnClick = { /* preview no-op */ },
        closeAction = true,
    ) {
        WiseForm_Tiered(
            state = viewState.form,
            isMoreExpanded = viewState.isMoreExpanded,
            onExpandMore = {
                viewState = viewState.copy(isMoreExpanded = !viewState.isMoreExpanded)
            },
            onAction = { action ->
                val updatedForm =
                    when (action) {
                        is WiseFormUiAction.HolderNameChanged ->
                            viewState.form.copy(holderName = action.value, holderNameError = null)
                        is WiseFormUiAction.EmailChanged ->
                            viewState.form.copy(email = action.value, emailError = null)
                        is WiseFormUiAction.CurrencyToggled -> {
                            val current = viewState.form.selectedCurrencyCodes
                            viewState.form.copy(
                                selectedCurrencyCodes =
                                    if (current.contains(action.code)) {
                                        current - action.code
                                    } else {
                                        current + action.code
                                    },
                                currencyError = null,
                            )
                        }
                        WiseFormUiAction.SelectAllCurrencies ->
                            viewState.form.copy(
                                selectedCurrencyCodes = WISE_CURRENCIES.map { it.first }.toSet(),
                                currencyError = null,
                            )
                        WiseFormUiAction.ClearAllCurrencies ->
                            viewState.form.copy(selectedCurrencyCodes = emptySet())
                        is WiseFormUiAction.CurrencySearchChanged ->
                            viewState.form.copy(currencySearchQuery = action.query)
                        else -> viewState.form
                    }
                viewState = viewState.copy(form = updatedForm)
            },
        )
    }
}

// -------------------------------------------------------------------------------------
// @Preview functions (4 states)
// -------------------------------------------------------------------------------------

/** State 1: Initial empty form — all currencies pre-selected, tier-2 collapsed */
@ExcludeFromCoverage
@Preview
@Composable
private fun WiseTiered_EmptyFormPreview() {
    BisqTheme.Preview {
        WiseTiered_WizardShell(
            initialViewState = WiseTieredViewState(),
        )
    }
}

/** State 2: Partially filled — name + email entered, only USD + EUR selected */
@ExcludeFromCoverage
@Preview
@Composable
private fun WiseTiered_PartiallyFilledPreview() {
    BisqTheme.Preview {
        WiseTiered_WizardShell(
            initialViewState =
                WiseTieredViewState(
                    form =
                        WiseFormUiState(
                            holderName = "Satoshi Nakamoto",
                            email = "satoshi@example.com",
                            selectedCurrencyCodes = setOf("USD", "EUR"),
                        ),
                ),
        )
    }
}

/** State 3: Validation error — Next tapped, no currencies selected, fields invalid */
@ExcludeFromCoverage
@Preview
@Composable
private fun WiseTiered_ValidationErrorPreview() {
    BisqTheme.Preview {
        WiseTiered_WizardShell(
            initialViewState =
                WiseTieredViewState(
                    form =
                        WiseFormUiState(
                            holderName = "",
                            email = "not-an-email",
                            selectedCurrencyCodes = emptySet(),
                            holderNameError = "Account holder name is required",
                            emailError = "Enter a valid email address",
                            currencyError = "Select at least one currency",
                        ),
                ),
        )
    }
}

/** State 4: "More currencies" section expanded, "dol" search active */
@ExcludeFromCoverage
@Preview
@Composable
private fun WiseTiered_ExpandedMoreSectionPreview() {
    BisqTheme.Preview {
        WiseTiered_WizardShell(
            initialViewState =
                WiseTieredViewState(
                    form =
                        WiseFormUiState(
                            holderName = "Satoshi Nakamoto",
                            email = "satoshi@example.com",
                            selectedCurrencyCodes = setOf("USD", "EUR", "GBP", "HKD", "NZD", "AUD"),
                            currencySearchQuery = "dol",
                        ),
                    isMoreExpanded = true,
                ),
        )
    }
}
