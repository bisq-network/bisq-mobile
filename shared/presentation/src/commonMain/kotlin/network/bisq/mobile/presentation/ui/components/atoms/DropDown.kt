package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.icons.ArrowDownIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BisqDropDown(
    label: String = "",
    helpText: String = "",
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
    maxSelectionLimit: Int? = null,
    outlineChip: Boolean = false,
) {
    val showError = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var errorDismissJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
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

    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            BisqText.baseLight(label)
            BisqGap.VQuarter()
        }

        BisqButton(
            onClick = {
                val limitReached = chipMultiSelect && maxSelectionLimit != null && selected.size >= maxSelectionLimit
                if (limitReached) {
                    showError.value = true

                    errorDismissJob?.cancel()
                    errorDismissJob = coroutineScope.launch {
                        delay(3000)
                        showError.value = false
                    }

                    expanded = false
                } else {
                    expanded = true
                }
            },
            fullWidth = true,
            padding = PaddingValues(
                horizontal = BisqUIConstants.ScreenPadding,
                vertical = BisqUIConstants.ScreenPaddingHalf
            ),
            backgroundColor = BisqTheme.colors.secondary,
            text = if (showKey) {
                items.find { it.first == value }?.first ?: placeholder
            } else {
                items.find { it.first == value }?.second ?: placeholder
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
                    onValueChange = { it, _ -> searchText = it },
                    placeholder = "mobile.components.dropdown.searchPlaceholder".i18n(),
                    modifier = Modifier.fillMaxWidth().padding(BisqUIConstants.ScreenPaddingHalfQuarter)
                )
            }

            filteredItems.forEach { item ->
                val textToShow = if (showKey) item.first else item.second

                DropdownMenuItem(
                    text = { BisqText.baseRegular(textToShow) },
                    onClick = {
                        showError.value = false
                        if (chipMultiSelect) {
                            val isAlreadySelected = selected.contains(item)

                            if (!isAlreadySelected) {
                                selected = selected + item
                            }

                            onSetChanged?.invoke(selected)
                            searchText = ""
                            expanded = false
                        } else {
                            onValueChanged?.invoke(item)
                            expanded = false
                            searchText = ""
                        }
                    },
                )
            }
        }

        BisqGap.VHalf()

        if(showError.value) {
            BisqText.smallRegular(
                text = "mobile.components.dropdown.maxSelection".i18n(maxSelectionLimit.toString()), // "Maximum of {0} items can be selected",
                modifier = Modifier.padding(
                    start = BisqUIConstants.ScreenPaddingQuarter,
                    top = BisqUIConstants.ScreenPadding1,
                    bottom = BisqUIConstants.ScreenPaddingQuarter
                ),
                color = BisqTheme.colors.danger
            )
        } else {
            BisqText.smallRegular(
                text = helpText,
                modifier = Modifier.padding(
                    start = BisqUIConstants.ScreenPaddingQuarter,
                    top = BisqUIConstants.ScreenPadding1,
                    bottom = BisqUIConstants.ScreenPaddingQuarter
                ),
            )
        }

        if (chipMultiSelect) {
            BisqGap.VHalf()
            // TODO: Should do BisqChipRow
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)
            ) {
                selected.forEach { pair ->
                    BisqChip(
                        label = if (chipShowOnlyKey) pair.first else pair.second,
                        showRemove = selected.size != 1,
                        type = BisqChipType.Outline,
                        onRemove = {
                            showError.value = false
                            selected = selected - pair
                            onSetChanged?.invoke(selected)
                            searchText = ""
                        }
                    )
                }
            }
        }
    }
}
