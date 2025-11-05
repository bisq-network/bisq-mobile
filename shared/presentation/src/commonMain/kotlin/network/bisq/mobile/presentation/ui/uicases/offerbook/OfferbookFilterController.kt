package network.bisq.mobile.presentation.ui.uicases.offerbook

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.ui.tooling.preview.Preview

import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText

import network.bisq.mobile.presentation.ui.components.atoms.BisqCheckbox
import network.bisq.mobile.presentation.ui.components.atoms.icons.ExpandAllIcon
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

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
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = false,
) {
    var expanded by remember { mutableStateOf(initialExpanded) }

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
                ) { expanded = true }
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
            BisqBottomSheet(onDismissRequest = { expanded = false }) {
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

                    // Payments
                    BisqText.baseLight("bisqEasy.offerbook.offerList.table.columns.paymentMethod".i18n())
                    FilterIconsRow(items = state.payment, onToggle = onTogglePayment, isPaymentRow = true)

                    // Settlement
                    BisqText.baseLight("bisqEasy.offerbook.offerList.table.columns.settlementMethod".i18n())
                    FilterIconsRow(items = state.settlement, onToggle = onToggleSettlement, isPaymentRow = false)

                    // Only my offers (disabled for Phase 1 functionality-wise)
                    BisqCheckbox(
                        label = "mobile.offerbook.filters.onlyMyOffers".i18n(),
                        checked = state.onlyMyOffers,
                        disabled = true,
                        onCheckedChange = { onOnlyMyOffersChange(it) }
                    )

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
    val iconSize = if (compact) 18.dp else 24.dp

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (compact) Modifier else Modifier.fillMaxWidth()
    ) {
        items(items) { item ->
            FilterIcon(item = item, size = iconSize, compact = compact, onToggle = onToggle, isPayment = isPaymentRow)
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
            val customIds = listOf(
                "custom_payment_1",
                "custom_payment_2",
                "custom_payment_3",
                "custom_payment_4",
                "custom_payment_5",
                "custom_payment_6",
            )
            val idx = network.bisq.mobile.presentation.ui.helpers.customPaymentIconIndex(item.id, customIds.size)
            Triple(true, "drawable/payment/fiat/${customIds[idx]}.png", item.id.firstOrNull()?.uppercase() ?: "?")
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
            network.bisq.mobile.presentation.ui.components.atoms.BisqText.baseBold(
                text = overlayLetter,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = network.bisq.mobile.presentation.ui.theme.BisqTheme.colors.dark_grey20,
            )
        }
    }
}


// Helpers to build icon paths similar to offer cards
private fun paymentIconPath(id: String): String =
    "drawable/payment/fiat/${id.lowercase().replace("-", "_")}.png"

private fun settlementIconPath(id: String): String =
    when (id.uppercase()) {
        "BTC", "MAIN_CHAIN", "ONCHAIN", "ON_CHAIN" -> "drawable/payment/bitcoin/main_chain.png"
        "LIGHTNING", "LN" -> "drawable/payment/bitcoin/ln.png"
        else -> "drawable/payment/bitcoin/${id.lowercase().replace("-", "_")}.png"
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
                initialExpanded = true
            )
        }
    }
}
