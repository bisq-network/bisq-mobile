/**
 * TradeHistoryFilterSheetDesign.kt — Design PoC
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * Bottom sheet for sort + filter of closed trades in the trade history list.
 * Opens from the sort icon in BisqSearchField's rightSuffix slot on TradeHistoryListScreen.
 *
 * ======================================================================================
 * LAYOUT STRUCTURE
 * ======================================================================================
 * The sheet is divided into two named sections separated by BisqHDivider:
 *
 *   ┌────────────────────────────────┐
 *   │       ▬  (drag handle)         │
 *   │  Sort by                       │  ← section label (BaseRegular, white)
 *   │  [Newest] [Oldest] [↑ Amount] [↓ Amount]  ← BisqSegmentButton (4 options)
 *   │  ─────────────────────────────  │  ← BisqHDivider
 *   │  Filter by outcome             │  ← section label
 *   │  [All] [Completed] [Cancelled] [Failed]  ← BisqSegmentButton (4 options)
 *   │  ─────────────────────────────  │  ← BisqHDivider
 *   │  Filter by role                │  ← section label
 *   │  [All] [Buyer] [Seller]        │  ← BisqSegmentButton (3 options)
 *   │  ─────────────────────────────
 *   │ [Apply]  [Reset]       │  ← action row
 *   └────────────────────────────────┘
 *
 * ======================================================================================
 * SORT OPTIONS (mirrors desktop BisqEasyHistoryView column sorts)
 * ======================================================================================
 * Desktop sorts by: date, quote amount, base amount, price, payment method, peer name.
 * Mobile condenses this to the 4 most useful on mobile — matching the primary card fields:
 *   1. Newest first  (default — matches desktop default date-desc sort)
 *   2. Oldest first
 *   3. Amount: high to low  (by quote/fiat amount — most useful for record-keeping)
 *   4. Amount: low to high
 *
 * Payment method sort is omitted: the filter-by-outcome is more actionable on mobile.
 * Peer name sort is omitted: search already handles peer-name lookup.
 *
 * ======================================================================================
 * FILTER BY OUTCOME
 * ======================================================================================
 * Maps the 40+ BisqEasyTradeStateDto FSM terminal states to the same 4 visual buckets
 * already used by TradeHistoryCard (SimulatedTradeOutcome):
 *   All | Completed | Cancelled / Rejected | Failed
 *
 * "Cancelled" and "Rejected" are merged in the filter because from the user's perspective
 * they carry the same meaning: the trade did not complete. The card still shows the
 * distinction via its badge. Power users who care can use search to find "Rejected".
 *
 * ======================================================================================
 * FILTER BY ROLE
 * ======================================================================================
 * Answers "show me only trades where I was a buyer" — a common use case for tax reporting
 * (capital gains on sales). Three options: All | Buyer | Seller.
 * Maker/Taker distinction is omitted from the filter — too granular for most users;
 * it is visible on each card's badge for those who care.
 *
 * ======================================================================================
 * ACTIVE FILTER INDICATOR
 * ======================================================================================
 * When any non-default filter/sort is active:
 *   - The sort icon in the search bar turns green (using icon_sort_green resource)
 *   - A slim "Active filters: Completed · Buyer" dismissible chip row appears between
 *     the search bar and the results count in TradeHistoryListScreen.
 *   - Both are simulated in the previews but implemented in the list screen (not here).
 *
 * The sheet itself shows the current selection via BisqSegmentButton's active state.
 * There is no persistent chip row inside the sheet — the sheet is stateless;
 * it opens with current values pre-selected and closes on Apply or Reset.
 *
 * ======================================================================================
 * SEGMENT BUTTON DEPENDENCY
 * ======================================================================================
 * This design uses the current BisqSegmentButton. Before implementing this screen,
 * replace BisqSegmentButton internals with the redesigned version from:
 *   presentation/design/components/SegmentButtonRedesign.kt
 *
 * The redesign uses AutoResizeText (maxLines=1) instead of Material3 SegmentedButton,
 * which fixes text wrapping for long/i18n labels. Same API signature — drop-in replacement.
 * See SegmentButtonRedesign.kt for full rationale and i18n stress-test previews.
 *
 * TODO: Replace BisqSegmentButton content with SimulatedBisqSegmentButtonV2 implementation
 *       before implementing this filter sheet.
 *
 * ======================================================================================
 * APPLY vs RESET
 * ======================================================================================
 * - "Apply" (primary button): closes sheet, emits new sort+filter values to presenter.
 * - "Reset" (grey button): resets to defaults (Newest first / All / All), then closes.
 *   Reset does NOT stay open so the user can review — it applies immediately.
 *   Reason: the defaults are well-understood (newest first = full list) and the user
 *   can always re-open the sheet. Staying open after reset adds an extra tap.
 *
 * ======================================================================================
 * PRESENTER CONTRACT (for implementation)
 * ======================================================================================
 * The presenter should expose:
 *   val sortOrder: StateFlow<TradeHistorySortOrder>
 *   val outcomeFilter: StateFlow<TradeHistoryOutcomeFilter>
 *   val roleFilter: StateFlow<TradeHistoryRoleFilter>
 *   fun applyFilterSort(sort: TradeHistorySortOrder, outcome: TradeHistoryOutcomeFilter, role: TradeHistoryRoleFilter)
 *   fun resetFilterSort()
 *   val isFilterActive: StateFlow<Boolean>   // true if any non-default is set
 *
 * The bottom sheet is shown/hidden via a local `showSheet` boolean in the list screen,
 * not via the presenter — it is a UI-only concern (same pattern as market filter sheet).
 *
 * ======================================================================================
 * I18N KEYS NEEDED
 * ======================================================================================
 * mobile.tradeHistory.filter.sortBy                 = "Sort by"
 * mobile.tradeHistory.filter.sort.newestFirst       = "Newest first"
 * mobile.tradeHistory.filter.sort.oldestFirst       = "Oldest first"
 * mobile.tradeHistory.filter.sort.amountHighLow     = "Amount: high to low"
 * mobile.tradeHistory.filter.sort.amountLowHigh     = "Amount: low to high"
 * mobile.tradeHistory.filter.filterByOutcome        = "Filter by outcome"
 * mobile.tradeHistory.filter.outcome.all            = "All"
 * mobile.tradeHistory.filter.outcome.completed      = "Completed"
 * mobile.tradeHistory.filter.outcome.cancelled      = "Cancelled"
 * mobile.tradeHistory.filter.outcome.failed         = "Failed"
 * mobile.tradeHistory.filter.filterByRole           = "Filter by role"
 * mobile.tradeHistory.filter.role.all               = "All"
 * mobile.tradeHistory.filter.role.buyer             = "Buyer"
 * mobile.tradeHistory.filter.role.seller            = "Seller"
 * mobile.tradeHistory.filter.action.apply           = "Apply"
 * mobile.tradeHistory.filter.action.reset           = "Reset"
 * mobile.tradeHistory.filter.activeLabel            = "Active filters:"
 */
package network.bisq.mobile.presentation.design.trade_history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqSegmentButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.SortIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BisqSearchField
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// -------------------------------------------------------------------------------------
// Sort / filter state enums — primitives only, no domain types
// -------------------------------------------------------------------------------------

internal enum class SimulatedTradeHistorySortOrder(
    val label: String,
) {
    NEWEST_FIRST("Newest"),
    OLDEST_FIRST("Oldest"),
    AMOUNT_HIGH_LOW("Amt ↓"),
    AMOUNT_LOW_HIGH("Amt ↑"),
}

internal enum class SimulatedTradeHistoryOutcomeFilter(
    val label: String,
) {
    ALL("All"),
    COMPLETED("Done"),
    CANCELLED("Cancel"),
    FAILED("Failed"),
}

internal enum class SimulatedTradeHistoryRoleFilter(
    val label: String,
) {
    ALL("All"),
    BUYER("Buyer"),
    SELLER("Seller"),
}

// -------------------------------------------------------------------------------------
// Default values — used for reset and "is active" detection
// -------------------------------------------------------------------------------------

internal val DEFAULT_SORT = SimulatedTradeHistorySortOrder.NEWEST_FIRST
internal val DEFAULT_OUTCOME = SimulatedTradeHistoryOutcomeFilter.ALL
internal val DEFAULT_ROLE = SimulatedTradeHistoryRoleFilter.ALL

internal fun isFilterActive(
    sort: SimulatedTradeHistorySortOrder,
    outcome: SimulatedTradeHistoryOutcomeFilter,
    role: SimulatedTradeHistoryRoleFilter,
): Boolean = sort != DEFAULT_SORT || outcome != DEFAULT_OUTCOME || role != DEFAULT_ROLE

// -------------------------------------------------------------------------------------
// Sheet composable
// -------------------------------------------------------------------------------------

/**
 * Sort and filter bottom sheet for the trade history list.
 *
 * The sheet is opened from the sort icon inside BisqSearchField's rightSuffix. It uses
 * BisqBottomSheet as the wrapper (which handles preview-mode, affected-device fallback,
 * and ModalBottomSheet for normal devices — no special handling needed here).
 *
 * All state is local to this composable. The caller receives new values only on Apply/Reset,
 * keeping the sheet interaction self-contained and preventing live list updates while the
 * user browses options.
 *
 * @param initialSort      Sort value currently applied to the list
 * @param initialOutcome   Outcome filter currently applied
 * @param initialRole      Role filter currently applied
 * @param onApply          Called with the new triple when user taps Apply
 * @param onReset          Called when user taps Reset (presenter should restore defaults)
 * @param onDismiss        Called when sheet is dismissed without applying (back / scrim tap)
 */
@Composable
internal fun TradeHistoryFilterSheet(
    initialSort: SimulatedTradeHistorySortOrder = DEFAULT_SORT,
    initialOutcome: SimulatedTradeHistoryOutcomeFilter = DEFAULT_OUTCOME,
    initialRole: SimulatedTradeHistoryRoleFilter = DEFAULT_ROLE,
    onApply: (SimulatedTradeHistorySortOrder, SimulatedTradeHistoryOutcomeFilter, SimulatedTradeHistoryRoleFilter) -> Unit = { _, _, _ -> },
    onReset: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    var sortOrder by remember { mutableStateOf(initialSort) }
    var outcomeFilter by remember { mutableStateOf(initialOutcome) }
    var roleFilter by remember { mutableStateOf(initialRole) }

    BisqBottomSheet(onDismissRequest = onDismiss) {
        TradeHistoryFilterSheetContent(
            sortOrder = sortOrder,
            outcomeFilter = outcomeFilter,
            roleFilter = roleFilter,
            onSortChange = { sortOrder = it },
            onOutcomeChange = { outcomeFilter = it },
            onRoleChange = { roleFilter = it },
            onApply = { onApply(sortOrder, outcomeFilter, roleFilter) },
            onReset = {
                sortOrder = DEFAULT_SORT
                outcomeFilter = DEFAULT_OUTCOME
                roleFilter = DEFAULT_ROLE
                onReset()
            },
        )
    }
}

// -------------------------------------------------------------------------------------
// Inner content — extracted to allow rendering in the inline preview layout
// -------------------------------------------------------------------------------------

/**
 * The visible content of the filter sheet — separated from BisqBottomSheet wrapper
 * so it can be previewed without the sheet chrome in isolation.
 */
@Composable
private fun TradeHistoryFilterSheetContent(
    sortOrder: SimulatedTradeHistorySortOrder,
    outcomeFilter: SimulatedTradeHistoryOutcomeFilter,
    roleFilter: SimulatedTradeHistoryRoleFilter,
    onSortChange: (SimulatedTradeHistorySortOrder) -> Unit,
    onOutcomeChange: (SimulatedTradeHistoryOutcomeFilter) -> Unit,
    onRoleChange: (SimulatedTradeHistoryRoleFilter) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = BisqUIConstants.ScreenPadding2X),
    ) {
        // ── Sort by ──────────────────────────────────────────────────────────────
        FilterSectionLabel("Sort by")

        BisqGap.VHalf()

        BisqSegmentButton(
            label = "",
            value = sortOrder,
            items = SimulatedTradeHistorySortOrder.entries.map { it to it.label },
            onValueChange = { pair -> onSortChange(pair.first) },
        )

        BisqHDivider(verticalPadding = BisqUIConstants.ScreenPadding)

        // ── Filter by outcome ─────────────────────────────────────────────────────
        FilterSectionLabel("Filter by outcome")

        BisqGap.VHalf()

        BisqSegmentButton(
            label = "",
            value = outcomeFilter,
            items = SimulatedTradeHistoryOutcomeFilter.entries.map { it to it.label },
            onValueChange = { pair -> onOutcomeChange(pair.first) },
        )

        BisqHDivider(verticalPadding = BisqUIConstants.ScreenPadding)

        // ── Filter by role ────────────────────────────────────────────────────────
        FilterSectionLabel("Filter by role")

        BisqGap.VHalf()

        BisqSegmentButton(
            label = "",
            value = roleFilter,
            items = SimulatedTradeHistoryRoleFilter.entries.map { it to it.label },
            onValueChange = { pair -> onRoleChange(pair.first) },
        )

        BisqHDivider(verticalPadding = BisqUIConstants.ScreenPadding)

        // ── Action row ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            BisqButton(
                text = "Reset",
                type = BisqButtonType.Grey,
                onClick = onReset,
                modifier = Modifier.weight(1f),
                fullWidth = true,
            )
            BisqButton(
                text = "Apply",
                onClick = onApply,
                modifier = Modifier.weight(1f),
                fullWidth = true,
            )
        }

        BisqGap.V1()
    }
}

// -------------------------------------------------------------------------------------
// Section label helper — small label above each filter group
// -------------------------------------------------------------------------------------

@Composable
private fun FilterSectionLabel(text: String) {
    BisqText.BaseRegular(
        text = text,
        color = BisqTheme.colors.white,
    )
}

// -------------------------------------------------------------------------------------
// Active filter summary row
// -------------------------------------------------------------------------------------

/**
 * Slim inline row shown below the search bar when any non-default filter is active.
 * Placed in TradeHistoryListScreen between the search bar and the results count header.
 *
 * Shows up to 2 active non-default dimensions; excess is hidden (unlikely in practice
 * since there are only 3 dimensions and "All" is the default for 2 of them).
 *
 * The "Clear" text button at the end calls onClearAll which resets to defaults.
 *
 * @param sort        Current sort order
 * @param outcome     Current outcome filter
 * @param role        Current role filter
 * @param onClearAll  Called when user taps "Clear" — presenter should reset to defaults
 */
@Composable
internal fun TradeHistoryActiveFilterBar(
    sort: SimulatedTradeHistorySortOrder,
    outcome: SimulatedTradeHistoryOutcomeFilter,
    role: SimulatedTradeHistoryRoleFilter,
    onClearAll: () -> Unit,
) {
    if (!isFilterActive(sort, outcome, role)) return

    val summaryParts =
        buildList {
            if (sort != DEFAULT_SORT) add(sort.label)
            if (outcome != DEFAULT_OUTCOME) add(outcome.label)
            if (role != DEFAULT_ROLE) add(role.label)
        }
    val summaryText = summaryParts.joinToString(" · ")

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPaddingQuarter,
                ),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BisqText.SmallLightGrey(
            text = summaryText,
            modifier = Modifier.weight(1f),
        )
        BisqButton(
            text = "Clear",
            type = BisqButtonType.Underline,
            onClick = onClearAll,
        )
    }
}

// -------------------------------------------------------------------------------------
// Previews
// -------------------------------------------------------------------------------------

/**
 * Preview: default state — all values at defaults (Newest first / All / All).
 * Represents the sheet as it appears when opened for the first time.
 */
@ExcludeFromCoverage
@Preview(showBackground = true)
@Composable
private fun FilterSheet_DefaultState_Preview() {
    BisqTheme.Preview {
        TradeHistoryFilterSheetContent(
            sortOrder = DEFAULT_SORT,
            outcomeFilter = DEFAULT_OUTCOME,
            roleFilter = DEFAULT_ROLE,
            onSortChange = {},
            onOutcomeChange = {},
            onRoleChange = {},
            onApply = {},
            onReset = {},
        )
    }
}

/**
 * Preview: active filters — Oldest first, Completed outcome, Seller role.
 * Represents a user who is reviewing their sell history oldest-first (tax use case).
 */
@ExcludeFromCoverage
@Preview(showBackground = true)
@Composable
private fun FilterSheet_ActiveFilters_Preview() {
    BisqTheme.Preview {
        TradeHistoryFilterSheetContent(
            sortOrder = SimulatedTradeHistorySortOrder.OLDEST_FIRST,
            outcomeFilter = SimulatedTradeHistoryOutcomeFilter.COMPLETED,
            roleFilter = SimulatedTradeHistoryRoleFilter.SELLER,
            onSortChange = {},
            onOutcomeChange = {},
            onRoleChange = {},
            onApply = {},
            onReset = {},
        )
    }
}

/**
 * Preview: amount sort, failed outcome — investigating protocol failures by size.
 */
@ExcludeFromCoverage
@Preview(showBackground = true)
@Composable
private fun FilterSheet_AmountSort_FailedOutcome_Preview() {
    BisqTheme.Preview {
        TradeHistoryFilterSheetContent(
            sortOrder = SimulatedTradeHistorySortOrder.AMOUNT_HIGH_LOW,
            outcomeFilter = SimulatedTradeHistoryOutcomeFilter.FAILED,
            roleFilter = SimulatedTradeHistoryRoleFilter.ALL,
            onSortChange = {},
            onOutcomeChange = {},
            onRoleChange = {},
            onApply = {},
            onReset = {},
        )
    }
}

/**
 * Preview: interactive — sheet state updates in Android Studio preview interaction mode.
 */
@ExcludeFromCoverage
@Preview(showBackground = true)
@Composable
private fun FilterSheet_Interactive_Preview() {
    BisqTheme.Preview {
        var sortOrder by remember { mutableStateOf(DEFAULT_SORT) }
        var outcomeFilter by remember { mutableStateOf(DEFAULT_OUTCOME) }
        var roleFilter by remember { mutableStateOf(DEFAULT_ROLE) }

        TradeHistoryFilterSheetContent(
            sortOrder = sortOrder,
            outcomeFilter = outcomeFilter,
            roleFilter = roleFilter,
            onSortChange = { sortOrder = it },
            onOutcomeChange = { outcomeFilter = it },
            onRoleChange = { roleFilter = it },
            onApply = {},
            onReset = {
                sortOrder = DEFAULT_SORT
                outcomeFilter = DEFAULT_OUTCOME
                roleFilter = DEFAULT_ROLE
            },
        )
    }
}

/**
 * Preview: active filter bar — shows the slim chip row below the search bar
 * when non-default filters are active.
 */
@ExcludeFromCoverage
@Preview(showBackground = true)
@Composable
private fun ActiveFilterBar_Preview() {
    BisqTheme.Preview {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                    .background(Color.Transparent),
        ) {
            TradeHistoryActiveFilterBar(
                sort = SimulatedTradeHistorySortOrder.OLDEST_FIRST,
                outcome = SimulatedTradeHistoryOutcomeFilter.COMPLETED,
                role = SimulatedTradeHistoryRoleFilter.SELLER,
                onClearAll = {},
            )
        }
    }
}

/**
 * Preview: full list screen context — shows how the search bar + active filter bar +
 * list count header look together with the filter bar visible.
 * Simulates the layout of TradeHistoryListScreen with an active filter applied.
 */
@ExcludeFromCoverage
@Preview(showBackground = true, heightDp = 600)
@Composable
private fun ListScreen_WithActiveFilterBar_Preview() {
    BisqTheme.Preview {
        var showSheet by remember { mutableStateOf(false) }
        var sortOrder by remember { mutableStateOf(SimulatedTradeHistorySortOrder.OLDEST_FIRST) }
        var outcomeFilter by remember { mutableStateOf(SimulatedTradeHistoryOutcomeFilter.COMPLETED) }
        var roleFilter by remember { mutableStateOf(SimulatedTradeHistoryRoleFilter.ALL) }
        var searchQuery by remember { mutableStateOf("") }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor),
        ) {
            // Search bar row (mirrors TradeHistoryListScreen)
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(BisqTheme.colors.backgroundColor)
                        .padding(
                            horizontal = BisqUIConstants.ScreenPadding,
                            vertical = BisqUIConstants.ScreenPaddingHalf,
                        ),
            ) {
                BisqSearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search by peer or market",
                    modifier = Modifier.fillMaxWidth(),
                    rightSuffix = {
                        BisqButton(
                            iconOnly = {
                                SortIcon()
                            },
                            onClick = { showSheet = true },
                            type = BisqButtonType.Clear,
                        )
                    },
                )
            }

            // Active filter bar — shown when any non-default filter is applied
            if (isFilterActive(sortOrder, outcomeFilter, roleFilter)) {
                TradeHistoryActiveFilterBar(
                    sort = sortOrder,
                    outcome = outcomeFilter,
                    role = roleFilter,
                    onClearAll = {
                        sortOrder = DEFAULT_SORT
                        outcomeFilter = DEFAULT_OUTCOME
                        roleFilter = DEFAULT_ROLE
                    },
                )
            }

            // Results count label (scrolls with list in production)
            BisqText.SmallLightGrey(
                text = "2 of 5 trades",
                modifier =
                    Modifier.padding(
                        start = BisqUIConstants.ScreenPadding,
                        bottom = BisqUIConstants.ScreenPaddingQuarter,
                    ),
            )

            BisqGap.V1()
            BisqText.SmallLightGrey(
                text = "(trade cards would render here)",
                modifier = Modifier.padding(horizontal = BisqUIConstants.ScreenPadding),
            )

            // Sheet — rendered inline in preview via InlinePreviewBottomSheet
            if (showSheet) {
                TradeHistoryFilterSheet(
                    initialSort = sortOrder,
                    initialOutcome = outcomeFilter,
                    initialRole = roleFilter,
                    onApply = { s, o, r ->
                        sortOrder = s
                        outcomeFilter = o
                        roleFilter = r
                        showSheet = false
                    },
                    onReset = {
                        sortOrder = DEFAULT_SORT
                        outcomeFilter = DEFAULT_OUTCOME
                        roleFilter = DEFAULT_ROLE
                        showSheet = false
                    },
                    onDismiss = { showSheet = false },
                )
            }
        }
    }
}
