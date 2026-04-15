package network.bisq.mobile.presentation.common.ui.components.molecules.inputfield

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.CloseIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.SearchIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.SortIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap.BisqGapHFill
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

@Composable
fun BisqSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    label: String = "",
    placeholder: String = "action.search".i18n(),
    rightSuffix: (@Composable () -> Unit)? = null,
    disabled: Boolean = false,
) {
    // Custom layout: text field is measured first and dictates the height.
    // The overlay row is then constrained to that exact height so it never
    // inflates the wrapper
    Layout(
        modifier = modifier,
        content = {
            BisqTextFieldV0(
                label = label,
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholder,
                leadingIcon = { SearchIcon() },
                trailingIcon = {
                    Spacer(
                        Modifier
                            .width(if (rightSuffix == null) 50.dp else 90.dp)
                            .padding(end = BisqUIConstants.ScreenPaddingHalf, bottom = 2.dp),
                    )
                },
                enabled = !disabled,
                singleLine = true,
                modifier = fieldModifier,
            )

            Row(
                modifier =
                    Modifier
                        .width(if (rightSuffix == null) 50.dp else 90.dp)
                        .padding(end = BisqUIConstants.ScreenPaddingHalf, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (value.isNotEmpty()) {
                    BisqButton(
                        iconOnly = {
                            CloseIcon(color = BisqTheme.colors.mid_grey20)
                        },
                        onClick = {
                            if (!disabled) {
                                onValueChange("")
                            }
                        },
                        type = BisqButtonType.Clear,
                        modifier = Modifier.weight(1f),
                    )
                } else if (rightSuffix != null) {
                    // when we don't have a clear button, we still want to
                    // have a spacer to fill in it's place, to push the
                    // right suffix which is a button usually in our case
                    // to the end of the row to look better and
                    // also prevent it from moving when our clear button is added
                    BisqGapHFill()
                }

                if (rightSuffix != null) {
                    rightSuffix()
                }
            }
        },
    ) { measurables, constraints ->
        val textField = measurables[0].measure(constraints)
        // Pin the overlay to the text field's height so it never expands the wrapper
        val overlay =
            measurables[1].measure(
                constraints.copy(minHeight = textField.height, maxHeight = textField.height),
            )
        layout(textField.width, textField.height) {
            textField.placeRelative(0, 0)
            // Simply Alignment.CenterEnd the overlay:
            overlay.placeRelative(x = textField.width - overlay.width, y = 0)
        }
    }
}

@Preview
@Composable
private fun BisqSearchField_EmptyPreview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("") }
            BisqSearchField(
                value = textState,
                onValueChange = { textState = it },
            )
        }
    }
}

@Preview
@Composable
private fun BisqSearchField_WithValuePreview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("bitcoin") }
            BisqSearchField(
                value = textState,
                onValueChange = { textState = it },
            )
        }
    }
}

@Preview
@Composable
private fun BisqSearchField_WithFilterButtonPreview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("") }
            BisqSearchField(
                value = textState,
                onValueChange = { textState = it },
                rightSuffix = {
                    BisqButton(
                        iconOnly = { SortIcon() },
                        onClick = {},
                        type = BisqButtonType.Clear,
                        modifier = Modifier.weight(1f),
                    )
                },
            )
        }
    }
}

@Preview
@Composable
private fun BisqSearchField_WithClearAndFilterButtonPreview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(12.dp)) {
            var textState by remember { mutableStateOf("bitcoin") }
            BisqSearchField(
                value = textState,
                onValueChange = { textState = it },
                rightSuffix = {
                    BisqButton(
                        iconOnly = { SortIcon() },
                        onClick = {},
                        type = BisqButtonType.Clear,
                        modifier = Modifier.weight(1f),
                    )
                },
            )
        }
    }
}
