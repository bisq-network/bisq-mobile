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

@Composable
fun BisqChip(
    label: String = "",
    onClick: ((String) -> Unit)? = null,
    onRemove: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {

    InputChip(
        onClick = {
            onClick?.invoke(label)
        },
        label = { BisqText.baseRegular(label, modifier = Modifier.padding(top= 12.dp)) },
        selected = false,
        trailingIcon = {
            IconButton(
                onClick = { onRemove?.invoke(label) }
            ) {
                CloseIcon(modifier = Modifier.size(InputChipDefaults.AvatarSize))
            }
        },
        modifier = modifier,
        colors = SelectableChipColors( // TODO:
            containerColor = BisqTheme.colors.primaryDim,
            labelColor = BisqTheme.colors.light_grey10,
            leadingIconColor =BisqTheme.colors.light_grey10,
            trailingIconColor = BisqTheme.colors.light_grey10,
            selectedLabelColor = BisqTheme.colors.light_grey10,
            selectedLeadingIconColor = BisqTheme.colors.primary,
            selectedTrailingIconColor = BisqTheme.colors.primary,
            selectedContainerColor = BisqTheme.colors.secondary,

            disabledContainerColor =BisqTheme.colors.secondary,
            disabledLabelColor = BisqTheme.colors.light_grey10,
            disabledLeadingIconColor = BisqTheme.colors.primary,
            disabledTrailingIconColor =BisqTheme.colors.primary ,
            disabledSelectedContainerColor = BisqTheme.colors.secondary,
        ),
        border = InputChipDefaults.inputChipBorder(
            borderColor = BisqTheme.colors.primary,
            selectedBorderColor = BisqTheme.colors.primary,
            enabled = true,
            selected = true,
        )
    )

}