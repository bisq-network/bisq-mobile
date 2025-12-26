package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ArrowDownIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BisqDropdown(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }

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
                value = options.getOrNull(selectedIndex) ?: EMPTY_STRING,
                onValueChange = {},
                readOnly = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
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
