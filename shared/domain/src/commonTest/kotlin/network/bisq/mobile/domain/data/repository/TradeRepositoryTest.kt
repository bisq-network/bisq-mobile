package network.bisq.mobile.domain.data.repository

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import network.bisq.mobile.domain.data.model.Trade
import network.bisq.mobile.domain.data.persistance.KeyValueStorage
import network.bisq.mobile.domain.data.persistance.PersistenceSource
import network.bisq.mobile.domain.di.testModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TradeRepositoryTest : KoinTest {

    private lateinit var tradeRepository: TradeRepository
    private val persistenceSource: PersistenceSource<Trade> by inject(qualifier = named("tradeStorage"))

    @BeforeTest
    fun setup() {
        startKoin {
            modules(testModule)
        }
        tradeRepository = TradeRepository(persistenceSource as KeyValueStorage<Trade>)
    }

    @AfterTest
    fun teardown() {
        runBlocking {
            tradeRepository.clear()
        }
        stopKoin()
    }

    @Test
    fun testCreateAndFetchById() = runBlocking {
        // Create a trade
        val trade = createSampleTrade("1", "BTC", "OPEN")
        tradeRepository.create(trade)

        // Fetch by ID
        val fetchedTrade = tradeRepository.fetchById("1")
        assertNotNull(fetchedTrade)
        assertEquals("1", fetchedTrade.tradeId)
        assertEquals("BTC", fetchedTrade.offerCurrency)
        assertEquals("OPEN", fetchedTrade.status)
    }

    @Test
    fun testFetchAll() = runBlocking {
        // Create multiple trades
        val trade1 = createSampleTrade("1", "BTC", "OPEN")
        val trade2 = createSampleTrade("2", "ETH", "CLOSED")
        tradeRepository.create(trade1)
        tradeRepository.create(trade2)

        // Fetch all
        val allTrades = tradeRepository.fetchAll()
        assertEquals(2, allTrades.size)
        assertEquals(setOf("1", "2"), allTrades.map { it.tradeId }.toSet())
    }

    @Test
    fun testUpdate() = runBlocking {
        // Create a trade
        val trade = createSampleTrade("1", "BTC", "OPEN")
        tradeRepository.create(trade)

        // Update the trade
        val updatedTrade = trade.apply {
            status = "IN_PROGRESS"
            offerAmount = 2.0
        }
        tradeRepository.update(updatedTrade)

        // Verify update
        val fetchedTrade = tradeRepository.fetchById("1")
        assertNotNull(fetchedTrade)
        assertEquals("IN_PROGRESS", fetchedTrade.status)
        assertEquals(2.0, fetchedTrade.offerAmount)
    }

    @Test
    fun testDelete() = runBlocking {
        // Create a trade
        val trade = createSampleTrade("1", "BTC", "OPEN")
        tradeRepository.create(trade)

        // Delete the trade
        tradeRepository.delete(trade)

        // Verify deletion
        val fetchedTrade = tradeRepository.fetchById("1")
        assertNull(fetchedTrade)
    }

    @Test
    fun testFindByStatus() = runBlocking {
        // Create trades with different statuses
        val trade1 = createSampleTrade("1", "BTC", "OPEN")
        val trade2 = createSampleTrade("2", "ETH", "CLOSED")
        val trade3 = createSampleTrade("3", "LTC", "OPEN")
        tradeRepository.create(trade1)
        tradeRepository.create(trade2)
        tradeRepository.create(trade3)

        // Find by status
        val openTrades = tradeRepository.findByStatus("OPEN")
        assertEquals(2, openTrades.size)
        assertEquals(setOf("1", "3"), openTrades.map { it.tradeId }.toSet())
    }

    @Test
    fun testFindByCurrency() = runBlocking {
        // Create trades with different currencies
        val trade1 = createSampleTrade("1", "BTC", "OPEN")
        val trade2 = createSampleTrade("2", "ETH", "CLOSED")
        val trade3 = createSampleTrade("3", "BTC", "CLOSED")
        tradeRepository.create(trade1)
        tradeRepository.create(trade2)
        tradeRepository.create(trade3)

        // Find by currency
        val btcTrades = tradeRepository.findByCurrency("BTC")
        assertEquals(2, btcTrades.size)
        assertEquals(setOf("1", "3"), btcTrades.map { it.tradeId }.toSet())
    }

    @Test
    fun testUpdateStatus() = runBlocking {
        // Create a trade
        val trade = createSampleTrade("1", "BTC", "OPEN")
        tradeRepository.create(trade)

        // Update status
        val updatedTrade = tradeRepository.updateStatus("1", "COMPLETED")
        assertNotNull(updatedTrade)
        assertEquals("COMPLETED", updatedTrade.status)

        // Verify update in repository
        val fetchedTrade = tradeRepository.fetchById("1")
        assertNotNull(fetchedTrade)
        assertEquals("COMPLETED", fetchedTrade.status)
    }

    @Test
    fun testUpdateNonExistentTrade() = runBlocking {
        // Try to update a non-existent trade
        val updatedTrade = tradeRepository.updateStatus("999", "COMPLETED")
        assertNull(updatedTrade)
    }

    private fun createSampleTrade(id: String, currency: String, status: String): Trade {
        return Trade(id).apply {
            offerCurrency = currency
            this.status = status
            offerAmount = 1.0
            price = 50000.0
            createdAt = Clock.System.now().toEpochMilliseconds()
            updatedAt = createdAt
        }
    }
}
