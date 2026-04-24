package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

@Composable
fun BisqSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier.fillMaxWidth(),
    label: String = "",
    disabled: Boolean = false,
    onSwitch: ((Boolean) -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BisqText.BaseLight(
            label,
            modifier =
                Modifier
                    .padding(end = BisqUIConstants.ScreenPadding)
                    .weight(1f)
                    .clickable(
                        enabled = !disabled,
                        onClick = {
                            if (onSwitch != null) {
                                onSwitch(!checked)
                            }
                        },
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ),
        )

        Switch(
            checked = checked,
            onCheckedChange = onSwitch,
            enabled = !disabled,
            colors =
                SwitchColors(
                    checkedThumbColor = BisqTheme.colors.primaryDim,
                    checkedTrackColor = BisqTheme.colors.primary65,
                    checkedBorderColor = BisqTheme.colors.backgroundColor,
                    checkedIconColor = BisqTheme.colors.backgroundColor,
                    uncheckedThumbColor = BisqTheme.colors.white,
                    uncheckedTrackColor = BisqTheme.colors.white.copy(alpha = 0.45.toFloat()),
                    uncheckedBorderColor = BisqTheme.colors.backgroundColor,
                    uncheckedIconColor = BisqTheme.colors.backgroundColor,
                    disabledCheckedThumbColor = BisqTheme.colors.mid_grey30,
                    disabledCheckedTrackColor = BisqTheme.colors.secondary,
                    disabledCheckedBorderColor = BisqTheme.colors.backgroundColor,
                    disabledCheckedIconColor = BisqTheme.colors.backgroundColor,
                    disabledUncheckedThumbColor = BisqTheme.colors.mid_grey30,
                    disabledUncheckedTrackColor = BisqTheme.colors.secondary,
                    disabledUncheckedBorderColor = BisqTheme.colors.backgroundColor,
                    disabledUncheckedIconColor = BisqTheme.colors.backgroundColor,
                ),
        )
    }
}

@Preview
@Composable
private fun BisqSwitch_LongLabelUncheckedPreview() {
    BisqTheme.Preview {
        var checked by remember { mutableStateOf(false) }
        BisqSwitch(
            checked = checked,
            label =
                "This is a very long switch label to validate wrapping behavior and spacing when text becomes much longer than usual in the same row.",
            onSwitch = { checked = it },
        )
    }
}

@Preview
@Composable
private fun BisqSwitch_CheckedPreview() {
    BisqTheme.Preview {
        var checked by remember { mutableStateOf(true) }
        BisqSwitch(
            checked = checked,
            label =
                "This is a normal switch label toggle to validate",
            onSwitch = { checked = it },
        )
    }
}

@Preview
@Composable
private fun BisqSwitch_LongLabelDisabledPreview() {
    BisqTheme.Preview {
        BisqSwitch(
            checked = true,
            disabled = true,
            label =
                "This is a very long switch label in disabled state to validate readability and layout when interaction is turned off.",
            onSwitch = {},
        )
    }
}
