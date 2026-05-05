package network.bisq.mobile.presentation.tabs.my_trades.closed.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.domain.model.trade.TradeOutcomeFilter
import network.bisq.mobile.domain.model.trade.TradeRoleFilter
import network.bisq.mobile.domain.model.trade.TradeSort
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqSegmentButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

@Composable
fun ClosedTradeListFilterSheet(
    initialSort: TradeSort,
    initialOutcome: TradeOutcomeFilter,
    initialRole: TradeRoleFilter,
    onApply: (TradeSort, TradeOutcomeFilter, TradeRoleFilter) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var sortOrder by remember { mutableStateOf(initialSort) }
    var outcomeFilter by remember { mutableStateOf(initialOutcome) }
    var roleFilter by remember { mutableStateOf(initialRole) }

    BisqBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BisqUIConstants.ScreenPadding),
        ) {
            SectionLabel("mobile.tradeHistory.filter.sortBy".i18n())
            BisqGap.VHalf()
            BisqSegmentButton(
                label = "",
                value = sortOrder,
                items = TradeSort.entries.map { it to it.labelKey.i18n() },
                onValueChange = { pair -> sortOrder = pair.first },
            )

            BisqGap.V1()

            SectionLabel("mobile.tradeHistory.filter.filterByOutcome".i18n())
            BisqGap.VHalf()
            BisqSegmentButton(
                label = "",
                value = outcomeFilter,
                items = TradeOutcomeFilter.entries.map { it to it.labelKey.i18n() },
                onValueChange = { pair -> outcomeFilter = pair.first },
            )

            BisqGap.V1()

            SectionLabel("mobile.tradeHistory.filter.filterByRole".i18n())
            BisqGap.VHalf()
            BisqSegmentButton(
                label = "",
                value = roleFilter,
                items = TradeRoleFilter.entries.map { it to it.labelKey.i18n() },
                onValueChange = { pair -> roleFilter = pair.first },
            )

            BisqHDivider(verticalPadding = BisqUIConstants.ScreenPaddingHalf)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                BisqButton(
                    text = "mobile.tradeHistory.filter.action.reset".i18n(),
                    type = BisqButtonType.Grey,
                    onClick = {
                        sortOrder = TradeSort.NEWEST_FIRST
                        outcomeFilter = TradeOutcomeFilter.ALL
                        roleFilter = TradeRoleFilter.ALL
                        onReset()
                    },
                    modifier = Modifier.weight(1f),
                    fullWidth = true,
                )
                BisqButton(
                    text = "mobile.tradeHistory.filter.action.apply".i18n(),
                    onClick = { onApply(sortOrder, outcomeFilter, roleFilter) },
                    modifier = Modifier.weight(1f),
                    fullWidth = true,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    BisqText.BaseRegular(text = text, color = BisqTheme.colors.white)
}

@ExcludeFromCoverage
@Preview
@Composable
private fun ClosedTradeListFilterSheet_Preview() {
    BisqTheme.Preview {
        ClosedTradeListFilterSheet(
            initialSort = TradeSort.NEWEST_FIRST,
            initialOutcome = TradeOutcomeFilter.ALL,
            initialRole = TradeRoleFilter.ALL,
            onApply = { _, _, _ -> },
            onReset = {},
            onDismiss = {},
        )
    }
}
