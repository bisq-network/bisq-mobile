package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import network.bisq.mobile.components.MaterialTextField
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun BisqTextField(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    placeholder: String?,
    labelRightSuffix: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row (
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BisqText.baseRegular(
                text = label,
                color = BisqTheme.colors.light2,
                )
            if (labelRightSuffix != null) {
                labelRightSuffix()
            }
        }
        MaterialTextField(
            text = value,
            placeholder = placeholder ?: "",
            onValueChanged = { onValueChanged(it) })
    }
}