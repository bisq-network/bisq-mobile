package network.bisq.mobile.domain.usecase.trade

import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.model.trade.TradeRoleFilter
import network.bisq.mobile.domain.model.trade.TradeSort

class FilterOpenTradesUseCase {
    operator fun invoke(
        items: List<TradeItemPresentationModel>,
        searchQuery: String = "",
        sortBy: TradeSort = TradeSort.NEWEST_FIRST,
        roleFilter: TradeRoleFilter = TradeRoleFilter.ALL,
    ): List<TradeItemPresentationModel> {
        val q = searchQuery.trim()
        val filtered =
            items
                .asSequence()
                .filter { roleFilter.matches(!it.bisqEasyTradeModel.isSeller) }
                .filter { matchesQuery(it, q) }
                .toList()

        return when (sortBy) {
            TradeSort.NEWEST_FIRST -> filtered.sortedByDescending { it.bisqEasyTradeModel.takeOfferDate }
            TradeSort.OLDEST_FIRST -> filtered.sortedBy { it.bisqEasyTradeModel.takeOfferDate }
            TradeSort.AMOUNT_HIGH_LOW -> filtered.sortedByDescending { it.quoteAmount }
            TradeSort.AMOUNT_LOW_HIGH -> filtered.sortedBy { it.quoteAmount }
        }
    }

    private fun matchesQuery(
        item: TradeItemPresentationModel,
        query: String,
    ): Boolean {
        if (query.isEmpty()) return true
        val haystack =
            listOf(
                item.peersUserName,
                item.myUserName,
                item.shortTradeId,
                item.tradeId,
                item.directionalTitle,
                item.quoteAmountWithCode,
                item.baseAmountWithCode,
                item.formattedPrice,
                item.bitcoinSettlementMethodDisplayString,
                item.fiatPaymentMethodDisplayString,
                item.market,
            )
        return haystack.any { it.contains(query, ignoreCase = true) }
    }
}
