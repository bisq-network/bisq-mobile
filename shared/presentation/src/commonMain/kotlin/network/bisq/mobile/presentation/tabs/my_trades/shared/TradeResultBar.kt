package network.bisq.mobile.presentation.tabs.my_trades.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.domain.model.trade.TradeOutcomeFilter
import network.bisq.mobile.domain.model.trade.TradeRoleFilter
import network.bisq.mobile.domain.model.trade.TradeSort
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

@Composable
fun TradeResultBar(
    sort: TradeSort,
    outcome: TradeOutcomeFilter,
    role: TradeRoleFilter,
    loadedCount: Int,
    totalCount: Int,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val parts =
        buildList {
            if (sort != TradeSort.NEWEST_FIRST) add(sort.labelKey.i18n())
            if (outcome != TradeOutcomeFilter.ALL) add(outcome.labelKey.i18n())
            if (role != TradeRoleFilter.ALL) add(role.labelKey.i18n())
        }

    val hasFilter = parts.isNotEmpty()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasFilter) {
            val summary = parts.joinToString(" · ")
            val countText =
                "mobile.tradeHistory.count.filtered".i18n(
                    loadedCount.toString(),
                    totalCount.toString(),
                )

            Column(modifier = Modifier.weight(1f)) {
                BisqText.SmallLightGrey(text = summary)
                BisqText.SmallLightGrey(text = countText)
            }
            BisqButton(
                text = "mobile.tradeHistory.filter.action.reset".i18n(),
                type = BisqButtonType.Underline,
                onClick = onClearAll,
            )
        } else {
            BisqText.SmallLightGrey(
                text =
                    if (totalCount == 1) {
                        "mobile.tradeHistory.count.one".i18n()
                    } else {
                        "mobile.tradeHistory.count.many".i18n(totalCount.toString())
                    },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun TradeResultBar_Filtered_Preview() {
    BisqTheme.Preview {
        TradeResultBar(
            sort = TradeSort.OLDEST_FIRST,
            outcome = TradeOutcomeFilter.COMPLETED,
            role = TradeRoleFilter.BUYER,
            loadedCount = 2,
            totalCount = 5,
            onClearAll = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun TradeResultBar_Default_Preview() {
    BisqTheme.Preview {
        TradeResultBar(
            sort = TradeSort.NEWEST_FIRST,
            outcome = TradeOutcomeFilter.ALL,
            role = TradeRoleFilter.ALL,
            loadedCount = 1,
            totalCount = 1,
            onClearAll = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
