package network.bisq.mobile.presentation.ui.components.molecules.inputfield

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.icons.SearchIcon

@Composable
fun BisqSearchField(
    label: String = "",
    value: String,
    onValueChanged: (String, Boolean) -> Unit,
    placeholder: String = "",
    rightSuffix: (@Composable () -> Unit)? = null,
    disabled: Boolean = false,
    modifier: Modifier = Modifier) {

    BisqTextField(
        label = label,
        value = value,
        onValueChange = onValueChanged,
        placeholder = placeholder,
        leftSuffix = { SearchIcon() },
        rightSuffix = rightSuffix,
        isSearch= true,
        disabled = disabled,
        modifier = modifier,
    )

}
