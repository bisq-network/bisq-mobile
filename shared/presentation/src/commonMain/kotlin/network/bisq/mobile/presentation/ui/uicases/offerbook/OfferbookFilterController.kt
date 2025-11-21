package network.bisq.mobile.presentation.ui.uicases.offerbook

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.heightIn


import network.bisq.mobile.presentation.ui.platform.platformTextStyleNoFontPadding

import androidx.compose.foundation.layout.width


import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds

import androidx.compose.ui.semantics.selected

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.ui.tooling.preview.Preview

import androidx.compose.ui.unit.dp

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText

import network.bisq.mobile.presentation.ui.components.atoms.BisqCheckbox
import network.bisq.mobile.presentation.ui.components.atoms.icons.ExpandAllIcon
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import network.bisq.mobile.presentation.ui.components.molecules.inputfield.BisqSearchField


private val CUSTOM_PAYMENT_IDS = listOf(
    "custom_payment_1",
    "custom_payment_2",
    "custom_payment_3",
    "custom_payment_4",
    "custom_payment_5",
    "custom_payment_6",
)

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
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onExpandedChange?.invoke(true) ?: run { internalExpanded = true }
                }
                .semantics { contentDescription = "offerbook_filters_header" }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BisqUIConstants.ScreenPadding, vertical = BisqUIConstants.ScreenPaddingHalf)
            ) {
                // Dynamic collapsed header: icons hug the ↔ centers by default, reserve space for chevron
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 32.dp)
                ) {
                    CollapsedHeaderBar(
                        payment = state.payment,
                        settlement = state.settlement,
                    )
                }
                ExpandAllIcon(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(20.dp)
                )
            }
        }

        // Bottom sheet content
        if (expanded) {
            BisqBottomSheet(onDismissRequest = {
                onExpandedChange?.invoke(false) ?: run { internalExpanded = false }
            }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = BisqUIConstants.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BisqText.h4Light(
                            text = "mobile.offerbook.filters.title".i18n(),
                            modifier = Modifier.weight(1f)
                        )
                        val clearEnabled = state.hasActiveFilters
                        val clearAlpha = if (clearEnabled) 1f else 0.4f
                        BisqText.baseRegular(
                            text = "bisqEasy.offerbook.offerList.table.filters.paymentMethods.clearFilters".i18n(),
                            underline = true,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .alpha(clearAlpha)
                                .clickable(enabled = clearEnabled) { onClearAll() }
)
                    }
                    var search by rememberSaveable { mutableStateOf("") }
                    BisqSearchField(
                        value = search,
                        onValueChanged = { text, _ -> search = text },
                        placeholder = "action.search".i18n()
                    )

                    val filteredPayment = if (search.isBlank()) state.payment else state.payment.filter {
                        it.label.contains(search, ignoreCase = true) || it.id.contains(search, ignoreCase = true)
                    }
                    val filteredSettlement = if (search.isBlank()) state.settlement else state.settlement.filter {
                        it.label.contains(search, ignoreCase = true) || it.id.contains(search, ignoreCase = true)
                    }

                    val selectedPaymentIds = state.payment.filter { it.selected }.map { it.id }.toSet()
                    val visiblePaymentIds = filteredPayment.map { it.id }.toSet()
                    val canSelectAllPayment = visiblePaymentIds.any { it !in selectedPaymentIds }
                    val canSelectNonePayment = visiblePaymentIds.any { it in selectedPaymentIds }

                    val selectedSettlementIds = state.settlement.filter { it.selected }.map { it.id }.toSet()
                    val visibleSettlementIds = filteredSettlement.map { it.id }.toSet()
                    val canSelectAllSettlement = visibleSettlementIds.any { it !in selectedSettlementIds }
                    val canSelectNoneSettlement = visibleSettlementIds.any { it in selectedSettlementIds }

                    // Payments
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        BisqText.baseLight("bisqEasy.offerbook.offerList.table.columns.paymentMethod".i18n())
                        Spacer(Modifier.weight(1f))
                        val alphaAllP = if (canSelectAllPayment) 1f else 0.4f
                        BisqText.baseRegular(
                            text = "mobile.offerbook.filters.selectAll".i18n(),
                            underline = true,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .alpha(alphaAllP)
                                .clickable(enabled = canSelectAllPayment) {
                                    val newSet = selectedPaymentIds + visiblePaymentIds
                                    onSetPaymentSelection(newSet)
                                }
                        )
                        Spacer(Modifier.width(12.dp))
                        val alphaNoneP = if (canSelectNonePayment) 1f else 0.4f
                        BisqText.baseRegular(
                            text = "mobile.offerbook.filters.selectNone".i18n(),
                            underline = true,
                            modifier = Modifier
                                .alpha(alphaNoneP)
                                .clickable(enabled = canSelectNonePayment) {
                                    val newSet = selectedPaymentIds - visiblePaymentIds
                                    onSetPaymentSelection(newSet)
                                }
                        )
                    }
                    FilterIconsRow(items = filteredPayment, onToggle = onTogglePayment, isPaymentRow = true)

                    // Settlement
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        BisqText.baseLight("bisqEasy.offerbook.offerList.table.columns.settlementMethod".i18n())
                        Spacer(Modifier.weight(1f))
                        val alphaAllS = if (canSelectAllSettlement) 1f else 0.4f
                        BisqText.baseRegular(
                            text = "mobile.offerbook.filters.selectAll".i18n(),
                            underline = true,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .alpha(alphaAllS)
                                .clickable(enabled = canSelectAllSettlement) {
                                    val newSet = selectedSettlementIds + visibleSettlementIds
                                    onSetSettlementSelection(newSet)
                                }
                        )
                        Spacer(Modifier.width(12.dp))
                        val alphaNoneS = if (canSelectNoneSettlement) 1f else 0.4f
                        BisqText.baseRegular(
                            text = "mobile.offerbook.filters.selectNone".i18n(),
                            underline = true,
                            modifier = Modifier
                                .alpha(alphaNoneS)
                                .clickable(enabled = canSelectNoneSettlement) {
                                    val newSet = selectedSettlementIds - visibleSettlementIds
                                    onSetSettlementSelection(newSet)
                                }
                        )
                    }
                    FilterIconsRow(items = filteredSettlement, onToggle = onToggleSettlement, isPaymentRow = false)


                    // Only my offers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BisqText.baseLight(
                            "mobile.offerbook.filters.onlyMyOffers".i18n(),
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    enabled = true,
                                    onClick = { onOnlyMyOffersChange(!state.onlyMyOffers) },
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                )
                        )
                        androidx.compose.material3.Checkbox(
                            checked = state.onlyMyOffers,
                            onCheckedChange = onOnlyMyOffersChange,
                            colors = androidx.compose.material3.CheckboxColors(
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
                            )
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

    Box(modifier = rowModifier) {
        if (items.isEmpty()) {
            // Stable empty state: no layout jump, subtle hint
            BisqText.baseLight(
                text = if (isPaymentRow)
                    "mobile.offerbook.filters.noPaymentMatches".i18n()
                else
                    "mobile.offerbook.filters.noSettlementMatches".i18n(),
                color = BisqTheme.colors.mid_grey20,
                modifier = Modifier
                    .align(Alignment.Center)
                    .semantics {
                        contentDescription = if (isPaymentRow) "no_payment_matches" else "no_settlement_matches"
                    }
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
                modifier = Modifier.fillMaxWidth().height(rowHeight)
            ) {
                items(items, key = { it.id }) { item ->
                    if (compact) {
                        // Collapsed header: keep icon-only
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
                    modifier = Modifier
                        .height(rowHeight)
                        .width(fadeWidth)
                        .align(Alignment.CenterStart)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(BisqTheme.colors.dark_grey50, Color.Transparent)
                            )
                        )
                )
            }
            if (canScrollForward) {
                Box(
                    modifier = Modifier
                        .height(rowHeight)
                        .width(fadeWidth)
                        .align(Alignment.CenterEnd)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, BisqTheme.colors.dark_grey50)
                            )
                        )
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

    androidx.compose.ui.layout.Layout(
        content = {
            // Left icons (all payment methods)
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clipToBounds()
            ) {
                payment.forEach { item ->
                    FilterIcon(item = item, size = iconSize, compact = true, onToggle = {}, isPayment = true)
                }
            }
            // Center arrow
            BisqText.baseLight("\u2194", singleLine = true)
            // Right icons (settlement, cap at 2 for compact header)
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clipToBounds()
            ) {
                settlement.take(2).forEach { item ->
                    FilterIcon(item = item, size = iconSize, compact = true, onToggle = {}, isPayment = false)
                }
            }
        }
    ) { measurables, constraints ->
        val width = constraints.maxWidth

        // Measure arrow first (natural size)
        val arrowPlaceable = measurables[1].measure(androidx.compose.ui.unit.Constraints())

        // Compute full widths based on counts, icon size and spacing
        val iconSizePx = iconSize.roundToPx()
        val spacingPx = spacing.roundToPx()
        val leftCount = payment.size
        val rightCount = kotlin.math.min(2, settlement.size)

        val leftFullWidth = if (leftCount > 0) leftCount * iconSizePx + (leftCount - 1) * spacingPx else 0
        val rightFullWidth = if (rightCount > 0) rightCount * iconSizePx + (rightCount - 1) * spacingPx else 0

        val gapPx = 8.dp.roundToPx()

        // Desired centered position
        var arrowX = (width - arrowPlaceable.width) / 2
        // Bounds so both sides fit (left can push it right; right can push it left)
        val minArrowX = leftFullWidth + gapPx
        val maxArrowX = width - rightFullWidth - gapPx - arrowPlaceable.width
        // Avoid empty range crashes when content overflows available width
        arrowX = if (minArrowX <= maxArrowX) {
            arrowX.coerceIn(minArrowX, maxArrowX)
        } else {
            // Prioritize fitting the (small) right side fully; left will be clipped
            maxArrowX.coerceAtLeast(0)
        }

        // Now constrain left/right to the space they actually have
        val leftMax = (arrowX - gapPx).coerceAtLeast(0)
        val rightMax = (width - (arrowX + arrowPlaceable.width + gapPx)).coerceAtLeast(0)

        val leftPlaceable = measurables[0].measure(androidx.compose.ui.unit.Constraints(maxWidth = leftMax))
        val rightPlaceable = measurables[2].measure(androidx.compose.ui.unit.Constraints(maxWidth = rightMax))

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
    size: androidx.compose.ui.unit.Dp,
    compact: Boolean,
    onToggle: (String) -> Unit,
    isPayment: Boolean = false,
) {
    val alpha = if (item.selected) 1f else 0.35f

    // For unknown payment methods, mirror the fallback used in PaymentMethods:
    // - Use one of the custom_payment_* icons deterministically
    // - Overlay the first letter as a label
    val (isMissingPayment, fallbackPath, overlayLetter) = if (isPayment) {
        val (_, missing) = network.bisq.mobile.presentation.ui.helpers.i18NPaymentMethod(item.id)
        if (missing) {
            val idx = network.bisq.mobile.presentation.ui.helpers.customPaymentIconIndex(item.id, CUSTOM_PAYMENT_IDS.size)
            Triple(true, "drawable/payment/fiat/${CUSTOM_PAYMENT_IDS[idx]}.png", item.id.firstOrNull()?.uppercase() ?: "?")
        } else Triple(false, null, null)
    } else Triple(false, null, null)

    val inPreview = androidx.compose.ui.platform.LocalInspectionMode.current
    var imageModifier = Modifier
        .size(size)
        .alpha(alpha)
        .semantics { contentDescription = "filter_icon_${item.id}" }
    if (!compact) {
        imageModifier = imageModifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onToggle(item.id) }
    }

    Box(modifier = imageModifier, contentAlignment = Alignment.Center) {
        if (inPreview) {
            val previewPath = fallbackPath ?: item.iconPath
            androidx.compose.foundation.Image(
                painter = org.jetbrains.compose.resources.painterResource(previewDrawableFromPath(previewPath)),


                contentDescription = item.label,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            DynamicImage(
                path = item.iconPath,
                fallbackPath = fallbackPath,
                contentDescription = item.label,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (isMissingPayment && overlayLetter != null) {
            val letterSizeSp = if (size < 16.dp) 11f else 12f
            // Use tighter lineHeight and no font padding to keep the letter visually centered in small boxes
            BisqText.styledText(
                text = overlayLetter,
                style = BisqTheme.typography.baseBold.copy(
                    fontSize = TextUnit(letterSizeSp, androidx.compose.ui.unit.TextUnitType.Sp),
                    lineHeight = TextUnit(letterSizeSp, androidx.compose.ui.unit.TextUnitType.Sp),
                    platformStyle = platformTextStyleNoFontPadding()
                ),
                textAlign = TextAlign.Center,
                color = BisqTheme.colors.dark_grey20,
            )
        }
    }
}



@Composable
private fun MethodChip(
    item: MethodIconState,
    isPaymentRow: Boolean,
    onToggle: (String) -> Unit,
    height: androidx.compose.ui.unit.Dp,
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
    val containerColor = if (item.selected) BisqTheme.colors.secondary else BisqTheme.colors.mid_grey10
    val borderColor = if (item.selected) BisqTheme.colors.primary else Color.Transparent

    val chipModifier = Modifier
        .heightIn(min = height)
        .then(
            Modifier.border(
                androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                shape
            )
        )
        .background(containerColor, shape)
        .clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onToggle(item.id) }
        .semantics {
            contentDescription = item.label
            selected = item.selected
        }
        .padding(horizontal = 10.dp)

    val iconSize = if (height >= 28.dp) 16.dp else 14.dp

    Row(
        modifier = chipModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Always provide a fallback custom icon for payment methods; overlay letter only when i18n is missing.
        val (isMissingPayment, overlayLetter) = if (isPaymentRow) {
            val (_, missing) = network.bisq.mobile.presentation.ui.helpers.i18NPaymentMethod(item.id)
            Pair(missing, item.id.firstOrNull()?.uppercase() ?: "?")
        } else Pair(false, null)
        val idx = network.bisq.mobile.presentation.ui.helpers.customPaymentIconIndex(item.id, CUSTOM_PAYMENT_IDS.size)
        val paymentFallbackPath = if (isPaymentRow) "drawable/payment/fiat/${CUSTOM_PAYMENT_IDS[idx]}.png" else null

        Box(modifier = Modifier.size(iconSize), contentAlignment = Alignment.Center) {
            DynamicImage(
                path = item.iconPath,
                fallbackPath = paymentFallbackPath,
                contentDescription = item.label,
                modifier = Modifier.size(iconSize)
            )
            if (isMissingPayment && overlayLetter != null) {
                val letterSizeSp = if (iconSize < 16.dp) 11f else 12f
                // Use tighter lineHeight and no font padding to keep the letter visually centered in small boxes
                network.bisq.mobile.presentation.ui.components.atoms.BisqText.styledText(
                    text = overlayLetter,
                    style = network.bisq.mobile.presentation.ui.theme.BisqTheme.typography.baseBold.copy(
                        fontSize = androidx.compose.ui.unit.TextUnit(letterSizeSp, androidx.compose.ui.unit.TextUnitType.Sp),
                        lineHeight = androidx.compose.ui.unit.TextUnit(letterSizeSp, androidx.compose.ui.unit.TextUnitType.Sp),
                        platformStyle = platformTextStyleNoFontPadding()
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = network.bisq.mobile.presentation.ui.theme.BisqTheme.colors.dark_grey20,
                )
            }
        }

        BisqText.baseLight(text = item.label, singleLine = true)
    }
}


// Preview-only helper: build a DrawableResource from a path without typed accessors
@OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)
private fun previewDrawableFromPath(path: String): org.jetbrains.compose.resources.DrawableResource {
    val full = "composeResources/bisqapps.shared.presentation.generated.resources/" + path
    return org.jetbrains.compose.resources.DrawableResource(
        "drawable:preview_$path",
        setOf(org.jetbrains.compose.resources.ResourceItem(emptySet(), full, -1, -1))
    )
}

// ----------------------
// Previews with mocked data
// ----------------------

private fun mockState(allSelected: Boolean = true, partial: Boolean = false): OfferbookFilterUiState {
    val payments = listOf("SEPA", "REVOLUT", "WISE", "CASH_APP").mapIndexed { idx, id ->
        MethodIconState(
            id = id,
            label = id,
            iconPath = paymentIconPath(id),
            selected = when {
                allSelected -> true
                partial -> idx % 2 == 0

                else -> false
            }
        )
    }
    val settlements = listOf("BTC", "LIGHTNING").mapIndexed { idx, id ->
        MethodIconState(
            id = id,
            label = id,
            iconPath = settlementIconPath(id),
            selected = when {
                allSelected -> true
                partial -> idx % 2 == 0
                else -> false
            }
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
private fun Preview_OfferbookFilterController_AllSelected() {
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
private fun Preview_OfferbookFilterController_PartialFilters() {
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
private fun Preview_OfferbookFilterController_ManyPayments() {
    val payments = listOf(
        "SEPA", "SEPA_INSTANT", "SWIFT", "PIX", "BIZUM", "UPI", "ZELLE",
        "CASH_APP", "WISE", "REVOLUT", "ACH_TRANSFER", "INTERAC_E_TRANSFER", "F2F"
    )
    val settlements = listOf("BTC", "LIGHTNING")

    val ui = OfferbookFilterUiState(
        payment = payments.mapIndexed { idx, id ->
            MethodIconState(
                id = id,
                label = id,
                iconPath = paymentIconPath(id),
                selected = idx % 2 == 0
            )
        },
        settlement = settlements.mapIndexed { idx, id ->
            MethodIconState(
                id = id,
                label = id,
                iconPath = settlementIconPath(id),
                selected = idx % 2 == 0
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
private fun Preview_OfferbookFilterController_Expanded() {
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
                initialExpanded = true
            )
        }
    }
}
