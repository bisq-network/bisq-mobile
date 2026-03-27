package network.bisq.mobile.presentation.design.offerbook_filters

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.min

/**
 * Design POC: Enhanced collapsed filter header bar with "My Offers" indicator.
 *
 * ## Changes from current production code
 * - New parameter: `onlyMyOffers: Boolean`
 * - When `onlyMyOffers` is true, a person icon appears to the right of settlement icons
 * - The custom Layout now measures 4 children instead of 3 when the person icon is present
 *
 * ## Visual layout
 * ```
 * ┌──────────────────────────────────────────────────┐
 * │ [pay1] [pay2] [pay3]  ↔  [settle1] [settle2] 👤 │
 * └──────────────────────────────────────────────────┘
 *   ↑ green tint when hasActiveFilters = true
 * ```
 *
 * The person icon uses `BisqTheme.colors.primary` (green) to match the active-filter
 * styling, making it clear this is an active filter state.
 */

private data class SimulatedMethodIcon(
    val id: String,
    val label: String,
)

/**
 * Simulated filter icon for design previews.
 * In production, this maps to the real FilterIcon composable.
 */
@Composable
private fun SimulatedFilterIcon(
    method: SimulatedMethodIcon,
    size: Dp,
) {
    Box(
        modifier =
            Modifier
                .size(size)
                .background(
                    color = BisqTheme.colors.mid_grey30,
                    shape = RoundedCornerShape(4.dp),
                ).semantics { contentDescription = "filter_icon_${method.id}" },
        contentAlignment = Alignment.Center,
    ) {
        BisqText.XSmallMedium(
            text = method.label.take(2).uppercase(),
            color = BisqTheme.colors.light_grey10,
        )
    }
}

/**
 * Enhanced collapsed header bar with optional "My Offers" person icon.
 *
 * Layout: [payment icons] ↔ [settlement icons] [👤]
 * The person icon only appears when onlyMyOffers is true.
 */
@Composable
private fun CollapsedHeaderBarDesign(
    paymentMethods: List<SimulatedMethodIcon>,
    settlementMethods: List<SimulatedMethodIcon>,
    onlyMyOffers: Boolean,
    hasActiveFilters: Boolean,
) {
    val iconSize = 18.dp
    val spacing = 10.dp

    val backgroundColor =
        if (hasActiveFilters) {
            BisqTheme.colors.primary.copy(alpha = 0.15f)
        } else {
            BisqTheme.colors.secondary
        }

    Surface(
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = backgroundColor,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { /* expand filter sheet */ },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = BisqUIConstants.ScreenPadding,
                        vertical = BisqUIConstants.ScreenPaddingHalf,
                    ),
        ) {
            Layout(
                content = {
                    // Child 0: Left icons (payment methods)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clipToBounds(),
                    ) {
                        paymentMethods.forEach { method ->
                            SimulatedFilterIcon(method = method, size = iconSize)
                        }
                    }
                    // Child 1: Center arrow
                    BisqText.BaseLight("\u2194", singleLine = true)
                    // Child 2: Right icons (settlement, capped at 2)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clipToBounds(),
                    ) {
                        settlementMethods.take(2).forEach { method ->
                            SimulatedFilterIcon(method = method, size = iconSize)
                        }
                    }
                    // Child 3: Person icon (only measured/placed when onlyMyOffers is true)
                    if (onlyMyOffers) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Only my offers filter active",
                            tint = BisqTheme.colors.primary,
                            modifier = Modifier.size(iconSize),
                        )
                    }
                },
            ) { measurables, constraints ->
                val width = constraints.maxWidth
                val gapPx = 8.dp.roundToPx()

                // Measure arrow (child 1)
                val arrowPlaceable = measurables[1].measure(Constraints())

                // Measure person icon if present (child 3)
                val hasPersonIcon = measurables.size > 3
                val personPlaceable =
                    if (hasPersonIcon) {
                        measurables[3].measure(Constraints())
                    } else {
                        null
                    }
                val personWidth = personPlaceable?.width ?: 0
                val personGap = if (hasPersonIcon) gapPx else 0

                // Compute icon counts and widths
                val iconSizePx = iconSize.roundToPx()
                val spacingPx = spacing.roundToPx()
                val leftCount = paymentMethods.size
                val rightCount = min(2, settlementMethods.size)

                val leftFullWidth =
                    if (leftCount > 0) leftCount * iconSizePx + (leftCount - 1) * spacingPx else 0
                val rightFullWidth =
                    if (rightCount > 0) rightCount * iconSizePx + (rightCount - 1) * spacingPx else 0

                // Center the arrow, accounting for person icon on the right
                var arrowX = (width - personWidth - personGap - arrowPlaceable.width) / 2
                val minArrowX = leftFullWidth + gapPx
                val maxArrowX =
                    width - rightFullWidth - personWidth - personGap - gapPx - arrowPlaceable.width
                arrowX =
                    if (minArrowX <= maxArrowX) {
                        arrowX.coerceIn(minArrowX, maxArrowX)
                    } else {
                        maxArrowX.coerceAtLeast(0)
                    }

                // Constrain children to available space
                val leftMax = (arrowX - gapPx).coerceAtLeast(0)
                val rightMax =
                    (width - (arrowX + arrowPlaceable.width + gapPx) - personWidth - personGap)
                        .coerceAtLeast(0)

                val leftPlaceable = measurables[0].measure(Constraints(maxWidth = leftMax))
                val rightPlaceable = measurables[2].measure(Constraints(maxWidth = rightMax))

                val layoutHeight =
                    maxOf(
                        arrowPlaceable.height,
                        leftPlaceable.height,
                        rightPlaceable.height,
                        personPlaceable?.height ?: 0,
                    )

                layout(width, layoutHeight) {
                    val centerY = { h: Int -> (layoutHeight - h) / 2 }

                    // Place arrow
                    arrowPlaceable.place(arrowX, centerY(arrowPlaceable.height))

                    // Place left (payment icons) hugging arrow
                    val leftX = (arrowX - gapPx - leftPlaceable.width).coerceAtLeast(0)
                    leftPlaceable.place(leftX, centerY(leftPlaceable.height))

                    // Place right (settlement icons) hugging arrow
                    val rightX = arrowX + arrowPlaceable.width + gapPx
                    rightPlaceable.place(rightX, centerY(rightPlaceable.height))

                    // Place person icon at far right
                    if (personPlaceable != null) {
                        val personX = rightX + rightPlaceable.width + personGap
                        personPlaceable.place(personX, centerY(personPlaceable.height))
                    }
                }
            }
        }
    }
}

private val samplePaymentMethods =
    listOf(
        SimulatedMethodIcon("SEPA", "SEPA"),
        SimulatedMethodIcon("REVOLUT", "Revolut"),
        SimulatedMethodIcon("ZELLE", "Zelle"),
    )

private val sampleSettlementMethods =
    listOf(
        SimulatedMethodIcon("MAIN_CHAIN", "On-chain"),
        SimulatedMethodIcon("LN", "Lightning"),
    )

@ExcludeFromCoverage
@Preview
@Composable
private fun CollapsedHeader_NoFilters_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.SmallLight("No active filters:", color = BisqTheme.colors.light_grey10)
            BisqGap.VHalf()
            CollapsedHeaderBarDesign(
                paymentMethods = samplePaymentMethods,
                settlementMethods = sampleSettlementMethods,
                onlyMyOffers = false,
                hasActiveFilters = false,
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CollapsedHeader_MethodFiltersActive_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.SmallLight(
                "Payment methods filtered (2 of 5 selected):",
                color = BisqTheme.colors.light_grey10,
            )
            BisqGap.VHalf()
            CollapsedHeaderBarDesign(
                paymentMethods = samplePaymentMethods.take(2),
                settlementMethods = sampleSettlementMethods,
                onlyMyOffers = false,
                hasActiveFilters = true,
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CollapsedHeader_OnlyMyOffers_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.SmallLight(
                "Only my offers ON (person icon visible):",
                color = BisqTheme.colors.light_grey10,
            )
            BisqGap.VHalf()
            CollapsedHeaderBarDesign(
                paymentMethods = samplePaymentMethods,
                settlementMethods = sampleSettlementMethods,
                onlyMyOffers = true,
                hasActiveFilters = true,
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CollapsedHeader_AllFiltersActive_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.SmallLight(
                "Method filters + my offers (all active):",
                color = BisqTheme.colors.light_grey10,
            )
            BisqGap.VHalf()
            CollapsedHeaderBarDesign(
                paymentMethods = samplePaymentMethods.take(1),
                settlementMethods = sampleSettlementMethods.take(1),
                onlyMyOffers = true,
                hasActiveFilters = true,
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CollapsedHeader_ManyPaymentMethods_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.SmallLight(
                "Many payment methods + my offers:",
                color = BisqTheme.colors.light_grey10,
            )
            BisqGap.VHalf()
            CollapsedHeaderBarDesign(
                paymentMethods =
                    listOf(
                        SimulatedMethodIcon("SEPA", "SEPA"),
                        SimulatedMethodIcon("REVOLUT", "Revolut"),
                        SimulatedMethodIcon("ZELLE", "Zelle"),
                        SimulatedMethodIcon("ACH", "ACH"),
                        SimulatedMethodIcon("WISE", "Wise"),
                        SimulatedMethodIcon("PAYPAL", "PayPal"),
                    ),
                settlementMethods = sampleSettlementMethods,
                onlyMyOffers = true,
                hasActiveFilters = true,
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CollapsedHeader_Comparison_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.SmallLight("Without my offers:", color = BisqTheme.colors.light_grey10)
            CollapsedHeaderBarDesign(
                paymentMethods = samplePaymentMethods.take(2),
                settlementMethods = sampleSettlementMethods,
                onlyMyOffers = false,
                hasActiveFilters = true,
            )
            BisqText.SmallLight("With my offers:", color = BisqTheme.colors.light_grey10)
            CollapsedHeaderBarDesign(
                paymentMethods = samplePaymentMethods.take(2),
                settlementMethods = sampleSettlementMethods,
                onlyMyOffers = true,
                hasActiveFilters = true,
            )
        }
    }
}
