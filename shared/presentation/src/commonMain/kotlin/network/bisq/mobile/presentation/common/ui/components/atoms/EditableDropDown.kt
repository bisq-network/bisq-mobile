package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ArrowDownIcon
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.BisqDialog

@Composable
fun BisqEditableDropDown(
    value: String,
    onValueChange: (String, Boolean) -> Unit,
    items: List<String>,
    label: String,
    validation: ((String) -> String?)? = null,
) {
    var showDialog by remember { mutableStateOf(false) }

    BisqTextField(
        value = value,
        onValueChange = { it, valid -> onValueChange(it, valid) },
        label = label,
        rightSuffix = {
            Box(
                modifier =
                    Modifier
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                        .clickable(
                            onClick = { showDialog = true },
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ),
            ) {
                ArrowDownIcon()
            }
        },
        validation = validation,
    )

    if (showDialog) {
        BisqDialog(onDismissRequest = { showDialog = false }) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp, max = 300.dp),
            ) {
                LazyColumn {
                    items(items) { item ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable {
                                        onValueChange(item, true)
                                        showDialog = false
                                    },
                        ) {
                            BisqText.BaseBold(item)
                        }
                    }
                }
            }
        }
    }
}
