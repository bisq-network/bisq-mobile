package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * A reusable composable that displays Save and Cancel action buttons for editable fields.
 * Typically used as a trailingIcon in text fields.
 *
 * @param onSave Callback invoked when the Save button is clicked
 * @param onCancel Callback invoked when the Cancel button is clicked (typically reverts changes)
 * @param modifier Modifier to be applied to the Row containing the buttons
 * @param disabled Whether the buttons are disabled (default: false)
 */
@Composable
fun EditableFieldActions(
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
) {
    Row(
        modifier = modifier,
    ) {
        BisqButton(
            modifier =
                Modifier
                    .padding(start = 4.dp)
                    .size(30.dp),
            disabled = disabled,
            iconOnly = {
                Icon(
                    Icons.Filled.Check,
                    "save",
                )
            },
            onClick = onSave,
        )

        BisqButton(
            type = BisqButtonType.Danger,
            modifier =
                Modifier
                    .padding(start = 10.dp)
                    .size(30.dp),
            disabled = disabled,
            iconOnly = {
                Icon(
                    Icons.Filled.Close,
                    "cancel",
                )
            },
            onClick = onCancel,
        )
    }
}

@Preview
@Composable
private fun EditableFieldActions_EnabledPreview() {
    BisqTheme.Preview {
        EditableFieldActions(
            onSave = {},
            onCancel = {},
        )
    }
}

@Preview
@Composable
private fun EditableFieldActions_DisabledPreview() {
    BisqTheme.Preview {
        EditableFieldActions(
            onSave = {},
            onCancel = {},
            disabled = true,
        )
    }
}
