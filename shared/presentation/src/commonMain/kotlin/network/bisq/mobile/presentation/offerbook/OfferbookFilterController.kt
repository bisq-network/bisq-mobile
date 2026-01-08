package network.bisq.mobile.presentation.offerbook

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ExpandAllIcon
import network.bisq.mobile.presentation.common.ui.components.molecules.PaymentMethodIcon
import network.bisq.mobile.presentation.common.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BisqSearchField
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.min

/** UI model for a toggleable method icon (payment or settlement). */
data class MethodIconState(
    val id: String,
    val label: String,
    val iconPath: String,
    val selected: Boolean,
)

/** Aggregate UI state for the controller. */
data class OfferbookFilterUiState(
    val payment: List<MethodIconState>,
    val settlement: List<MethodIconState>,
    val onlyMyOffers: Boolean,
    val hasActiveFilters: Boolean,
)

@Composable
fun OfferbookFilterController(
    state: OfferbookFilterUiState,
    onTogglePayment: (id: String) -> Unit,
    onToggleSettlement: (id: String) -> Unit,
    onOnlyMyOffersChange: (Boolean) -> Unit,
    onClearAll: () -> Unit,
    onSetPaymentSelection: (Set<String>) -> Unit,
    onSetSettlementSelection: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = false,
    isExpanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
) {
    // Controlled/uncontrolled expansion state
    var internalExpanded by remember { mutableStateOf(initialExpanded) }
    val expanded = isExpanded ?: internalExpanded

    // Collapsed trigger bar
    val headerBg = if (state.hasActiveFilters) BisqTheme.colors.primary2 else BisqTheme.colors.secondary

    Column(modifier = modifier) {
        Surface(
            color = headerBg,
            shape = RoundedCornerShape(12.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {
                        onExpandedChange?.invoke(true) ?: run { internalExpanded = true }
                    }.semantics { contentDescription = "offerbook_filters_header" },
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = BisqUIConstants.ScreenPadding, vertical = BisqUIConstants.ScreenPaddingHalf),
            ) {
                // Dynamic collapsed header: icons hug the ↔ centers by default, reserve space for chevron
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(end = 32.dp),
                ) {
                    CollapsedHeaderBar(
                        payment = state.payment,
                        settlement = state.settlement,
                    )
                }
                ExpandAllIcon(
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .size(20.dp),
                )
            }
        }

        // Bottom sheet content
        if (expanded) {
            BisqBottomSheet(onDismissRequest = {
                onExpandedChange?.invoke(false) ?: run { internalExpanded = false }
            }) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(all = BisqUIConstants.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BisqText.H4Light(
                            text = "mobile.offerbook.filters.title".i18n(),
                            modifier = Modifier.weight(1f),
                        )
                        val clearEnabled = state.hasActiveFilters
                        val clearAlpha = if (clearEnabled) 1f else 0.4f
                        BisqText.BaseRegular(
                            text = "bisqEasy.offerbook.offerList.table.filters.paymentMethods.clearFilters".i18n(),
                            underline = true,
                            modifier =
                                Modifier
                                    .padding(start = 12.dp)
                                    .alpha(clearAlpha)
                                    .clickable(enabled = clearEnabled) { onClearAll() },
                        )
                    }
                    var search by rememberSaveable { mutableStateOf("") }
                    BisqSearchField(
                        value = search,
                        onValueChange = { text, _ -> search = text },
                        placeholder = "action.search".i18n(),
                    )

                    val filteredPayment =
                        if (search.isBlank()) {
                            state.payment
                        } else {
                            state.payment.filter {
                                it.label.contains(search, ignoreCase = true) || it.id.contains(search, ignoreCase = true)
                            }
                        }
                    val filteredSettlement =
                        if (search.isBlank()) {
                            state.settlement
                        } else {
                            state.settlement.filter {
                                it.label.contains(search, ignoreCase = true) || it.id.contains(search, ignoreCase = true)
                            }
                        }

                    val selectedPaymentIds =
                        state.payment
                            .filter { it.selected }
                            .map { it.id }
                            .toSet()
                    val visiblePaymentIds = filteredPayment.map { it.id }.toSet()
                    val canSelectAllPayment = visiblePaymentIds.any { it !in selectedPaymentIds }
                    val canSelectNonePayment = visiblePaymentIds.any { it in selectedPaymentIds }

                    val selectedSettlementIds =
                        state.settlement
                            .filter { it.selected }
                            .map { it.id }
                            .toSet()
                    val visibleSettlementIds = filteredSettlement.map { it.id }.toSet()
                    val canSelectAllSettlement = visibleSettlementIds.any { it !in selectedSettlementIds }
                    val canSelectNoneSettlement = visibleSettlementIds.any { it in selectedSettlementIds }

                    // Payments
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        BisqText.BaseLight("bisqEasy.offerbook.offerList.table.columns.paymentMethod".i18n())
                        Spacer(Modifier.weight(1f))
                        val alphaAllP = if (canSelectAllPayment) 1f else 0.4f
                        BisqText.BaseRegular(
                            text = "mobile.offerbook.filters.selectAll".i18n(),
                            underline = true,
                            modifier =
                                Modifier
                                    .padding(start = 12.dp)
                                    .alpha(alphaAllP)
                                    .clickable(enabled = canSelectAllPayment) {
                                        val newSet = selectedPaymentIds + visiblePaymentIds
                                        onSetPaymentSelection(newSet)
                                    },
                        )
                        Spacer(Modifier.width(12.dp))
                        val alphaNoneP = if (canSelectNonePayment) 1f else 0.4f
                        BisqText.BaseRegular(
                            text = "mobile.offerbook.filters.selectNone".i18n(),
                            underline = true,
                            modifier =
                                Modifier
                                    .alpha(alphaNoneP)
                                    .clickable(enabled = canSelectNonePayment) {
                                        val newSet = selectedPaymentIds - visiblePaymentIds
                                        onSetPaymentSelection(newSet)
                                    },
                        )
                    }
                    FilterIconsRow(items = filteredPayment, onToggle = onTogglePayment, isPaymentRow = true)

                    // Settlement
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        BisqText.BaseLight("bisqEasy.offerbook.offerList.table.columns.settlementMethod".i18n())
                        Spacer(Modifier.weight(1f))
                        val alphaAllS = if (canSelectAllSettlement) 1f else 0.4f
                        BisqText.BaseRegular(
                            text = "mobile.offerbook.filters.selectAll".i18n(),
                            underline = true,
                            modifier =
                                Modifier
                                    .padding(start = 12.dp)
                                    .alpha(alphaAllS)
                                    .clickable(enabled = canSelectAllSettlement) {
                                        val newSet = selectedSettlementIds + visibleSettlementIds
                                        onSetSettlementSelection(newSet)
                                    },
                        )
                        Spacer(Modifier.width(12.dp))
                        val alphaNoneS = if (canSelectNoneSettlement) 1f else 0.4f
                        BisqText.BaseRegular(
                            text = "mobile.offerbook.filters.selectNone".i18n(),
                            underline = true,
                            modifier =
                                Modifier
                                    .alpha(alphaNoneS)
                                    .clickable(enabled = canSelectNoneSettlement) {
                                        val newSet = selectedSettlementIds - visibleSettlementIds
                                        onSetSettlementSelection(newSet)
                                    },
                        )
                    }
                    FilterIconsRow(items = filteredSettlement, onToggle = onToggleSettlement, isPaymentRow = false)

                    // Only my offers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BisqText.BaseLight(
                            "mobile.offerbook.filters.onlyMyOffers".i18n(),
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .clickable(
                                        enabled = true,
                                        onClick = { onOnlyMyOffersChange(!state.onlyMyOffers) },
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ),
                        )
                        Checkbox(
                            checked = state.onlyMyOffers,
                            onCheckedChange = onOnlyMyOffersChange,
                            colors =
                                CheckboxColors(
                                    uncheckedBoxColor = BisqTheme.colors.secondary,
                                    uncheckedBorderColor = BisqTheme.colors.mid_grey20,
                                    uncheckedCheckmarkColor = BisqTheme.colors.secondary,
                                    checkedBoxColor = BisqTheme.colors.secondary,
                                    checkedBorderColor = BisqTheme.colors.primaryDim,
                                    checkedCheckmarkColor = BisqTheme.colors.primary,
                                    disabledBorderColor = BisqTheme.colors.backgroundColor,
                                    disabledUncheckedBorderColor = BisqTheme.colors.backgroundColor,
                                    disabledIndeterminateBorderColor = BisqTheme.colors.backgroundColor,
                                    disabledCheckedBoxColor = BisqTheme.colors.secondary,
                                    disabledUncheckedBoxColor = BisqTheme.colors.secondary,
                                    disabledIndeterminateBoxColor = BisqTheme.colors.secondary,
                                ),
                        )
                    }

                    Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding))
                }
            }
        }
    }
}

@Composable
private fun FilterIconsRow(
    items: List<MethodIconState>,
    onToggle: (id: String) -> Unit,
    compact: Boolean = false,
    isPaymentRow: Boolean = false,
) {
    // Keep row height stable; chips are taller than bare icons
    val rowHeight = if (compact) 18.dp else 32.dp
    val rowModifier = if (compact) Modifier.height(rowHeight) else Modifier.fillMaxWidth().height(rowHeight)

    // In compact mode, mirror the collapsed header semantics and only show selected methods.
    val visibleItems = if (compact) items.filter { it.selected } else items

    Box(modifier = rowModifier) {
        if (visibleItems.isEmpty()) {
            // Stable empty state: no layout jump, subtle hint
            BisqText.BaseLight(
                text =
                    if (isPaymentRow) {
                        "mobile.offerbook.filters.noPaymentMatches".i18n()
                    } else {
                        "mobile.offerbook.filters.noSettlementMatches".i18n()
                    },
                color = BisqTheme.colors.mid_grey20,
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .semantics {
                            contentDescription = if (isPaymentRow) "no_payment_matches" else "no_settlement_matches"
                        },
            )
        } else {
            val listState = rememberLazyListState()
            val canScrollBack by remember(listState) {
                derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
            }
            val canScrollForward by remember(listState) {
                derivedStateOf {
                    val info = listState.layoutInfo
                    val last = info.visibleItemsInfo.lastOrNull()
                    last != null && (last.index < info.totalItemsCount - 1 || (last.offset + last.size) > info.viewportEndOffset)
                }
            }

            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(rowHeight),
            ) {
                items(visibleItems, key = { it.id }) { item ->
                    if (compact) {
                        // Collapsed header style: keep icon-only
                        FilterIcon(item = item, size = 18.dp, compact = true, onToggle = {}, isPayment = isPaymentRow)
                    } else {
                        MethodChip(item = item, isPaymentRow = isPaymentRow, onToggle = onToggle, height = rowHeight)
                    }
                }
            }

            // Subtle edge fades to indicate horizontal scrollability (only when not empty)
            val fadeWidth = 12.dp
            if (canScrollBack) {
                Box(
                    modifier =
                        Modifier
                            .height(rowHeight)
                            .width(fadeWidth)
                            .align(Alignment.CenterStart)
                            .background(
                                brush =
                                    Brush.horizontalGradient(
                                        colors = listOf(BisqTheme.colors.dark_grey50, Color.Transparent),
                                    ),
                            ),
                )
            }
            if (canScrollForward) {
                Box(
                    modifier =
                        Modifier
                            .height(rowHeight)
                            .width(fadeWidth)
                            .align(Alignment.CenterEnd)
                            .background(
                                brush =
                                    Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, BisqTheme.colors.dark_grey50),
                                    ),
                            ),
                )
            }
        }
    }
}

@Composable
private fun CollapsedHeaderBar(
    payment: List<MethodIconState>,
    settlement: List<MethodIconState>,
) {
    val iconSize = 18.dp
    val spacing = 10.dp

    // In the collapsed header, show only the currently selected methods;
    // the tinted background already indicates that filters are active.
    val paymentSelected = payment.filter { it.selected }
    val settlementSelected = settlement.filter { it.selected }

    Layout(
        content = {
            // Left icons (selected payment methods)
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clipToBounds(),
            ) {
                paymentSelected.forEach { item ->
                    FilterIcon(item = item, size = iconSize, compact = true, onToggle = {}, isPayment = true)
                }
            }
            // Center arrow
            BisqText.BaseLight("\u2194", singleLine = true)
            // Right icons (selected settlement, cap at 2 for compact header)
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clipToBounds(),
            ) {
                settlementSelected.take(2).forEach { item ->
                    FilterIcon(item = item, size = iconSize, compact = true, onToggle = {}, isPayment = false)
                }
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth

        // Measure arrow first (natural size)
        val arrowPlaceable = measurables[1].measure(Constraints())

        // Compute full widths based on counts, icon size and spacing
        val iconSizePx = iconSize.roundToPx()
        val spacingPx = spacing.roundToPx()
        val leftCount = paymentSelected.size
        val rightCount = min(2, settlementSelected.size)

        val leftFullWidth = if (leftCount > 0) leftCount * iconSizePx + (leftCount - 1) * spacingPx else 0
        val rightFullWidth = if (rightCount > 0) rightCount * iconSizePx + (rightCount - 1) * spacingPx else 0

        val gapPx = 8.dp.roundToPx()

        // Desired centered position
        var arrowX = (width - arrowPlaceable.width) / 2
        // Bounds so both sides fit (left can push it right; right can push it left)
        val minArrowX = leftFullWidth + gapPx
        val maxArrowX = width - rightFullWidth - gapPx - arrowPlaceable.width
        // Avoid empty range crashes when content overflows available width
        arrowX =
            if (minArrowX <= maxArrowX) {
                arrowX.coerceIn(minArrowX, maxArrowX)
            } else {
                // Prioritize fitting the (small) right side fully; left will be clipped
                maxArrowX.coerceAtLeast(0)
            }

        // Now constrain left/right to the space they actually have
        val leftMax = (arrowX - gapPx).coerceAtLeast(0)
        val rightMax = (width - (arrowX + arrowPlaceable.width + gapPx)).coerceAtLeast(0)

        val leftPlaceable = measurables[0].measure(Constraints(maxWidth = leftMax))
        val rightPlaceable = measurables[2].measure(Constraints(maxWidth = rightMax))

        val layoutHeight = maxOf(arrowPlaceable.height, leftPlaceable.height, rightPlaceable.height)

        layout(width, layoutHeight) {
            val centerY = (layoutHeight - arrowPlaceable.height) / 2
            // Place arrow
            arrowPlaceable.place(arrowX, centerY)

            // Place left hugging arrow
            val leftY = (layoutHeight - leftPlaceable.height) / 2
            val leftX = (arrowX - gapPx - leftPlaceable.width).coerceAtLeast(0)
            leftPlaceable.place(leftX, leftY)

            // Place right hugging arrow
            val rightY = (layoutHeight - rightPlaceable.height) / 2
            val rightX = (arrowX + arrowPlaceable.width + gapPx)
            rightPlaceable.place(rightX, rightY)
        }
    }
}

@Composable
private fun FilterIcon(
    item: MethodIconState,
    size: Dp,
    compact: Boolean,
    onToggle: (String) -> Unit,
    isPayment: Boolean = false,
) {
    val alpha = if (item.selected) 1f else 0.35f

    var boxModifier =
        Modifier
            .size(size)
            .semantics { contentDescription = "filter_icon_${item.id}" }
    if (!compact) {
        boxModifier =
            boxModifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onToggle(item.id) }
    }

    Box(modifier = boxModifier, contentAlignment = Alignment.Center) {
        PaymentMethodIcon(
            methodId = item.id,
            isPaymentMethod = isPayment,
            size = size,
            alpha = alpha,
            contentDescription = item.label,
            useStyledText = true,
            iconPathOverride = item.iconPath,
        )
    }
}

@Composable
private fun MethodChip(
    item: MethodIconState,
    isPaymentRow: Boolean,
    onToggle: (String) -> Unit,
    height: Dp,
) {
    val shape =
        androidx.compose.foundation.shape
            .RoundedCornerShape(50)
    val containerColor = if (item.selected) BisqTheme.colors.secondary else BisqTheme.colors.mid_grey10
    val borderColor = if (item.selected) BisqTheme.colors.primary else Color.Transparent

    val chipModifier =
        Modifier
            .heightIn(min = height)
            .then(
                Modifier.border(
                    BorderStroke(1.dp, borderColor),
                    shape,
                ),
            ).background(containerColor, shape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onToggle(item.id) }
            .semantics {
                contentDescription = item.label
                selected = item.selected
            }.padding(horizontal = 10.dp)

    val iconSize = if (height >= 28.dp) 16.dp else 14.dp

    Row(
        modifier = chipModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PaymentMethodIcon(
            methodId = item.id,
            isPaymentMethod = isPaymentRow,
            size = iconSize,
            cornerRadius = 2.dp,
            contentDescription = item.label,
            useStyledText = true,
            iconPathOverride = item.iconPath,
        )

        BisqText.BaseLight(text = item.label, singleLine = true)
    }
}

// ----------------------
// Previews with mocked data
// ----------------------

private fun mockState(
    allSelected: Boolean = true,
    partial: Boolean = false,
): OfferbookFilterUiState {
    val payments =
        listOf("SEPA", "REVOLUT", "WISE", "CASH_APP").mapIndexed { idx, id ->
            MethodIconState(
                id = id,
                label = id,
                iconPath = paymentIconPath(id),
                selected =
                    when {
                        allSelected -> true
                        partial -> idx % 2 == 0

                        else -> false
                    },
            )
        }
    val settlements =
        listOf("BTC", "LIGHTNING").mapIndexed { idx, id ->
            MethodIconState(
                id = id,
                label = id,
                iconPath = settlementIconPath(id),
                selected =
                    when {
                        allSelected -> true
                        partial -> idx % 2 == 0
                        else -> false
                    },
            )
        }
    val hasActive = payments.any { !it.selected } || settlements.any { !it.selected }
    return OfferbookFilterUiState(
        payment = payments,
        settlement = settlements,
        onlyMyOffers = false,
        hasActiveFilters = hasActive,
    )
}

@Preview
@Composable
private fun OfferbookFilterController_AllSelectedPreview() {
    val ui = mockState(allSelected = true)
    BisqTheme.Preview {
        Box(Modifier.background(BisqTheme.colors.backgroundColor).padding(12.dp)) {
            OfferbookFilterController(
                state = ui,
                onTogglePayment = {},
                onToggleSettlement = {},
                onOnlyMyOffersChange = {},
                onClearAll = {},
                onSetPaymentSelection = {},
                onSetSettlementSelection = {},
            )
        }
    }
}

@Preview
@Composable
private fun OfferbookFilterController_PartialFiltersPreview() {
    val ui = mockState(allSelected = false, partial = true)
    BisqTheme.Preview {
        Box(Modifier.background(BisqTheme.colors.backgroundColor).padding(12.dp)) {
            OfferbookFilterController(
                state = ui.copy(hasActiveFilters = true),
                onTogglePayment = {},
                onToggleSettlement = {},
                onOnlyMyOffersChange = {},
                onClearAll = {},
                onSetPaymentSelection = {},
                onSetSettlementSelection = {},
            )
        }
    }
}

@Preview
@Composable
private fun OfferbookFilterController_ManyPaymentsPreview() {
    val payments =
        listOf(
            "SEPA",
            "SEPA_INSTANT",
            "SWIFT",
            "PIX",
            "BIZUM",
            "UPI",
            "ZELLE",
            "CASH_APP",
            "WISE",
            "REVOLUT",
            "ACH_TRANSFER",
            "INTERAC_E_TRANSFER",
            "F2F",
        )
    val settlements = listOf("BTC", "LIGHTNING")

    val ui =
        OfferbookFilterUiState(
            payment =
                payments.mapIndexed { idx, id ->
                    MethodIconState(
                        id = id,
                        label = id,
                        iconPath = paymentIconPath(id),
                        selected = idx % 2 == 0,
                    )
                },
            settlement =
                settlements.mapIndexed { idx, id ->
                    MethodIconState(
                        id = id,
                        label = id,
                        iconPath = settlementIconPath(id),
                        selected = idx % 2 == 0,
                    )
                },
            onlyMyOffers = false,
            hasActiveFilters = true,
        )

    BisqTheme.Preview {
        Box(Modifier.background(BisqTheme.colors.backgroundColor).padding(12.dp)) {
            OfferbookFilterController(
                state = ui,
                onTogglePayment = {},
                onToggleSettlement = {},
                onOnlyMyOffersChange = {},
                onClearAll = {},
                onSetPaymentSelection = {},
                onSetSettlementSelection = {},
            )
        }
    }
}

@Preview
@Composable
private fun OfferbookFilterController_ExpandedPreview() {
    val ui = mockState(allSelected = false, partial = true)
    BisqTheme.Preview {
        Box(Modifier.background(BisqTheme.colors.backgroundColor).padding(12.dp)) {
            OfferbookFilterController(
                state = ui.copy(hasActiveFilters = true),
                onTogglePayment = {},
                onToggleSettlement = {},
                onOnlyMyOffersChange = {},
                onClearAll = {},
                onSetPaymentSelection = {},
                onSetSettlementSelection = {},
                initialExpanded = true,
            )
        }
    }
}
