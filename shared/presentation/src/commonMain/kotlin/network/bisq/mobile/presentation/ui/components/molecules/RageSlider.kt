package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BisqRangeSlider(
    minAmount: Float,
    maxAmount: Float,
    tradeValue: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit
){
    val colors = SliderColors(
        thumbColor = BisqTheme.colors.primary,
        activeTrackColor = BisqTheme.colors.grey2,
        activeTickColor = Color.Unspecified,
        inactiveTrackColor = BisqTheme.colors.grey2,
        inactiveTickColor = Color.Unspecified,
        disabledThumbColor = Color.Unspecified,
        disabledActiveTrackColor = Color.Unspecified,
        disabledActiveTickColor = Color.Unspecified,
        disabledInactiveTrackColor = Color.Unspecified,
        disabledInactiveTickColor = Color.Unspecified
    )

    RangeSlider(
        modifier = Modifier.fillMaxWidth(),
        value = tradeValue,
        onValueChange = {onValueChange(it)},
        valueRange = minAmount .. maxAmount,
        track = { rangeSliderState  ->
            SliderDefaults.Track(
                trackInsideCornerSize = 0.dp,
                thumbTrackGapSize = 0.dp,
                modifier = Modifier.height(2.dp),
                rangeSliderState  = rangeSliderState ,
                colors = colors,
                drawStopIndicator = null
            )
        },
        startThumb = {
            SliderDefaults.Thumb(
                interactionSource = remember { MutableInteractionSource() },
                thumbSize = DpSize(16.dp, 16.dp),
                colors = colors
            )
        },
        endThumb = {
            SliderDefaults.Thumb(
                interactionSource = remember { MutableInteractionSource() },
                thumbSize = DpSize(16.dp, 16.dp),
                colors = colors
            )
        }
    )
}