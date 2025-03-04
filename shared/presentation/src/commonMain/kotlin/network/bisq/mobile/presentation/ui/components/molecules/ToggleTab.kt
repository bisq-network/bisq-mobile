package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.helpers.StringHelper
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun <T> ToggleTab(
    options: List<T>,
    initialOption: T,
    onStateChange: (T) -> Unit,
    getDisplayString: (T) -> String,// Custom function to display the label for each option
    modifier: Modifier = Modifier
) {
    var selectedOption by remember { mutableStateOf(initialOption) }
    val hPadding = BisqUIConstants.ScreenPadding2X
    val vPadding = BisqUIConstants.ScreenPadding

    val textWidth = StringHelper.calculateTotalWidthOfStrings(
        strings = options.map { getDisplayString(it) },
    )

    val textHeight = StringHelper.calculateMaxHeightOfStrings(
        strings = options.map { getDisplayString(it) },
        maxWidth = textWidth - hPadding
    ) + vPadding

    val slideOffset by animateDpAsState(
        targetValue = if (selectedOption == options[0]) 0.dp else textWidth + (hPadding * 2),
        animationSpec = tween(durationMillis = 300)
    )

    Surface(
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.wrapContentSize().then(
            other = modifier
        )
    ) {
        Box(
            modifier = Modifier
                .background(BisqTheme.colors.dark4)
                .padding(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .offset(x = slideOffset)
                    .background(BisqTheme.colors.primary, RoundedCornerShape(4.dp))
            ) {
                BisqText.baseRegular(
                    text = getDisplayString(selectedOption),
                    modifier = Modifier
                        .padding(horizontal = hPadding, vertical = vPadding)
                        .width(textWidth)
                        .height(textHeight)
                        .alpha(0f),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                options.forEach { option ->
                    Box(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    selectedOption = option
                                    onStateChange(selectedOption)
                                }
                            )
                            .padding(horizontal = hPadding, vertical = vPadding)
                            .height(textHeight)
                            .width(textWidth),
                        contentAlignment = Alignment.Center
                    ) {
                        BisqText.baseRegular(
                            text = getDisplayString(option),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
