package network.bisq.mobile.presentation.tabs.open_trades

import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.core.pagination.PaginatedResponse
import network.bisq.mobile.domain.model.trade.ClosedTradeListItem
import network.bisq.mobile.domain.model.trade.TradeOutcome
import network.bisq.mobile.domain.model.trade.TradeOutcomeFilter
import network.bisq.mobile.domain.model.trade.TradeRoleFilter
import network.bisq.mobile.domain.model.trade.TradeSort
import network.bisq.mobile.domain.usecase.trade.GetPaginatedClosedTradesUseCase
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.tabs.my_trades.closed.paging.ClosedTradesPagingSource
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ClosedTradesPagingSourceTest {
    private val useCase: GetPaginatedClosedTradesUseCase = mockk()
    private lateinit var totalCountSink: MutableStateFlow<Int?>

    private val pageSize = 20

    @BeforeTest
    fun setUp() {
        I18nSupport.initialize("en")
        // Fresh sink per test so a value set by one test (e.g., the failure-reset case priming
        // it to 99) cannot leak into another test's assertions.
        totalCountSink = MutableStateFlow(null)
    }

    private fun buildSource(
        sortBy: TradeSort = TradeSort.NEWEST_FIRST,
        outcomeFilter: TradeOutcomeFilter = TradeOutcomeFilter.ALL,
        roleFilter: TradeRoleFilter = TradeRoleFilter.ALL,
        searchQuery: String = "",
    ) = ClosedTradesPagingSource(
        useCase = useCase,
        searchQuery = searchQuery,
        sortBy = sortBy,
        outcomeFilter = outcomeFilter,
        roleFilter = roleFilter,
        totalCountSink = totalCountSink,
    )

    private fun sampleItem(tradeId: String): ClosedTradeListItem =
        ClosedTradeListItem(
            tradeId = tradeId,
            peersUserProfile = createMockUserProfile("Peer"),
            peersReputationScore = ReputationScoreVO(100L, 3.0, 50),
            myUserProfile = createMockUserProfile("Me"),
            priceQuote = null,
            fiatPaymentMethod = "SEPA",
            bitcoinSettlementMethod = "MAIN_CHAIN",
            isMaker = true,
            isBuyer = true,
            outcome = TradeOutcome.COMPLETED,
            takeOfferDate = 1_000_000L,
            tradeCompletedDate = null,
            baseAmount = 100_000L,
            quoteAmount = 50_000L,
            paymentAccountData = null,
            bitcoinPaymentData = null,
            paymentProof = null,
        )

    private fun refreshParams(key: Int? = null): LoadParams<Int> = LoadParams.Refresh(key = key, loadSize = pageSize, placeholdersEnabled = false)

    // -----------------------------------------------------------------------
    // First page
    // -----------------------------------------------------------------------

    @Test
    fun `load refresh on first page returns prevKey null and nextKey 2 when more pages exist`() =
        runTest {
            val items = listOf(sampleItem("t1"), sampleItem("t2"))
            coEvery { useCase(page = 1, pageSize = any(), any(), any(), any(), any()) } returns
                Result.success(
                    PaginatedResponse(items = items, page = 1, pageSize = 20, totalItems = 40, totalPages = 2),
                )

            val result = buildSource().load(refreshParams(key = null))

            assertIs<LoadResult.Page<Int, ClosedTradeListItem>>(result)
            assertNull(result.prevKey)
            assertEquals(2, result.nextKey)
            assertEquals(items, result.data)
        }

    // -----------------------------------------------------------------------
    // Last page
    // -----------------------------------------------------------------------

    @Test
    fun `load refresh on last page returns nextKey null`() =
        runTest {
            val items = listOf(sampleItem("t3"))
            coEvery { useCase(page = 3, pageSize = any(), any(), any(), any(), any()) } returns
                Result.success(
                    PaginatedResponse(items = items, page = 3, pageSize = 20, totalItems = 45, totalPages = 3),
                )

            val result = buildSource().load(refreshParams(key = 3))

            assertIs<LoadResult.Page<Int, ClosedTradeListItem>>(result)
            assertNull(result.nextKey)
            assertEquals(2, result.prevKey)
        }

    // -----------------------------------------------------------------------
    // Middle page
    // -----------------------------------------------------------------------

    @Test
    fun `load refresh on middle page returns both prevKey and nextKey`() =
        runTest {
            val items = listOf(sampleItem("t5"), sampleItem("t6"))
            coEvery { useCase(page = 2, pageSize = any(), any(), any(), any(), any()) } returns
                Result.success(
                    PaginatedResponse(items = items, page = 2, pageSize = 20, totalItems = 60, totalPages = 3),
                )

            val result = buildSource().load(refreshParams(key = 2))

            assertIs<LoadResult.Page<Int, ClosedTradeListItem>>(result)
            assertEquals(1, result.prevKey)
            assertEquals(3, result.nextKey)
        }

    // -----------------------------------------------------------------------
    // Failure
    // -----------------------------------------------------------------------

    @Test
    fun `load refresh on use-case failure returns LoadResult Error`() =
        runTest {
            val error = RuntimeException("network error")
            coEvery { useCase(page = 1, pageSize = any(), any(), any(), any(), any()) } returns
                Result.failure(error)

            val result = buildSource().load(refreshParams(key = null))

            assertIs<LoadResult.Error<Int, ClosedTradeListItem>>(result)
            assertEquals(error, result.throwable)
        }

    @Test
    fun `load refresh on failure resets totalCountSink to null`() =
        runTest {
            totalCountSink.value = 99
            coEvery { useCase(page = 1, pageSize = any(), any(), any(), any(), any()) } returns
                Result.failure(RuntimeException("boom"))

            buildSource().load(refreshParams(key = null))

            assertNull(totalCountSink.value)
        }

    // -----------------------------------------------------------------------
    // totalCountSink on success
    // -----------------------------------------------------------------------

    @Test
    fun `load refresh on success updates totalCountSink to totalItems`() =
        runTest {
            val items = listOf(sampleItem("t10"))
            coEvery { useCase(page = 1, pageSize = any(), any(), any(), any(), any()) } returns
                Result.success(
                    PaginatedResponse(items = items, page = 1, pageSize = 20, totalItems = 42, totalPages = 3),
                )

            buildSource().load(refreshParams(key = null))

            assertEquals(42, totalCountSink.value)
        }
}
