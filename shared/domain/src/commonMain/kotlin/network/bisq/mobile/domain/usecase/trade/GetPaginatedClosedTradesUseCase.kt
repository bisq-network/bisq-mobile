package network.bisq.mobile.domain.usecase.trade

import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.core.pagination.PaginatedResponse
import network.bisq.mobile.domain.core.pagination.PaginationParams
import network.bisq.mobile.domain.model.trade.ClosedTradeListItem
import network.bisq.mobile.domain.model.trade.TradeOutcomeFilter
import network.bisq.mobile.domain.model.trade.TradeRoleFilter
import network.bisq.mobile.domain.model.trade.TradeSort

class GetPaginatedClosedTradesUseCase(
    private val tradesServiceFacade: TradesServiceFacade,
) {
    suspend operator fun invoke(
        page: Int = PaginationParams.DEFAULT_PAGE,
        pageSize: Int = PaginationParams.DEFAULT_PAGE_SIZE,
        searchQuery: String = "",
        sortBy: TradeSort = TradeSort.NEWEST_FIRST,
        outcomeFilter: TradeOutcomeFilter = TradeOutcomeFilter.ALL,
        roleFilter: TradeRoleFilter = TradeRoleFilter.ALL,
    ): Result<PaginatedResponse<ClosedTradeListItem>> =
        tradesServiceFacade.getClosedTradesPaginated(
            params = PaginationParams.of(page, pageSize),
            search = searchQuery.trim().takeIf { it.isNotEmpty() },
            sortBy = sortBy,
            outcomeFilter = outcomeFilter,
            roleFilter = roleFilter,
        )
}
