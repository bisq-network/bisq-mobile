package network.bisq.mobile.presentation.ui.components.molecules.inputfield

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.icons.CloseIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.SearchIcon
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun BisqSearchField(
    label: String = "",
    value: String,
    onValueChanged: (String, Boolean) -> Unit,
    placeholder: String = "action.search".i18n(),
    rightSuffix: (@Composable () -> Unit)? = null,
    disabled: Boolean = false,
    modifier: Modifier = Modifier
) {

    BisqTextField(
        label = label,
        value = value,
        onValueChange = onValueChanged,
        placeholder = placeholder,
        leftSuffix = { SearchIcon() },
        rightSuffixModifier = if (rightSuffix == null)
            Modifier.width(50.dp)
        else
            Modifier.width(80.dp),
        rightSuffix = {
            if (rightSuffix != null) {
                Row {
                    BisqButton(
                        iconOnly = {
                            CloseIcon(color = BisqTheme.colors.mid_grey20)
                        },
                        onClick = { onValueChanged("", true) },
                        type = BisqButtonType.Clear
                    )
                    rightSuffix()
                }
            } else {

                BisqButton(
                    iconOnly = {
                        CloseIcon(color = BisqTheme.colors.mid_grey20)
                    },
                    onClick = { onValueChanged("", true) },
                    type = BisqButtonType.Clear
                )
            }
        },
        isSearch = true,
        disabled = disabled,
        modifier = modifier,
    )

}
