package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.icons.ArrowDownIcon
import network.bisq.mobile.presentation.ui.components.molecules.BisqDialog
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun BisqEditableDropDown(
    value: String,
    onValueChanged: (String) -> Unit,
    items: List<String>,
    label: String
) {
    var showDialog by remember { mutableStateOf(false) }

    BisqTextField(
        value = value,
        onValueChange = { it, _ -> onValueChanged(it) },
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
                                    onValueChanged(item)
                                    showDialog = false
                                }
                        ) {
                            BisqText.baseBold(text = item)
                        }
                    }
                }
            }
        }
    }
}
