package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun BisqSwitch(
    label: String = "",
    disabled: Boolean = false,
    checked: Boolean,
    onSwitch: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BisqText.baseRegular(
            label,
            modifier = Modifier
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
                )
        )

        Switch(
            checked = checked,
            onCheckedChange = onSwitch,
            enabled = !disabled,
            colors = SwitchColors(
                checkedThumbColor = BisqTheme.colors.primaryDim,
                checkedTrackColor = BisqTheme.colors.primary65,
                checkedBorderColor = BisqTheme.colors.backgroundColor,
                checkedIconColor =  BisqTheme.colors.backgroundColor,

                uncheckedThumbColor = BisqTheme.colors.white,
                uncheckedTrackColor = BisqTheme.colors.white.copy(alpha = 0.45.toFloat()),
                uncheckedBorderColor = BisqTheme.colors.backgroundColor,
                uncheckedIconColor = BisqTheme.colors.backgroundColor,

                disabledCheckedThumbColor = BisqTheme.colors.mid_grey30,
                disabledCheckedTrackColor = BisqTheme.colors.secondary,
                disabledCheckedBorderColor = BisqTheme.colors.backgroundColor,
                disabledCheckedIconColor = BisqTheme.colors.backgroundColor,

                disabledUncheckedThumbColor =BisqTheme.colors.mid_grey30,
                disabledUncheckedTrackColor = BisqTheme.colors.secondary,
                disabledUncheckedBorderColor =BisqTheme.colors.backgroundColor ,
                disabledUncheckedIconColor =BisqTheme.colors.backgroundColor,
            )
        )
    }
}
