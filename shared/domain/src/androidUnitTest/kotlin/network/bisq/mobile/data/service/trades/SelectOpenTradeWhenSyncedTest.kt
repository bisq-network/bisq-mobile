package network.bisq.mobile.data.service.trades

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SelectOpenTradeWhenSyncedTest {
    private val tradeId = "tid"

    private fun facadeSyncing(
        openTradeItems: MutableStateFlow<List<TradeItemPresentationModel>>,
        openTradesSynced: MutableStateFlow<Boolean> = MutableStateFlow(false),
        openTradesSyncFailed: MutableStateFlow<Boolean> = MutableStateFlow(false),
    ): TradesServiceFacade {
        val facade = mockk<TradesServiceFacade>(relaxed = true)
        every { facade.openTradeItems } returns openTradeItems
        every { facade.openTradesSynced } returns openTradesSynced
        every { facade.openTradesSyncFailed } returns openTradesSyncFailed
        return facade
    }

    private fun tradeItem(id: String = tradeId): TradeItemPresentationModel {
        val trade = mockk<TradeItemPresentationModel>()
        every { trade.tradeId } returns id
        return trade
    }

    @Test
    fun `a trade already in the list resolves without waiting and is selected once`() =
        runTest {
            val item = tradeItem()
            val facade = facadeSyncing(MutableStateFlow(listOf(item)))

            assertEquals(item, facade.selectOpenTradeWhenSynced(tradeId))
            verify(exactly = 1) { facade.selectOpenTrade(tradeId) }
        }

    @Test
    fun `a trade that arrives with a later sync update still resolves`() =
        runTest {
            val openTradeItems = MutableStateFlow<List<TradeItemPresentationModel>>(emptyList())
            val facade = facadeSyncing(openTradeItems)

            val result = async { facade.selectOpenTradeWhenSynced(tradeId) }
            runCurrent()

            val item = tradeItem()
            openTradeItems.value = listOf(item)

            assertEquals(item, result.await())
        }

    /**
     * Two screens can wait on different trades at once (a deep link and a chat notification). Selecting
     * on every emission would have them overwrite each other's shared selection with null on every miss,
     * and the trade actions all read that selection.
     */
    @Test
    fun `waiting does not touch the shared selection`() =
        runTest {
            val openTradeItems = MutableStateFlow<List<TradeItemPresentationModel>>(emptyList())
            val facade = facadeSyncing(openTradeItems)

            val result = async { facade.selectOpenTradeWhenSynced(tradeId) }
            runCurrent()
            openTradeItems.value = listOf(tradeItem(id = "other"))
            runCurrent()

            assertTrue(result.isActive, "Still waiting for the trade")
            verify(exactly = 0) { facade.selectOpenTrade(any()) }
            result.cancel()
        }

    @Test
    fun `a trade missing from a synced list is reported as absent and never selected`() =
        runTest {
            val facade = facadeSyncing(MutableStateFlow(emptyList()), MutableStateFlow(true))

            assertNull(facade.selectOpenTradeWhenSynced(tradeId))
            verify(exactly = 0) { facade.selectOpenTrade(any()) }
        }

    @Test
    fun `a trade missing while the list is still syncing is not reported as absent`() =
        runTest {
            val openTradesSynced = MutableStateFlow(false)
            val facade = facadeSyncing(MutableStateFlow(emptyList()), openTradesSynced)

            val result = async { facade.selectOpenTradeWhenSynced(tradeId) }
            runCurrent()

            assertTrue(result.isActive, "Still waiting for the open trades to sync")

            openTradesSynced.value = true

            assertNull(result.await())
        }

    /** On the client a subscribe that fails once is only retried on the next reconnect. */
    @Test
    fun `a sync that failed is reported as absent rather than waited on`() =
        runTest {
            val openTradesSyncFailed = MutableStateFlow(false)
            val facade = facadeSyncing(MutableStateFlow(emptyList()), openTradesSyncFailed = openTradesSyncFailed)

            val result = async { facade.selectOpenTradeWhenSynced(tradeId) }
            runCurrent()
            assertTrue(result.isActive, "Still waiting for the open trades to sync")

            openTradesSyncFailed.value = true

            assertNull(result.await())
            verify(exactly = 0) { facade.selectOpenTrade(any()) }
        }
}
