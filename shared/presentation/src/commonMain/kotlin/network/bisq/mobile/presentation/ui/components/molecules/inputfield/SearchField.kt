package network.bisq.mobile.presentation.ui.components.molecules.inputfield

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
            Row(horizontalArrangement = Arrangement.End) {
                if (value.isNotEmpty()) {
                    BisqButton(
                        iconOnly = {
                            CloseIcon(color = BisqTheme.colors.mid_grey20)
                        },
                        onClick = { onValueChanged("", true) },
                        type = BisqButtonType.Clear,
                        modifier = Modifier.weight(1f),
                    )
                } else if (rightSuffix != null) {
                    // when we don't have a clear button, we still want to
                    // have a spacer to fill in it's place, to push the
                    // right suffix which is a button usually in our case
                    // to the end of the row to look better and
                    // also prevent it from moving when our clear button is added
                    Spacer(Modifier.weight(1f))
                }

                if (rightSuffix != null) {
                    rightSuffix()
                }
            }
        },
        isSearch = true,
        disabled = disabled,
        modifier = modifier,
    )

}
