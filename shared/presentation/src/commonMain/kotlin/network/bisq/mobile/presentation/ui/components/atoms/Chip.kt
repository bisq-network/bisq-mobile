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
            containerColor = BisqTheme.colors.primary2,
            labelColor = BisqTheme.colors.light1,
            leadingIconColor =BisqTheme.colors.light1,
            trailingIconColor = BisqTheme.colors.light1,
            selectedLabelColor = BisqTheme.colors.light1,
            selectedLeadingIconColor = BisqTheme.colors.primary,
            selectedTrailingIconColor = BisqTheme.colors.primary,
            selectedContainerColor = BisqTheme.colors.secondary,

            disabledContainerColor =BisqTheme.colors.secondary,
            disabledLabelColor = BisqTheme.colors.light1,
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