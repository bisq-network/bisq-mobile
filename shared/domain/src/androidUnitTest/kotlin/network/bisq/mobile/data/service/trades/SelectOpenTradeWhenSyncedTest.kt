package network.bisq.mobile.data.service.trades

import io.mockk.every
import io.mockk.mockk
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

    /** Mimics a facade whose lookup only succeeds once the trade is in the open trades list. */
    private fun facadeSyncing(
        openTradeItems: MutableStateFlow<List<TradeItemPresentationModel>>,
        openTradesSynced: MutableStateFlow<Boolean> = MutableStateFlow(false),
    ): TradesServiceFacade {
        val selected = MutableStateFlow<TradeItemPresentationModel?>(null)
        val facade = mockk<TradesServiceFacade>(relaxed = true)
        every { facade.openTradeItems } returns openTradeItems
        every { facade.openTradesSynced } returns openTradesSynced
        every { facade.selectedTrade } returns selected
        every { facade.selectOpenTrade(tradeId) } answers {
            selected.value = openTradeItems.value.find { it.tradeId == tradeId }
        }
        return facade
    }

    private fun tradeItem(): TradeItemPresentationModel {
        val trade = mockk<TradeItemPresentationModel>()
        every { trade.tradeId } returns tradeId
        return trade
    }

    @Test
    fun `a trade already in the list resolves without waiting`() =
        runTest {
            val item = tradeItem()
            val facade = facadeSyncing(MutableStateFlow(listOf(item)))

            assertEquals(item, facade.selectOpenTradeWhenSynced(tradeId))
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

    @Test
    fun `a trade missing from a synced list is reported as absent`() =
        runTest {
            val facade = facadeSyncing(MutableStateFlow(emptyList()), MutableStateFlow(true))

            assertNull(facade.selectOpenTradeWhenSynced(tradeId))
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
}
