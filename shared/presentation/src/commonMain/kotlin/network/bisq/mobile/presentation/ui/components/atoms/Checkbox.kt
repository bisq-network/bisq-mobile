package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun BisqCheckbox(
    label: String = "",
    disabled: Boolean = false,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val grey2Color = BisqTheme.colors.mid_grey20
    val whiteColor = BisqTheme.colors.white
    val finalLabelColor by remember(disabled) {
        mutableStateOf(
            if (disabled) {
                grey2Color
            } else {
                whiteColor
            }
        )
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = !disabled,
            colors = CheckboxColors(
                uncheckedBoxColor = BisqTheme.colors.secondary,
                uncheckedBorderColor = BisqTheme.colors.backgroundColor,
                uncheckedCheckmarkColor = BisqTheme.colors.secondary,

                checkedBoxColor = BisqTheme.colors.secondary,
                checkedBorderColor = BisqTheme.colors.primaryDim,
                checkedCheckmarkColor = BisqTheme.colors.primary,

                disabledBorderColor = BisqTheme.colors.backgroundColor,
                disabledUncheckedBorderColor = BisqTheme.colors.backgroundColor,
                disabledIndeterminateBorderColor = BisqTheme.colors.backgroundColor,

                disabledCheckedBoxColor = BisqTheme.colors.secondary,
                disabledUncheckedBoxColor = BisqTheme.colors.secondary,
                disabledIndeterminateBoxColor = BisqTheme.colors.secondary,
            )
        )
        BisqText.baseRegular(
            label,
            color = finalLabelColor,
            modifier = Modifier
                .clickable(
                    enabled = !disabled,
                    onClick = {
                        if (onCheckedChange != null) {
                            onCheckedChange(!checked)
                        }
                    },
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                )
        )
    }
}