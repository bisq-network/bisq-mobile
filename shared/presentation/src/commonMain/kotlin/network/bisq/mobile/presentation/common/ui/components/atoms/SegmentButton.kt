package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme

@Composable
fun <T> BisqSegmentButton(
    value: T,
    items: List<Pair<T, String>>,
    modifier: Modifier = Modifier.fillMaxWidth(),
    label: String = "",
    disabled: Boolean = false,
    onValueChange: ((Pair<T, String>) -> Unit)? = null,
) {
    val selectedIndex = items.indexOfFirst { it.first == value }.coerceAtLeast(0)

    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            BisqText.BaseRegular(label)
            BisqGap.VQuarter()
        }
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            items.forEachIndexed { index, pair ->
                SegmentedButton(
                    shape =
                        SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = items.size,
                        ),
                    onClick = {
                        onValueChange?.invoke(items[index])
                    },
                    selected = index == selectedIndex,
                    enabled = !disabled,
                    label = {
                        BisqText.BaseRegular(
                            pair.second,
                            color =
                                if (disabled) {
                                    BisqTheme.colors.mid_grey20
                                } else {
                                    BisqTheme.colors.white
                                },
                        )
                    },
                    colors =
                        SegmentedButtonColors(
                            activeContainerColor = BisqTheme.colors.primary,
                            activeContentColor = BisqTheme.colors.light_grey50,
                            activeBorderColor = BisqTheme.colors.backgroundColor,
                            inactiveContainerColor = BisqTheme.colors.secondaryDisabled,
                            inactiveContentColor = BisqTheme.colors.light_grey50,
                            inactiveBorderColor = BisqTheme.colors.backgroundColor,
                            disabledActiveContainerColor = BisqTheme.colors.primaryDisabled,
                            disabledActiveContentColor = BisqTheme.colors.mid_grey30,
                            disabledActiveBorderColor = BisqTheme.colors.backgroundColor,
                            disabledInactiveContainerColor = BisqTheme.colors.secondaryDisabled,
                            disabledInactiveContentColor = BisqTheme.colors.mid_grey30,
                            disabledInactiveBorderColor = BisqTheme.colors.backgroundColor,
                        ),
                )
            }
        }
    }
}
