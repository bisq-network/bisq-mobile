package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.icons.CloseIcon
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

enum class BisqChipType {
    Default,
    Outline,
}

@Composable
fun BisqChip(
    label: String = "",
    showRemove: Boolean = true,
    onClick: ((String) -> Unit)? = null,
    onRemove: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    type: BisqChipType = BisqChipType.Default,
) {

    val chipColors = if (type == BisqChipType.Outline) {
        SelectableChipColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            labelColor = BisqTheme.colors.primary,
            leadingIconColor = BisqTheme.colors.primary,
            trailingIconColor = BisqTheme.colors.primary,
            selectedLabelColor = BisqTheme.colors.primary,
            selectedLeadingIconColor = BisqTheme.colors.primary,
            selectedTrailingIconColor = BisqTheme.colors.primary,
            selectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledLabelColor = BisqTheme.colors.primary.copy(alpha = 0.4f),
            disabledLeadingIconColor = BisqTheme.colors.primary.copy(alpha = 0.4f),
            disabledTrailingIconColor = BisqTheme.colors.primary.copy(alpha = 0.4f),
            disabledSelectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
        )
    } else {
        SelectableChipColors(
            containerColor = BisqTheme.colors.primaryDim,
            labelColor = BisqTheme.colors.light_grey10,
            leadingIconColor = BisqTheme.colors.light_grey10,
            trailingIconColor = BisqTheme.colors.light_grey10,
            selectedLabelColor = BisqTheme.colors.light_grey10,
            selectedLeadingIconColor = BisqTheme.colors.primary,
            selectedTrailingIconColor = BisqTheme.colors.primary,
            selectedContainerColor = BisqTheme.colors.secondary,
            disabledContainerColor = BisqTheme.colors.secondary,
            disabledLabelColor = BisqTheme.colors.light_grey10.copy(alpha = 0.4f),
            disabledLeadingIconColor = BisqTheme.colors.primary.copy(alpha = 0.4f),
            disabledTrailingIconColor = BisqTheme.colors.primary.copy(alpha = 0.4f),
            disabledSelectedContainerColor = BisqTheme.colors.secondary,
        )
    }

    InputChip(
        onClick = {
            onClick?.invoke(label)
        },
        label = { BisqText.baseRegular(label, modifier = Modifier.padding(vertical = BisqUIConstants.ScreenPadding)) },
        selected = false,
        trailingIcon = {
            if (showRemove) {
                IconButton(
                    onClick = { onRemove?.invoke(label) }
                ) {
                    CloseIcon(modifier = Modifier.size(InputChipDefaults.AvatarSize))
                }
            }
        },
        modifier = modifier,
        colors = chipColors,
        border = if (type == BisqChipType.Outline) {
            InputChipDefaults.inputChipBorder(
                borderColor = BisqTheme.colors.primary,
                selectedBorderColor = BisqTheme.colors.primary,
                enabled = true,
                selected = false,
            )
        } else {
            null
        }
    )
}
