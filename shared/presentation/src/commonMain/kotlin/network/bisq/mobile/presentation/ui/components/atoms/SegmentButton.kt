package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun BisqSegmentButton(
    label: String = "",
    disabled: Boolean = false,
    value: String,
    items: List<Pair<String, String>>,
    onValueChange: ((Pair<String, String>) -> Unit)? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    var selectedIndex by remember { mutableIntStateOf(
        items.indexOfFirst{ it.first == value }
    ) }

    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            BisqText.baseRegular(label)
            BisqGap.VQuarter()
        }
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = items.size
                    ),
                    onClick = {
                        selectedIndex = index
                        if (onValueChange != null) {
                            onValueChange(items[selectedIndex])
                        }
                    },
                    selected = index == selectedIndex,
                    enabled = !disabled,
                    label = {
                        BisqText.baseRegular(
                            label.second,
                            color = if(disabled)
                                BisqTheme.colors.mid_grey20
                            else
                                BisqTheme.colors.white
                        )
                    },
                    colors = SegmentedButtonColors(
                        activeContainerColor = BisqTheme.colors.secondary,
                        activeContentColor = BisqTheme.colors.light_grey50,
                        activeBorderColor = BisqTheme.colors.backgroundColor,
                        inactiveContainerColor = BisqTheme.colors.secondaryDisabled,
                        inactiveContentColor = BisqTheme.colors.light_grey50,
                        inactiveBorderColor = BisqTheme.colors.backgroundColor,
                        disabledActiveContainerColor = BisqTheme.colors.secondaryDisabled,
                        disabledActiveContentColor = BisqTheme.colors.mid_grey30,
                        disabledActiveBorderColor = BisqTheme.colors.backgroundColor,
                        disabledInactiveContainerColor = BisqTheme.colors.secondaryDisabled,
                        disabledInactiveContentColor = BisqTheme.colors.mid_grey30,
                        disabledInactiveBorderColor = BisqTheme.colors.backgroundColor,
                    )
                )
            }
        }
    }
}