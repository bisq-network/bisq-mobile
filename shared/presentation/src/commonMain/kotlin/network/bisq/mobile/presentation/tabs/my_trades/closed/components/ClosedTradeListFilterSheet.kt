package network.bisq.mobile.presentation.tabs.my_trades.closed.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
    sort: TradeSort,
    outcome: TradeOutcomeFilter,
    role: TradeRoleFilter,
    onSortChange: (TradeSort) -> Unit,
    onOutcomeChange: (TradeOutcomeFilter) -> Unit,
    onRoleChange: (TradeRoleFilter) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
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
                value = sort,
                items = TradeSort.entries.map { it to it.labelKey.i18n() },
                onValueChange = { pair -> onSortChange(pair.first) },
            )

            BisqGap.V1()

            SectionLabel("mobile.tradeHistory.filter.filterByOutcome".i18n())
            BisqGap.VHalf()
            BisqSegmentButton(
                label = "",
                value = outcome,
                items = TradeOutcomeFilter.entries.map { it to it.labelKey.i18n() },
                onValueChange = { pair -> onOutcomeChange(pair.first) },
            )

            BisqGap.V1()

            SectionLabel("mobile.tradeHistory.filter.filterByRole".i18n())
            BisqGap.VHalf()
            BisqSegmentButton(
                label = "",
                value = role,
                items = TradeRoleFilter.entries.map { it to it.labelKey.i18n() },
                onValueChange = { pair -> onRoleChange(pair.first) },
            )

            BisqHDivider(verticalPadding = BisqUIConstants.ScreenPaddingHalf)

            BisqButton(
                text = "mobile.tradeHistory.filter.action.reset".i18n(),
                type = BisqButtonType.Grey,
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
                fullWidth = true,
            )
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
            sort = TradeSort.NEWEST_FIRST,
            outcome = TradeOutcomeFilter.ALL,
            role = TradeRoleFilter.ALL,
            onSortChange = {},
            onOutcomeChange = {},
            onRoleChange = {},
            onReset = {},
            onDismiss = {},
        )
    }
}
