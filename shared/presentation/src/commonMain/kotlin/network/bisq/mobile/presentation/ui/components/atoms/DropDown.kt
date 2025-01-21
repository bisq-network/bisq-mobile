package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import network.bisq.mobile.presentation.ui.components.atoms.icons.ArrowDownIcon
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BisqDropDown(
    label: String = "",
    items: List<String>,
    value: String,
    displayText: String? = null,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select an item",
    searchable: Boolean = false,
    chipMultiSelect: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptyList<String>()) }

    val filteredItems = if (searchable && searchText.isNotEmpty()) {
        items.filter { it.contains(searchText, ignoreCase = true) }
    } else {
        items
    }

    Column {
        if (label.isNotEmpty()) {
            BisqText.baseRegular(
                text = label,
                color = BisqTheme.colors.light2,
            )
        }

        BisqButton(
            onClick = { expanded = true },
            fullWidth = true,
            padding = PaddingValues(
                horizontal = BisqUIConstants.ScreenPadding,
                vertical = BisqUIConstants.ScreenPaddingHalf
            ),
            backgroundColor = BisqTheme.colors.secondary,
            text = displayText ?: value,
            textAlign = TextAlign.Start,
            rightIcon = { ArrowDownIcon() }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.wrapContentSize().background(color = BisqTheme.colors.secondary)
        ) {
            if (searchable) {
                BisqTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = "Search...",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            filteredItems.forEach { item ->
                DropdownMenuItem(
                    text = { BisqText.baseRegular(text = item) },
                    onClick = {
                        onValueChanged.invoke(item)
                        expanded = false
                        if (chipMultiSelect) {
                            val updatedList = selected.toMutableList()
                            if (!updatedList.contains(item)) { // Prevent duplicates
                                updatedList.add(item)
                            }
                            selected = updatedList
                        }
                    },
                )
            }
        }

        if (chipMultiSelect) {
            // TODO: Should do BisqChipRow
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)
            ) {
                selected.forEach { item ->
                    BisqChip(item, onRemove = {
                        val updatedList = selected.toMutableList()
                        updatedList.remove(item)
                        selected = updatedList
                    })
                }
            }
        }
    }
}
