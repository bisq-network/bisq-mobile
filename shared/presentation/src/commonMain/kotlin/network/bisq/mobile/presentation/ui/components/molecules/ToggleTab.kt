package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun <T> ToggleTab(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    getDisplayString: (T) -> String,// Custom function to display the label for each option
    singleLine: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hPadding = BisqUIConstants.ScreenPadding
    val vPadding = BisqUIConstants.ScreenPadding

    val density = LocalDensity.current

    // Store the width and x offset of each option
    val optionWidths = remember { mutableStateMapOf<T, Int>() }
    val optionHeights = remember { mutableStateMapOf<T, Int>() }
    val optionOffsets = remember { mutableStateMapOf<T, Int>() }

    // Get the width and offset for the selected option
    val selectedWidthPx = optionWidths[selectedOption] ?: 0
    val selectedOffsetPx = optionOffsets[selectedOption] ?: 0

    val selectedWidthDp = with(density) { selectedWidthPx.toDp() }
    val selectedOffsetDp = with(density) { selectedOffsetPx.toDp() }

    // Track if it's the first render
    var isFirstRender by remember { mutableStateOf(true) }

    LaunchedEffect(selectedOption, selectedWidthDp, selectedOffsetDp) {
        if (isFirstRender && selectedWidthPx > 0 && selectedOffsetPx >= 0) {
            isFirstRender = false
        }
    }

    val animatedWidth by animateDpAsState(
        targetValue = selectedWidthDp,
        animationSpec = if (isFirstRender) snap() else tween(durationMillis = 300)
    )
    val animatedOffset by animateDpAsState(
        targetValue = selectedOffsetDp,
        animationSpec = if (isFirstRender) snap() else tween(durationMillis = 300)
    )

    // Find the maximum height among all options (in px)
    val maxHeightPx = optionHeights.values.maxOrNull() ?: 0
    val maxHeightDp = with(density) { maxHeightPx.toDp() }

    Surface(
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.wrapContentSize().then(
            other = modifier
        )
    ) {
        Box(
            modifier = Modifier
                .background(BisqTheme.colors.dark_grey40)
                .padding(6.dp)
        ) {

            Box(
                modifier = Modifier
                    .offset(x = animatedOffset)
                    .width(animatedWidth)
                    .height(maxHeightDp)
                    .background(BisqTheme.colors.primaryDim, RoundedCornerShape(4.dp))
                    .wrapContentSize()
            ) {
                BisqText.baseRegular(
                    text = getDisplayString(selectedOption),
                    modifier = Modifier
                        .padding(horizontal = hPadding, vertical = vPadding)
                        .alpha(0f)
                        .clearAndSetSemantics { },
                    singleLine = singleLine
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                options.forEach { option ->
                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                optionWidths[option] = coordinates.size.width
                                optionHeights[option] = coordinates.size.height
                                optionOffsets[option] = coordinates.positionInParent().x.toInt()
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    onOptionSelected(option)
                                }
                            )
                            .padding(horizontal = hPadding, vertical = vPadding)
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        BisqText.baseRegular(
                            text = getDisplayString(option),
                            textAlign = TextAlign.Center,
                            singleLine = singleLine
                        )
                    }
                }
            }
        }
    }
}
