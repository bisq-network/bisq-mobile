package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import network.bisq.mobile.presentation.ui.components.atoms.icons.ArrowDownIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

// TODO: Should do Multi-select dropdown separately?
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BisqDropDown(
    label: String = "",
    items: List<Pair<String, String>>,
    value: String,
    values: Set<String>? = null,
    onValueChanged: ((Pair<String, String>) -> Unit)? = null,
    onSetChanged: ((Set<Pair<String, String>>) -> Unit)? = null,
    modifier: Modifier = Modifier,
    placeholder: String = "Select an item",
    searchable: Boolean = false,
    showKey: Boolean = false,
    chipMultiSelect: Boolean = false,
    chipShowOnlyKey: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var selected by remember(items, values) {
        mutableStateOf(items.filter { values?.contains(it.first) == true }.toSet())
    }

    val filteredItems = if (searchable && searchText.isNotEmpty()) {
        items.filter { it.second.contains(searchText, ignoreCase = true) }
    } else {
        items
    }

    Column {
        if (label.isNotEmpty()) {
            BisqText.baseLight(label)
            BisqGap.VQuarter()
        }

        BisqButton(
            onClick = { expanded = true },
            fullWidth = true,
            padding = PaddingValues(
                horizontal = BisqUIConstants.ScreenPadding,
                vertical = BisqUIConstants.ScreenPaddingHalf
            ),
            backgroundColor = BisqTheme.colors.secondary,
            text = if (showKey) {
                items.find { it.first == value }?.first
            } else {
                items.find { it.first == value }?.second
            },
            textAlign = TextAlign.Start,
            rightIcon = { ArrowDownIcon() }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = BisqTheme.colors.dark_grey40,
            modifier = Modifier.wrapContentSize()
        ) {
            if (searchable) {
                BisqTextField(
                    value = searchText,
                    onValueChange = { it, isValid -> searchText = it },
                    placeholder = "Search...",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            filteredItems.forEachIndexed { index, item ->
                val textToShow = if (showKey) {
                    item.first
                } else {
                    item.second
                }
                DropdownMenuItem(
                    text = { BisqText.baseRegular(textToShow) },
                    onClick = {
                        onValueChanged?.invoke(item)
                        expanded = false
                        if (chipMultiSelect) {
                            selected = selected + item
                        }
                        onSetChanged?.invoke(selected)
                        searchText = ""
                    },
                )
            }
        }

        if (chipMultiSelect) {
            BisqGap.VQuarter()
            // TODO: Should do BisqChipRow
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)
            ) {
                selected.forEach { pair ->
                    BisqChip(
                        if (chipShowOnlyKey) pair.first else pair.second,
                        onRemove = {
                            selected = selected - pair
                            onSetChanged?.invoke(selected)
                            searchText = ""
                        })
                }
            }
        }
    }
}
