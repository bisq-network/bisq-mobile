package network.bisq.mobile.presentation.common.ui.components.atoms.slider

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.RangeSliderState
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BisqRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val thumbColor = if (enabled) BisqTheme.colors.primary else BisqTheme.colors.mid_grey20

    val customThumb: @Composable (RangeSliderState) -> Unit = { _ ->
        Box(
            modifier =
                Modifier
                    .width(4.dp)
                    .height(26.dp)
                    .background(
                        color = thumbColor,
                        shape = RoundedCornerShape(2.dp),
                    ),
        )
    }

    RangeSlider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished,
        enabled = enabled,
        startThumb = customThumb,
        endThumb = customThumb,
        colors =
            SliderDefaults.colors(
                thumbColor = BisqTheme.colors.primary,
                activeTrackColor = BisqTheme.colors.primary,
                inactiveTrackColor = BisqTheme.colors.mid_grey10,
                disabledThumbColor = BisqTheme.colors.mid_grey20,
                disabledActiveTrackColor = BisqTheme.colors.mid_grey10,
                disabledInactiveTrackColor = BisqTheme.colors.dark_grey30,
            ),
    )
}

@Preview
@Composable
private fun BisqRangeSlider_DefaultPreview() {
    BisqTheme.Preview {
        val rangeValue = remember { mutableStateOf(0.25f..0.75f) }
        Column(modifier = Modifier.padding(16.dp)) {
            BisqText.BaseRegular("Default (0.25f..0.75f)")
            Spacer(modifier = Modifier.height(8.dp))
            BisqRangeSlider(
                value = rangeValue.value,
                onValueChange = { rangeValue.value = it },
            )
            Spacer(modifier = Modifier.height(4.dp))
            BisqText.SmallRegular("Range: ${rangeValue.value.start}..${rangeValue.value.endInclusive}")
        }
    }
}

@Preview
@Composable
private fun BisqRangeSlider_MinRangePreview() {
    BisqTheme.Preview {
        val rangeValue = remember { mutableStateOf(0f..0.1f) }
        Column(modifier = Modifier.padding(16.dp)) {
            BisqText.BaseRegular("Min Range (0f..0.1f)")
            Spacer(modifier = Modifier.height(8.dp))
            BisqRangeSlider(
                value = rangeValue.value,
                onValueChange = { rangeValue.value = it },
            )
            Spacer(modifier = Modifier.height(4.dp))
            BisqText.SmallRegular("Range: ${rangeValue.value.start}..${rangeValue.value.endInclusive}")
        }
    }
}

@Preview
@Composable
private fun BisqRangeSlider_MaxRangePreview() {
    BisqTheme.Preview {
        val rangeValue = remember { mutableStateOf(0.9f..1f) }
        Column(modifier = Modifier.padding(16.dp)) {
            BisqText.BaseRegular("Max Range (0.9f..1f)")
            Spacer(modifier = Modifier.height(8.dp))
            BisqRangeSlider(
                value = rangeValue.value,
                onValueChange = { rangeValue.value = it },
            )
            Spacer(modifier = Modifier.height(4.dp))
            BisqText.SmallRegular("Range: ${rangeValue.value.start}..${rangeValue.value.endInclusive}")
        }
    }
}

@Preview
@Composable
private fun BisqRangeSlider_DisabledPreview() {
    BisqTheme.Preview {
        val rangeValue = remember { mutableStateOf(0.25f..0.75f) }
        Column(modifier = Modifier.padding(16.dp)) {
            BisqText.BaseRegular("Disabled (enabled=false)")
            Spacer(modifier = Modifier.height(8.dp))
            BisqRangeSlider(
                value = rangeValue.value,
                onValueChange = { rangeValue.value = it },
                enabled = false,
            )
            Spacer(modifier = Modifier.height(4.dp))
            BisqText.SmallRegular("Range: ${rangeValue.value.start}..${rangeValue.value.endInclusive}")
        }
    }
}

@Preview
@Composable
private fun BisqRangeSlider_CustomRangePreview() {
    BisqTheme.Preview {
        val rangeValue = remember { mutableStateOf(50f..150f) }
        Column(modifier = Modifier.padding(16.dp)) {
            BisqText.BaseRegular("Custom Range (6f..250f)")
            Spacer(modifier = Modifier.height(8.dp))
            BisqRangeSlider(
                value = rangeValue.value,
                onValueChange = { rangeValue.value = it },
                valueRange = 6f..250f,
            )
            Spacer(modifier = Modifier.height(4.dp))
            BisqText.SmallRegular("Range: ${rangeValue.value.start}..${rangeValue.value.endInclusive}")
        }
    }
}
