package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.CloseIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

enum class BisqChipType {
    Default,
    Outline,
}

enum class BisqChipSize {
    Default,
    Compact,
}

@Composable
fun BisqChip(
    modifier: Modifier = Modifier,
    label: String = "",
    showRemove: Boolean = true,
    selected: Boolean = false,
    onClick: ((String) -> Unit)? = null,
    onRemove: ((String) -> Unit)? = null,
    type: BisqChipType = BisqChipType.Default,
    size: BisqChipSize = BisqChipSize.Default,
) {
    val chipColors =
        if (type == BisqChipType.Outline) {
            SelectableChipColors(
                containerColor = Color.Transparent,
                labelColor = BisqTheme.colors.primary,
                leadingIconColor = BisqTheme.colors.primary,
                trailingIconColor = BisqTheme.colors.primary,
                selectedLabelColor = BisqTheme.colors.primary,
                selectedLeadingIconColor = BisqTheme.colors.primary,
                selectedTrailingIconColor = BisqTheme.colors.primary,
                selectedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                disabledLabelColor = BisqTheme.colors.primary.copy(alpha = 0.4f),
                disabledLeadingIconColor = BisqTheme.colors.primary.copy(alpha = 0.4f),
                disabledTrailingIconColor = BisqTheme.colors.primary.copy(alpha = 0.4f),
                disabledSelectedContainerColor = Color.Transparent,
            )
        } else {
            SelectableChipColors(
                containerColor = BisqTheme.colors.dark_grey40,
                labelColor = BisqTheme.colors.light_grey10,
                leadingIconColor = BisqTheme.colors.light_grey10,
                trailingIconColor = BisqTheme.colors.light_grey10,
                // Selected must be unmistakable: `secondary` (#2C2C2C) vs the unselected
                // `dark_grey40` (#2B2B2B) is imperceptible — use the green-tinted pair instead.
                selectedLabelColor = BisqTheme.colors.primary,
                selectedLeadingIconColor = BisqTheme.colors.primary,
                selectedTrailingIconColor = BisqTheme.colors.primary,
                selectedContainerColor = BisqTheme.colors.primary2,
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
        label = {
            when (size) {
                BisqChipSize.Default ->
                    BisqText.BaseLight(label, modifier = Modifier.padding(vertical = BisqUIConstants.ScreenPadding))
                BisqChipSize.Compact -> BisqText.SmallLight(label)
            }
        },
        selected = selected,
        // null, not a no-op lambda: InputChip reserves the trailing-icon slot width whenever the
        // param is non-null, so a conditional no-op still costs every chip dead trailing space.
        trailingIcon =
            if (showRemove) {
                {
                    IconButton(
                        onClick = { onRemove?.invoke(label) },
                    ) {
                        CloseIcon(modifier = Modifier.size(InputChipDefaults.AvatarSize))
                    }
                }
            } else {
                null
            },
        modifier = modifier,
        colors = chipColors,
        border =
            if (type == BisqChipType.Outline) {
                InputChipDefaults.inputChipBorder(
                    borderColor = BisqTheme.colors.primary,
                    selectedBorderColor = BisqTheme.colors.primary,
                    enabled = true,
                    selected = selected,
                )
            } else {
                // Transparent when unselected (not null) so selecting never changes the chip's size.
                InputChipDefaults.inputChipBorder(
                    borderColor = Color.Transparent,
                    selectedBorderColor = BisqTheme.colors.primary,
                    enabled = true,
                    selected = selected,
                )
            },
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
    )
}
