package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ExpandAllIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.common.ui.utils.rememberBlurTriggerSetup
import network.bisq.mobile.presentation.common.ui.utils.setBlurTrigger
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> BisqMultiSelect(
    label: String = "",
    helpText: String = "",
    options: Iterable<T>,
    optionKey: ((T) -> String) = { it.toString() },
    optionLabel: ((T) -> String) = { it.toString() },
    selectedKeys: Set<String> = setOf(),
    onSelectionChanged: ((option: T, selected: Boolean) -> Unit),
    modifier: Modifier = Modifier,
    placeholder: String = "mobile.components.select.placeholder".i18n(),
    searchable: Boolean = false,
    maxSelectionLimit: Int = Int.MAX_VALUE,
    minSelectionLimit: Int = 0,
    chipType: BisqChipType = BisqChipType.Default,
    disabled: Boolean = false,
) {
    val coroutineScope = rememberCoroutineScope()
    var errorText by remember { mutableStateOf<String?>(null) }
    var errorDismissJob by remember { mutableStateOf<Job?>(null) }
    val menuItemColors = remember {
        MenuItemColors(
            textColor = BisqTheme.colors.white,
            trailingIconColor = BisqTheme.colors.primary,
            disabledTextColor = BisqTheme.colors.mid_grey20,
            disabledTrailingIconColor = BisqTheme.colors.mid_grey20,
            leadingIconColor = BisqTheme.colors.white,
            disabledLeadingIconColor = BisqTheme.colors.mid_grey20
        )
    }

    BisqSelect(
        label = label,
        helpText = helpText,
        errorText = errorText,
        options = options,
        optionKey = optionKey,
        optionLabel = optionLabel,
        selectedKey = selectedKeys.firstOrNull(),
        modifier = modifier,
        placeholder = placeholder,
        searchable = searchable,
        disabled = disabled,
        onTriggerClicked = {
            val limitReached = selectedKeys.size >= maxSelectionLimit
            if (limitReached) {
                errorText =
                    "mobile.components.dropdown.maxSelection".i18n(maxSelectionLimit.toString()) // "Maximum of {0} items can be selected"

                errorDismissJob?.cancel()
                errorDismissJob = coroutineScope.launch {
                    delay(3000)
                    errorText = null
                }

                false
            } else {
                true
            }
        },
        itemContent = { keyOptionLabelEntry, onDismiss ->
            val isSelected = selectedKeys.contains(keyOptionLabelEntry.key)
            DropdownMenuItem(
                text = { BisqText.local(keyOptionLabelEntry.value.second, maxLines = 1) },
                onClick = {
                    errorText = null
                    if (selectedKeys.size >= maxSelectionLimit && !isSelected) return@DropdownMenuItem
                    if (selectedKeys.size - 1 < minSelectionLimit && isSelected) return@DropdownMenuItem
                    onSelectionChanged(
                        keyOptionLabelEntry.value.first,
                        !isSelected, // simply toggling
                    )
                    // we don't close dropdown after max selection is reached,
                    // because it would be better UX this way, and we disable selection of other items
                    // this is to allow user to quickly decide between their choices
                    // vs closing the selection and making them delete chips first then open again
                },
                trailingIcon = {
                    SegmentedButtonDefaults.Icon(isSelected)
                },
                colors = menuItemColors,
                enabled = isSelected || (selectedKeys.size < maxSelectionLimit)
            )
        },
        extraContent = { keyLabelOptionEntries ->
            BisqGap.VHalf()
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)
            ) {
                keyLabelOptionEntries.entries.filter { it.key in selectedKeys }.forEach {
                    val optionLabelPair = it.value
                    key(it.key) { // key is for proper detection of changes & animations
                        BisqChip(
                            label = optionLabelPair.second,
                            showRemove = selectedKeys.size > minSelectionLimit,
                            type = chipType,
                            onRemove = {
                                errorText = null
                                onSelectionChanged(optionLabelPair.first, false)
                            }
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> BisqSelect(
    label: String = "",
    helpText: String = "",
    errorText: String? = null,
    options: Iterable<T>,
    optionKey: ((T) -> String) = { it.hashCode().toString() },
    optionLabel: ((T) -> String) = { it.toString() },
    selectedKey: String?,
    onSelected: (T) -> Unit = {},
    modifier: Modifier = Modifier,
    placeholder: String = "mobile.components.select.placeholder".i18n(),
    searchable: Boolean = false,
    disabled: Boolean = false,
    onTriggerClicked: () -> Boolean? = { null },
    itemContent: @Composable (Map.Entry<String, Pair<T, String>>, () -> Unit) -> Unit = { option, onDismiss ->
        DropdownMenuItem(
            text = { BisqText.baseLight(option.value.second, singleLine = true) },
            onClick = {
                onSelected(option.value.first)
                onDismiss()
            },
        )
    },
    extraContent: @Composable ColumnScope.(entries: Map<String, Pair<T, String>>) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val onDismiss = remember {
        {
            expanded = false
            searchText = ""
        }
    }

    val keyLabelOptionEntries = remember(
        options,
        optionKey,
        optionLabel,
    ) { options.associate { optionKey(it) to Pair(it, optionLabel(it)) } }

    val filteredOptions = remember(searchable, searchText, keyLabelOptionEntries) {
        if (searchable && searchText.isNotEmpty()) {
            keyLabelOptionEntries.filter {
                it.value.second.contains(
                    searchText,
                    ignoreCase = true
                )
            }
        } else {
            keyLabelOptionEntries
        }
    }

    val selectedLabel = remember(optionKey, selectedKey, keyLabelOptionEntries) {
        keyLabelOptionEntries.filter { it.key == selectedKey }
            .let { if (it.isNotEmpty()) it.values.first().second else "" }
    }

    val blurTriggerSetup = rememberBlurTriggerSetup()

    Column(modifier = modifier) {
        BisqTextField(
            label = label,
            readOnly = true,
            value = selectedLabel,
            placeholder = placeholder,
            helperText = helpText,
            rightSuffix = {
                ExpandAllIcon()
            },
            validation = { errorText },
            disabled = disabled,
            modifier = Modifier.setBlurTrigger(blurTriggerSetup),
            onFocus = {
                val newExpand = onTriggerClicked() ?: true
                expanded = newExpand
                if (!newExpand) {
                    blurTriggerSetup.triggerBlur()
                }
            }
        )
        extraContent(keyLabelOptionEntries)
    }

    if (expanded) {
        BisqBottomSheet(
            onDismissRequest = {
                expanded = false
                searchText = ""
            },
        ) {
            if (searchable) {
                BisqTextField(
                    value = searchText,
                    onValueChange = { it, _ -> searchText = it },
                    placeholder = "mobile.components.dropdown.searchPlaceholder".i18n(),
                    modifier = Modifier.fillMaxWidth()
                        .padding(BisqUIConstants.ScreenPaddingHalfQuarter)
                )
            }

            LazyColumn {
                items(filteredOptions.entries.toList(), key = { it.key }) { item ->
                    itemContent(item, onDismiss)
                }
            }

            if (searchable && filteredOptions.isEmpty()) {
                Surface(
                    color = BisqTheme.colors.dark_grey40,
                    shape = RoundedCornerShape(BisqUIConstants.ScreenPadding),
                    modifier = Modifier.padding(BisqUIConstants.ScreenPadding).fillMaxWidth(),
                ) {
                    Box(
                        Modifier.fillMaxWidth().height(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BisqText.largeRegular("mobile.components.select.empty".i18n())
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun BisqSelectPreview() {
    BisqTheme.Preview {
        BisqSelect(
            label = "settings.language.headline".i18n(),
            options = listOf("English", "Spanish"),
            optionKey = { it },
            optionLabel = { it },
            selectedKey = "English",
            helpText = "random help text"
        )
    }
}
@Preview
@Composable
private fun BisqSelectPlaceholderPreview() {
    BisqTheme.Preview {
        BisqSelect(
            label = "settings.language.headline".i18n(),
            options = listOf("English", "Spanish"),
            placeholder = "English",
            optionKey = { it },
            optionLabel = { it },
            selectedKey = "",
            helpText = "random help text"
        )
    }
}

@Preview
@Composable
private fun BisqSelectErrorPreview() {
    BisqTheme.Preview {
        BisqSelect(
            label = "settings.language.headline".i18n(),
            options = listOf("English", "Spanish"),
            optionKey = { it },
            optionLabel = { it },
            selectedKey = "English",
            helpText = "random help text",
            errorText = "random error text"
        )
    }
}

