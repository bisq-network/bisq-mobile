package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                checkedThumbColor = BisqTheme.colors.primary,
                checkedTrackColor = BisqTheme.colors.primaryDisabled,
                checkedBorderColor = BisqTheme.colors.backgroundColor,
                checkedIconColor =  BisqTheme.colors.backgroundColor,

                uncheckedThumbColor = BisqTheme.colors.grey1,
                uncheckedTrackColor = BisqTheme.colors.secondary,
                uncheckedBorderColor = BisqTheme.colors.backgroundColor,
                uncheckedIconColor = BisqTheme.colors.backgroundColor,

                disabledCheckedThumbColor = BisqTheme.colors.grey4,
                disabledCheckedTrackColor = BisqTheme.colors.secondary,
                disabledCheckedBorderColor = BisqTheme.colors.backgroundColor,
                disabledCheckedIconColor = BisqTheme.colors.backgroundColor,

                disabledUncheckedThumbColor =BisqTheme.colors.grey4,
                disabledUncheckedTrackColor = BisqTheme.colors.secondary,
                disabledUncheckedBorderColor =BisqTheme.colors.backgroundColor ,
                disabledUncheckedIconColor =BisqTheme.colors.backgroundColor,
            )
        )
    }
}
