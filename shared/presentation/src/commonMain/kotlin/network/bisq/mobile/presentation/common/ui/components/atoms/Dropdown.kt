package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ArrowDownIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BisqDropdown(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    prompt: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedValue = options.getOrNull(selectedIndex) ?: prompt.orEmpty()

    Column(modifier = modifier) {
        if (!label.isNullOrBlank()) {
            BisqText.BaseLight(
                text = label,
                modifier = Modifier.padding(4.dp),
            )
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            topStart = BisqUIConstants.BorderRadius,
                            topEnd = BisqUIConstants.BorderRadius,
                        ),
                    ),
        ) {
            BisqTextFieldV0(
                value = selectedValue,
                onValueChange = {},
                readOnly = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                trailingIcon = {
                    ArrowDownIcon()
                },
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier =
                    Modifier
                        .background(BisqTheme.colors.dark_grey40),
            ) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { BisqText.BaseRegular(option) },
                        onClick = {
                            onOptionSelect(index)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BisqDropdownSearchable(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    prompt: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val selectedValue = options.getOrNull(selectedIndex) ?: prompt.orEmpty()
    val textFieldValue = if (expanded) searchQuery else selectedValue
    val filteredOptions =
        remember(options, searchQuery) {
            options
                .mapIndexed { index, option -> index to option }
                .filter { (_, option) -> option.contains(searchQuery, ignoreCase = true) }
        }

    Column(modifier = modifier) {
        if (!label.isNullOrBlank()) {
            BisqText.BaseLight(
                text = label,
                modifier = Modifier.padding(4.dp),
            )
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { isExpanded ->
                expanded = isExpanded
                if (isExpanded) {
                    searchQuery = ""
                }
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            topStart = BisqUIConstants.BorderRadius,
                            topEnd = BisqUIConstants.BorderRadius,
                        ),
                    ),
        ) {
            BisqTextFieldV0(
                value = textFieldValue,
                onValueChange = { value ->
                    searchQuery = value
                    expanded = true
                },
                placeholder = if (expanded) "mobile.components.dropdown.searchPlaceholder".i18n() else null,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                trailingIcon = {
                    ArrowDownIcon()
                },
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                    searchQuery = ""
                },
                modifier =
                    Modifier
                        .background(BisqTheme.colors.dark_grey40),
            ) {
                if (filteredOptions.isEmpty()) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(96.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BisqText.BaseRegular("mobile.components.select.empty".i18n())
                    }
                } else {
                    filteredOptions.forEach { (index, option) ->
                        DropdownMenuItem(
                            text = { BisqText.BaseRegular(option) },
                            onClick = {
                                onOptionSelect(index)
                                expanded = false
                                searchQuery = ""
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun BisqDropdownSearchablePreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.fillMaxWidth()) {
            var selectedIndex by remember { mutableIntStateOf(-1) }
            BisqDropdownSearchable(
                options = listOf("United States", "United Kingdom", "Germany", "France"),
                selectedIndex = selectedIndex,
                onOptionSelect = { index -> selectedIndex = index },
                label = "Country",
                prompt = "Select country",
            )
        }
    }
}

@Preview
@Composable
private fun BisqDropdownPromptPreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.fillMaxWidth()) {
            BisqDropdown(
                options = listOf("Option 1", "Option 2", "Option 3"),
                selectedIndex = -1,
                onOptionSelect = {},
                label = "Label",
                prompt = "Select an option",
            )
        }
    }
}

@Preview
@Composable
private fun BisqDropdownPreview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.fillMaxWidth()) {
            val options = listOf("Option 1", "Option 2", "Option 3", "Option 4", "Option 5")
            var selectedIndex by remember { mutableIntStateOf(0) }

            BisqDropdown(
                options = options,
                selectedIndex = selectedIndex,
                onOptionSelect = { index ->
                    selectedIndex = index
                    // You can also access the selected value: options[index]
                },
            )
        }
    }
}
