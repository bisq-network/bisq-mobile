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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BisqSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val thumbColor = if (enabled) BisqTheme.colors.primary else BisqTheme.colors.mid_grey20

    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        valueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished,
        enabled = enabled,
        thumb = {
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
        },
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
private fun BisqSlider_DefaultPreview() {
    BisqTheme.Preview {
        val value = remember { mutableFloatStateOf(0.5f) }
        Column(modifier = Modifier.padding(16.dp)) {
            BisqText.BaseRegular("Default (0f..1f)")
            Spacer(modifier = Modifier.height(8.dp))
            BisqSlider(
                value = value.floatValue,
                onValueChange = { value.floatValue = it },
            )
            Spacer(modifier = Modifier.height(4.dp))
            BisqText.SmallRegular("Value: ${value.floatValue}")
        }
    }
}

@Preview
@Composable
private fun BisqSlider_MinValuePreview() {
    BisqTheme.Preview {
        val value = remember { mutableFloatStateOf(0f) }
        Column(modifier = Modifier.padding(16.dp)) {
            BisqText.BaseRegular("Min Value")
            Spacer(modifier = Modifier.height(8.dp))
            BisqSlider(
                value = value.floatValue,
                onValueChange = { value.floatValue = it },
            )
            Spacer(modifier = Modifier.height(4.dp))
            BisqText.SmallRegular("Value: ${value.floatValue}")
        }
    }
}

@Preview
@Composable
private fun BisqSlider_MaxValuePreview() {
    BisqTheme.Preview {
        val value = remember { mutableFloatStateOf(1f) }
        Column(modifier = Modifier.padding(16.dp)) {
            BisqText.BaseRegular("Max Value")
            Spacer(modifier = Modifier.height(8.dp))
            BisqSlider(
                value = value.floatValue,
                onValueChange = { value.floatValue = it },
            )
            Spacer(modifier = Modifier.height(4.dp))
            BisqText.SmallRegular("Value: ${value.floatValue}")
        }
    }
}

@Preview
@Composable
private fun BisqSlider_DisabledPreview() {
    BisqTheme.Preview {
        val value = remember { mutableFloatStateOf(0.5f) }
        Column(modifier = Modifier.padding(16.dp)) {
            BisqText.BaseRegular("Disabled (enabled=false)")
            Spacer(modifier = Modifier.height(8.dp))
            BisqSlider(
                value = value.floatValue,
                onValueChange = { value.floatValue = it },
                enabled = false,
            )
            Spacer(modifier = Modifier.height(4.dp))
            BisqText.SmallRegular("Value: ${value.floatValue}")
        }
    }
}

@Preview
@Composable
private fun BisqSlider_CustomRangePreview() {
    BisqTheme.Preview {
        val value = remember { mutableFloatStateOf(125f) }
        Column(modifier = Modifier.padding(16.dp)) {
            BisqText.BaseRegular("Custom Range (6f..250f)")
            Spacer(modifier = Modifier.height(8.dp))
            BisqSlider(
                value = value.floatValue,
                onValueChange = { value.floatValue = it },
                valueRange = 6f..250f,
            )
            Spacer(modifier = Modifier.height(4.dp))
            BisqText.SmallRegular("Value: ${value.floatValue}")
        }
    }
}
