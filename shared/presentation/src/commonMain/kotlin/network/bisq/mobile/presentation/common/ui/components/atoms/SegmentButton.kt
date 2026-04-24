package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

private val CornerRadius = 22.dp
private val FlatRadius = BisqUIConstants.BorderRadiusSmall
private val SegmentHeight = 44.dp
private val InnerPadding = BisqUIConstants.ScreenPadding2
private val TextHorizontalPadding = BisqUIConstants.ScreenPaddingHalf
private const val ANIMATION_DURATION_MS = 300

@Composable
fun <T> BisqSegmentButton(
    value: T,
    items: List<Pair<T, String>>,
    modifier: Modifier = Modifier.fillMaxWidth(),
    label: String = "",
    disabled: Boolean = false,
    onValueChange: ((Pair<T, String>) -> Unit)? = null,
) {
    val selectedIndex = items.indexOfFirst { it.first == value }.coerceAtLeast(0)

    val density = LocalDensity.current
    val innerMinHeight = SegmentHeight - InnerPadding * 2

    val optionWidths = remember(items) { mutableStateMapOf<Int, Int>() }
    val optionOffsets = remember(items) { mutableStateMapOf<Int, Int>() }
    var containerWidthPx by remember { mutableIntStateOf(0) }
    var rowHeightPx by remember { mutableIntStateOf(0) }
    val rowHeightDp = with(density) { rowHeightPx.toDp() }

    val selectedWidthPx = optionWidths[selectedIndex] ?: 0
    val selectedOffsetPx = optionOffsets[selectedIndex] ?: 0

    val selectedWidthDp = with(density) { selectedWidthPx.toDp() }
    val selectedOffsetDp = with(density) { selectedOffsetPx.toDp() }

    var measured by remember(items) { mutableStateOf(false) }
    LaunchedEffect(items, selectedWidthPx) {
        if (!measured && selectedWidthPx > 0) measured = true
    }

    val spec =
        remember(measured) {
            if (measured) tween<Dp>(durationMillis = ANIMATION_DURATION_MS) else snap()
        }
    val animatedWidth by animateDpAsState(targetValue = selectedWidthDp, animationSpec = spec)
    val animatedOffset by animateDpAsState(targetValue = selectedOffsetDp, animationSpec = spec)

    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            BisqText.BaseRegular(label, color = BisqTheme.colors.white)
            BisqGap.VQuarter()
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = SegmentHeight)
                    .clip(RoundedCornerShape(CornerRadius))
                    .background(BisqTheme.colors.secondaryDisabled)
                    .padding(InnerPadding)
                    .onGloballyPositioned { containerWidthPx = it.size.width },
        ) {
            val containerWidthDp = with(density) { containerWidthPx.toDp() }
            val innerContainerWidthDp = (containerWidthDp - InnerPadding * 2).coerceAtLeast(0.dp)
            val leftDistance = animatedOffset.coerceAtLeast(0.dp)
            val rightDistance =
                (innerContainerWidthDp - animatedOffset - animatedWidth).coerceAtLeast(0.dp)
            val leftCorner = (CornerRadius - leftDistance).coerceIn(FlatRadius, CornerRadius)
            val rightCorner = (CornerRadius - rightDistance).coerceIn(FlatRadius, CornerRadius)
            val indicatorShape =
                AbsoluteRoundedCornerShape(
                    topLeft = leftCorner,
                    bottomLeft = leftCorner,
                    topRight = rightCorner,
                    bottomRight = rightCorner,
                )

            if (selectedWidthPx > 0) {
                Box(
                    modifier =
                        Modifier
                            .absoluteOffset(x = animatedOffset)
                            .width(animatedWidth)
                            .height(rowHeightDp)
                            .clip(indicatorShape)
                            .background(BisqTheme.colors.primary),
                )
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                        .onGloballyPositioned { rowHeightPx = it.size.height },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, pair ->
                    val isSelected = index == selectedIndex

                    val textColor by animateColorAsState(
                        targetValue =
                            when {
                                disabled -> BisqTheme.colors.mid_grey10
                                isSelected -> BisqTheme.colors.white
                                else -> BisqTheme.colors.light_grey50
                            },
                        animationSpec = tween(ANIMATION_DURATION_MS),
                        label = "segmentText",
                    )

                    val segmentShape =
                        when (index) {
                            0 ->
                                RoundedCornerShape(
                                    topStart = CornerRadius,
                                    bottomStart = CornerRadius,
                                    topEnd = FlatRadius,
                                    bottomEnd = FlatRadius,
                                )

                            items.lastIndex ->
                                RoundedCornerShape(
                                    topStart = FlatRadius,
                                    bottomStart = FlatRadius,
                                    topEnd = CornerRadius,
                                    bottomEnd = CornerRadius,
                                )

                            else -> RoundedCornerShape(FlatRadius)
                        }

                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = innerMinHeight)
                                .onGloballyPositioned { coordinates ->
                                    optionWidths[index] = coordinates.size.width
                                    optionOffsets[index] = coordinates.positionInParent().x.toInt()
                                }.clip(segmentShape)
                                .selectable(
                                    selected = isSelected,
                                    enabled = !disabled,
                                    role = Role.RadioButton,
                                    onClick = { onValueChange?.invoke(pair) },
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        AutoResizeText(
                            text = pair.second,
                            color = textColor,
                            textStyle = BisqTheme.typography.baseRegular,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            modifier = Modifier.padding(horizontal = TextHorizontalPadding),
                        )
                    }
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
    AMT_HIGH("Amt↓"),
    AMT_LOW("Amt↑"),
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

@ExcludeFromCoverage
@Preview(showBackground = true)
@Preview(showBackground = true, fontScale = 3f)
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
            BisqSegmentButton(
                label = "Sort by",
                value = sort,
                items = DemoSort.entries.map { it to it.label },
                onValueChange = { sort = it.first },
            )
            BisqSegmentButton(
                label = "Filter by outcome",
                value = outcome,
                items = DemoOutcome.entries.map { it to it.label },
                onValueChange = { outcome = it.first },
            )
            BisqSegmentButton(
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
            BisqSegmentButton(
                label = "Disabled state",
                value = DemoOutcome.DONE,
                items = DemoOutcome.entries.map { it to it.label },
                disabled = true,
            )
        }
    }
}
