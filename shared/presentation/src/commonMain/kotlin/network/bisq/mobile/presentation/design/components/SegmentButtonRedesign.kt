/**
 * SegmentButtonRedesign.kt — Design PoC
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * Smoother, more polished segmented button that handles long/i18n text gracefully.
 *
 * ======================================================================================
 * PROBLEMS WITH CURRENT BisqSegmentButton
 * ======================================================================================
 * 1. Uses Material3 SegmentedButton which has rigid text sizing — long labels wrap
 *    and break visually ("Complet\ned", "Cancelle\nd")
 * 2. Checkmark icon on the selected segment takes up space, pushing text further
 * 3. Pill shape from SegmentedButtonDefaults looks bulky on mobile
 * 4. No text scaling for i18n languages (German, Russian, Portuguese have longer words)
 *
 * ======================================================================================
 * DESIGN APPROACH
 * ======================================================================================
 * Custom implementation using basic Compose primitives (Row + Box + clickable)
 * instead of Material3 SegmentedButton. Key differences:
 *
 * - AutoResizeText with maxLines=1 — text shrinks to fit, never wraps
 * - No checkmark icon — selected state shown by color alone (green bg + white text)
 * - Rounded pill corners on ends, flat joins in the middle
 * - Thinner, more compact segments (44dp height instead of Material3's ~48dp)
 * - Subtle animation on selection change
 * - Horizontal padding adapts: segments get equal weight (Modifier.weight(1f))
 *
 * ======================================================================================
 * MIGRATION PATH
 * ======================================================================================
 * This is a drop-in replacement for BisqSegmentButton. Same API signature:
 *   BisqSegmentButtonV2(value, items, label, disabled, onValueChange)
 *
 * To migrate: replace BisqSegmentButton with BisqSegmentButtonV2 at call sites.
 * Once validated, rename V2 → BisqSegmentButton and delete the old one.
 */
package network.bisq.mobile.presentation.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// -------------------------------------------------------------------------------------
// Component
// -------------------------------------------------------------------------------------

@Composable
fun <T> SimulatedBisqSegmentButtonV2(
    value: T,
    items: List<Pair<T, String>>,
    modifier: Modifier = Modifier.fillMaxWidth(),
    label: String = "",
    disabled: Boolean = false,
    onValueChange: ((Pair<T, String>) -> Unit)? = null,
) {
    val selectedIndex = items.indexOfFirst { it.first == value }.coerceAtLeast(0)
    val cornerRadius = 22.dp
    val segmentHeight = 44.dp

    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            BisqText.BaseRegular(label, color = BisqTheme.colors.white)
            BisqGap.VQuarter()
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(segmentHeight)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(BisqTheme.colors.secondaryDisabled),
        ) {
            items.forEachIndexed { index, pair ->
                val isSelected = index == selectedIndex

                val bgColor by animateColorAsState(
                    targetValue =
                        if (isSelected) {
                            BisqTheme.colors.primary
                        } else {
                            Color.Transparent
                        },
                    animationSpec = tween(200),
                    label = "segmentBg",
                )

                val textColor =
                    when {
                        disabled -> BisqTheme.colors.mid_grey20
                        isSelected -> BisqTheme.colors.white
                        else -> BisqTheme.colors.light_grey50
                    }

                // Shape: pill ends on first/last, flat in the middle
                val shape =
                    when (index) {
                        0 ->
                            RoundedCornerShape(
                                topStart = cornerRadius,
                                bottomStart = cornerRadius,
                                topEnd = 4.dp,
                                bottomEnd = 4.dp,
                            )
                        items.lastIndex ->
                            RoundedCornerShape(
                                topStart = 4.dp,
                                bottomStart = 4.dp,
                                topEnd = cornerRadius,
                                bottomEnd = cornerRadius,
                            )
                        else -> RoundedCornerShape(4.dp)
                    }

                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(segmentHeight)
                            .padding(2.dp)
                            .clip(shape)
                            .background(bgColor)
                            .then(
                                if (!disabled) {
                                    Modifier.clickable { onValueChange?.invoke(pair) }
                                } else {
                                    Modifier
                                },
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    AutoResizeText(
                        text = pair.second,
                        color = textColor,
                        textStyle = BisqTheme.typography.baseRegular,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// Previews
// -------------------------------------------------------------------------------------

private enum class DemoSort(
    val label: String,
) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    AMT_HIGH("Amt ↓"),
    AMT_LOW("Amt ↑"),
}

private enum class DemoOutcome(
    val label: String,
) {
    ALL("All"),
    DONE("Completed"),
    CANCEL("Cancelled"),
    FAILED("Failed"),
}

private enum class DemoRole(
    val label: String,
) {
    ALL("All"),
    BUYER("Buyer"),
    SELLER("Seller"),
}

private enum class DemoMarketSort(
    val label: String,
) {
    MOST_OFFERS("Most offers"),
    NAME_AZ("Name A-Z"),
    NAME_ZA("Name Z-A"),
}

@ExcludeFromCoverage
@Preview(showBackground = true)
@Composable
private fun V2_DefaultState_Preview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SimulatedBisqSegmentButtonV2(
                label = "Sort by",
                value = DemoSort.NEWEST,
                items = DemoSort.entries.map { it to it.label },
            )
            SimulatedBisqSegmentButtonV2(
                label = "Filter by outcome",
                value = DemoOutcome.ALL,
                items = DemoOutcome.entries.map { it to it.label },
            )
            SimulatedBisqSegmentButtonV2(
                label = "Filter by role",
                value = DemoRole.ALL,
                items = DemoRole.entries.map { it to it.label },
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(showBackground = true)
@Composable
private fun V2_ActiveSelections_Preview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SimulatedBisqSegmentButtonV2(
                label = "Sort by",
                value = DemoSort.AMT_HIGH,
                items = DemoSort.entries.map { it to it.label },
            )
            SimulatedBisqSegmentButtonV2(
                label = "Filter by outcome",
                value = DemoOutcome.DONE,
                items = DemoOutcome.entries.map { it to it.label },
            )
            SimulatedBisqSegmentButtonV2(
                label = "Filter by role",
                value = DemoRole.SELLER,
                items = DemoRole.entries.map { it to it.label },
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(showBackground = true)
@Composable
private fun V2_Interactive_Preview() {
    BisqTheme.Preview {
        var sort by remember { mutableStateOf(DemoSort.NEWEST) }
        var outcome by remember { mutableStateOf(DemoOutcome.ALL) }
        var role by remember { mutableStateOf(DemoRole.ALL) }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SimulatedBisqSegmentButtonV2(
                label = "Sort by",
                value = sort,
                items = DemoSort.entries.map { it to it.label },
                onValueChange = { sort = it.first },
            )
            SimulatedBisqSegmentButtonV2(
                label = "Filter by outcome",
                value = outcome,
                items = DemoOutcome.entries.map { it to it.label },
                onValueChange = { outcome = it.first },
            )
            SimulatedBisqSegmentButtonV2(
                label = "Filter by role",
                value = role,
                items = DemoRole.entries.map { it to it.label },
                onValueChange = { role = it.first },
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(showBackground = true)
@Composable
private fun V2_Disabled_Preview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SimulatedBisqSegmentButtonV2(
                label = "Disabled state",
                value = DemoOutcome.DONE,
                items = DemoOutcome.entries.map { it to it.label },
                disabled = true,
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(showBackground = true)
@Composable
private fun V2_LongI18nLabels_Preview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Simulates German-length labels
            SimulatedBisqSegmentButtonV2(
                label = "Sortieren nach",
                value = "newest",
                items =
                    listOf(
                        "newest" to "Neueste zuerst",
                        "oldest" to "Älteste zuerst",
                        "amt_high" to "Betrag abst.",
                        "amt_low" to "Betrag aufst.",
                    ),
            )
            // Simulates Portuguese-length labels
            SimulatedBisqSegmentButtonV2(
                label = "Filtrar por resultado",
                value = "all",
                items =
                    listOf(
                        "all" to "Todos",
                        "completed" to "Concluídos",
                        "cancelled" to "Cancelados",
                        "failed" to "Fracassados",
                    ),
            )
            // Stress test: 5 options with long labels
            SimulatedBisqSegmentButtonV2(
                label = "Stress test (5 options)",
                value = "opt1",
                items =
                    listOf(
                        "opt1" to "Very long option one",
                        "opt2" to "Another long opt",
                        "opt3" to "Medium text",
                        "opt4" to "Short",
                        "opt5" to "Extra long label here",
                    ),
            )
        }
    }
}

@ExcludeFromCoverage
@Preview(showBackground = true)
@Composable
private fun V2_MarketFilter_Comparison_Preview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            BisqText.SmallLightGrey("Market filter (existing use case)")
            SimulatedBisqSegmentButtonV2(
                label = "Sort by",
                value = DemoMarketSort.MOST_OFFERS,
                items = DemoMarketSort.entries.map { it to it.label },
            )
            SimulatedBisqSegmentButtonV2(
                label = "Show markets",
                value = "with_offers",
                items =
                    listOf(
                        "with_offers" to "With offers",
                        "all" to "All",
                    ),
            )
        }
    }
}
