package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun <T> ToggleTab(
    options: List<T>,
    initialOption: T,
    onStateChange: (T) -> Unit,
    getDisplayString: (T) -> String,// Custom function to display the label for each option
    textWidth: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    var selectedOption by remember { mutableStateOf(initialOption) }

    val slideOffset by animateDpAsState(
        targetValue = if (selectedOption == options[0]) 0.dp else textWidth + 64.dp,
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
                .background(BisqTheme.colors.dark5)
                .padding(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .offset(x = slideOffset)
                    .background(BisqTheme.colors.primary, RoundedCornerShape(4.dp))
            ) {
                BisqText.baseMedium(
                    text = getDisplayString(selectedOption),
                    color = BisqTheme.colors.light1,
                    modifier = Modifier
                        .padding(horizontal = 32.dp, vertical = 12.dp)
                        .width(textWidth)
                        .alpha(0f),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                options.forEach { option ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 32.dp, vertical = 12.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    selectedOption = option
                                    onStateChange(selectedOption)
                                }
                            )
                    ) {
                        BisqText.baseMedium(
                            text = getDisplayString(option),
                            color = BisqTheme.colors.light1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(textWidth)
                        )
                    }
                }
            }
        }
    }
}
