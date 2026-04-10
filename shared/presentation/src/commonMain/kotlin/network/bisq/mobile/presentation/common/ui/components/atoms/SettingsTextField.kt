package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.runtime.Composable

@Composable
fun SettingsTextField(
    label: String,
    value: String,
    editable: Boolean = true,
    isTextArea: Boolean = false,
    onValueChange: (String) -> Unit = {},
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    BisqTextFieldV0(
        label = label,
        value = value,
        enabled = editable,
        singleLine = !isTextArea,
        minLines = if (isTextArea) 2 else 1,
        maxLines = if (isTextArea) Int.MAX_VALUE else 1,
        onValueChange = onValueChange,
        trailingIcon = trailingIcon,
    )
}
