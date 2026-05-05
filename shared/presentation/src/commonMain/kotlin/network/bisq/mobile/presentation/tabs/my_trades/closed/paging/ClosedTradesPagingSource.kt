package network.bisq.mobile.presentation.tabs.my_trades.closed.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.domain.core.pagination.PaginationParams
import network.bisq.mobile.domain.model.trade.ClosedTradeListItem
import network.bisq.mobile.domain.model.trade.TradeOutcomeFilter
import network.bisq.mobile.domain.model.trade.TradeRoleFilter
import network.bisq.mobile.domain.model.trade.TradeSort
import network.bisq.mobile.domain.usecase.trade.GetPaginatedClosedTradesUseCase

class ClosedTradesPagingSource(
    private val useCase: GetPaginatedClosedTradesUseCase,
    private val searchQuery: String,
    private val sortBy: TradeSort,
    private val outcomeFilter: TradeOutcomeFilter,
    private val roleFilter: TradeRoleFilter,
    private val totalCountSink: MutableStateFlow<Int?>,
) : PagingSource<Int, ClosedTradeListItem>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ClosedTradeListItem> {
        val page = params.key ?: PaginationParams.DEFAULT_PAGE
        return useCase(
            page = page,
            pageSize = PaginationParams.DEFAULT_PAGE_SIZE,
            searchQuery = searchQuery,
            sortBy = sortBy,
            outcomeFilter = outcomeFilter,
            roleFilter = roleFilter,
        ).fold(
            onSuccess = { response ->
                totalCountSink.value = response.totalItems.toInt()
                LoadResult.Page(
                    data = response.items,
                    prevKey = if (page <= 1) null else page - 1,
                    nextKey = if (page >= response.totalPages) null else page + 1,
                )
            },
            onFailure = {
                totalCountSink.value = null
                LoadResult.Error(it)
            },
        )
    }

    override fun getRefreshKey(state: PagingState<Int, ClosedTradeListItem>): Int? =
        state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
}
