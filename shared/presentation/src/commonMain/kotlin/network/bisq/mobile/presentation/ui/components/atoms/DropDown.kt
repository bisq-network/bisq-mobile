package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun BisqDropDown(
    label: String = "",
    items: List<String>,
    value: String,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select an item"
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        if (label.isNotEmpty()) {
            BisqText.baseRegular(
                text = label,
                color = BisqTheme.colors.light2,
            )
        }

        BisqButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = BisqTheme.colors.grey5,
            textComponent = {
                BisqText.baseRegular(
                    text = value,
                    color = BisqTheme.colors.light1,
                    textAlign = TextAlign.Start
                )
            },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.wrapContentSize().background(color = BisqTheme.colors.secondary)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { BisqText.baseRegular(text = item) },
                    onClick = {
                        onValueChanged.invoke(item)
                        expanded = false
                    },
                )
            }
        }
    }
}
