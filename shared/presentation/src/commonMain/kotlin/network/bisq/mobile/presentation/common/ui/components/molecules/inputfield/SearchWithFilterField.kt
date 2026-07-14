package network.bisq.mobile.presentation.common.ui.components.molecules.inputfield

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.GreenSortIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.SortIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

/**
 * [BisqSearchField] with the standard filter/sort button suffix.
 * The icon renders green while a filter is active.
 */
@Composable
fun SearchWithFilterField(
    value: String,
    onValueChange: (String) -> Unit,
    isFilterActive: Boolean,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "action.search".i18n(),
) {
    BisqSearchField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        modifier = modifier,
        rightSuffix = {
            BisqButton(
                iconOnly = {
                    if (isFilterActive) {
                        GreenSortIcon()
                    } else {
                        SortIcon()
                    }
                },
                onClick = onFilterClick,
                type = BisqButtonType.Clear,
                modifier = Modifier.weight(1f),
            )
        },
    )
}

@Preview
@Composable
private fun SearchWithFilterField_InactivePreview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            var textState by remember { mutableStateOf("") }
            SearchWithFilterField(
                value = textState,
                onValueChange = { textState = it },
                isFilterActive = false,
                onFilterClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun SearchWithFilterField_ActivePreview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            var textState by remember { mutableStateOf("bitcoin") }
            SearchWithFilterField(
                value = textState,
                onValueChange = { textState = it },
                isFilterActive = true,
                onFilterClick = {},
            )
        }
    }
}
