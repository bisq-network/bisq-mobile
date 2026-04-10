package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import network.bisq.mobile.presentation.common.ui.components.atoms.button.PasswordIconButton
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme

@Composable
fun BisqPasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    color: Color = BisqTheme.colors.light_grey20,
    colors: BisqTextFieldColors = BisqTextFieldColors.default(),
    textStyle: TextStyle = BisqTextFieldDefaults.textStyle(color),
    label: String? = null,
    placeholder: String? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    bottomMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
) {
    var obscurePassword by remember { mutableStateOf(true) }

    BisqTextFieldV0(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        color = color,
        colors = colors,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        trailingIcon =
            if (enabled) {
                { PasswordIconButton(onObscurePassword = { obscurePassword = it }) }
            } else {
                null
            },
        prefix = prefix,
        suffix = suffix,
        isError = isError,
        bottomMessage = bottomMessage,
        visualTransformation = if (obscurePassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        interactionSource = interactionSource,
    )
}

private object BisqTextFieldDefaults {
    @Composable
    fun textStyle(color: Color): TextStyle = BisqTheme.typography.baseLight.copy(color = color)
}
