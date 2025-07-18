package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.runtime.Composable

@Composable
fun SettingsTextField(
    label: String,
    value: String,
    editable: Boolean = true,
    isTextArea: Boolean = false,
    onValueChange: ((String, Boolean) -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    BisqTextField(
        label,
        value = value,
        disabled = !editable,
        isTextArea = isTextArea,
        onValueChange = { newValue, isValid ->
            if (onValueChange != null) {
                onValueChange(newValue, isValid)
            }
        },
        rightSuffix = trailingIcon
    )
}