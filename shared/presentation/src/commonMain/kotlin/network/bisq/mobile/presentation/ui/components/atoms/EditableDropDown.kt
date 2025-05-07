package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.icons.ArrowDownIcon
import network.bisq.mobile.presentation.ui.components.molecules.dialog.BisqDialog

@Composable
fun BisqEditableDropDown(
    value: String,
    onValueChanged: (String, Boolean) -> Unit,
    items: List<String>,
    label: String,
    validation: ((String) -> String?)? = null,
) {
    var showDialog by remember { mutableStateOf(false) }

    BisqTextField(
        value = value,
        onValueChange = { it, valid -> onValueChanged(it, valid) },
        label = label,
        rightSuffix = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .clickable(
                        onClick = { showDialog = true },
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    )
            ) {
                ArrowDownIcon()
            }
        },
        validation = validation,
    )

    if (showDialog) {
        BisqDialog(onDismissRequest = { showDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp, max = 300.dp)
            ) {
                LazyColumn {
                    items(items) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable {
                                    onValueChanged(item, true)
                                    showDialog = false
                                }
                        ) {
                            BisqText.baseBold(item)
                        }
                    }
                }
            }
        }
    }
}
