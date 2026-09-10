package network.bisq.mobile.data.service.trades

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.domain.core.pagination.PaginatedResponse
import network.bisq.mobile.domain.core.pagination.PaginationParams
import network.bisq.mobile.domain.model.trade.ClosedTradeListItem
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HasTradedWithTest {
    private val peerId = "peer-1"

    private fun facade(
        openTrades: List<TradeItemPresentationModel> = emptyList(),
        closedPages: List<List<ClosedTradeListItem>> = listOf(emptyList()),
        openTradesSynced: MutableStateFlow<Boolean> = MutableStateFlow(true),
        openTradeItems: MutableStateFlow<List<TradeItemPresentationModel>> = MutableStateFlow(openTrades),
    ): TradesServiceFacade {
        val facade = mockk<TradesServiceFacade>(relaxed = true)
        every { facade.openTradeItems } returns openTradeItems
        every { facade.openTradesSynced } returns openTradesSynced
        every { facade.openTradesSyncFailed } returns MutableStateFlow(false)
        coEvery { facade.getClosedTradesPaginated(any(), any(), any(), any(), any()) } answers {
            val params = firstArg<PaginationParams>()
            val items = closedPages.getOrElse(params.page - 1) { emptyList() }
            Result.success(
                PaginatedResponse(
                    items = items,
                    page = params.page,
                    pageSize = params.pageSize,
                    totalItems = closedPages.sumOf { it.size }.toLong(),
                    totalPages = closedPages.size,
                ),
            )
        }
        return facade
    }

    private fun openTrade(peerProfileId: String): TradeItemPresentationModel =
        mockk {
            every { peersUserProfile } returns createMockUserProfile(peerProfileId)
        }

    private fun closedTrade(peerProfileId: String): ClosedTradeListItem =
        mockk {
            every { peersUserProfile } returns createMockUserProfile(peerProfileId)
        }

    @Test
    fun `a peer in the open trades resolves without touching the closed-trades history`() =
        runTest {
            val facade = facade(openTrades = listOf(openTrade(peerId)))

            assertTrue(facade.hasTradedWith(peerId))
            coVerify(exactly = 0) { facade.getClosedTradesPaginated(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `a peer found on a later closed-trades page resolves true`() =
        runTest {
            val facade =
                facade(
                    closedPages =
                        listOf(
                            listOf(closedTrade("someone-else")),
                            listOf(closedTrade(peerId)),
                        ),
                )

            assertTrue(facade.hasTradedWith(peerId))
            coVerify(exactly = 2) { facade.getClosedTradesPaginated(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `an exhausted history without the peer resolves false`() =
        runTest {
            val facade =
                facade(
                    openTrades = listOf(openTrade("other-open")),
                    closedPages = listOf(listOf(closedTrade("other-closed"))),
                )

            assertFalse(facade.hasTradedWith(peerId))
        }

    /**
     * On Bisq Connect the closed-trades API is capability-gated; an old trusted node answers with a
     * failure. Degrading to false is the documented contract — the peer profile's gate also admits
     * contacts, so an under-report only hides the section, never breaks it.
     */
    @Test
    fun `a failing closed-trades lookup degrades to false`() =
        runTest {
            val facade = mockk<TradesServiceFacade>(relaxed = true)
            every { facade.openTradeItems } returns MutableStateFlow(emptyList())
            every { facade.openTradesSynced } returns MutableStateFlow(true)
            every { facade.openTradesSyncFailed } returns MutableStateFlow(false)
            coEvery { facade.getClosedTradesPaginated(any(), any(), any(), any(), any()) } returns
                Result.failure(RuntimeException("closed-trades API unavailable on this node"))

            assertFalse(facade.hasTradedWith(peerId))
        }

    /**
     * On a cold start the open-trades list is empty because the TRADES snapshot has not arrived,
     * not because there are no trades — the check must wait for sync (same contract as
     * [selectOpenTradeWhenSynced]) instead of under-reporting from a premature read.
     */
    @Test
    fun `an unsynced open-trades list is awaited until the snapshot lands`() =
        runTest {
            val openTradeItems = MutableStateFlow<List<TradeItemPresentationModel>>(emptyList())
            val openTradesSynced = MutableStateFlow(false)
            val facade =
                facade(
                    openTradesSynced = openTradesSynced,
                    openTradeItems = openTradeItems,
                )

            val result = async { facade.hasTradedWith(peerId) }
            runCurrent()

            openTradeItems.value = listOf(openTrade(peerId))
            openTradesSynced.value = true

            assertTrue(result.await())
            coVerify(exactly = 0) { facade.getClosedTradesPaginated(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `an empty history resolves false after a single page`() =
        runTest {
            val facade = facade()

            assertFalse(facade.hasTradedWith(peerId))
            coVerify(exactly = 1) { facade.getClosedTradesPaginated(any(), any(), any(), any(), any()) }
        }
}
