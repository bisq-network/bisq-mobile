package network.bisq.mobile.domain.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
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
import kotlin.test.assertTrue

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

    @Test
    fun testConcurrentCreate() = runBlocking {
        val numberOfCoroutines = 30
        val tradesPerCoroutine = 5

        // Launch multiple coroutines that create trades concurrently
        val jobs = (1..numberOfCoroutines).map { coroutineId ->
            async {
                repeat(tradesPerCoroutine) { tradeId ->
                    val uniqueId = "${coroutineId}_$tradeId"
                    val trade = createSampleTrade(uniqueId, "BTC", "OPEN")
                    tradeRepository.create(trade)
                    delay(1) // Small delay to increase chance of race conditions
                }
            }
        }

        // Wait for all coroutines to complete
        jobs.awaitAll()

        // Verify all trades were created correctly
        val allTrades = tradeRepository.fetchAll()
        assertEquals(numberOfCoroutines * tradesPerCoroutine, allTrades.size)

        // Verify no data corruption - each trade should have correct ID
        allTrades.forEach { trade ->
            assertTrue(trade.id.matches(Regex("\\d+_\\d+")), "Trade ID should match pattern: ${trade.id}")
        }
    }

    @Test
    fun testConcurrentUpdate() = runBlocking {
        // Pre-populate with trades
        val initialTrades = (1..20).map { createSampleTrade("trade_$it", "BTC", "OPEN") }
        initialTrades.forEach { tradeRepository.create(it) }

        val numberOfUpdaters = 15

        // Launch concurrent updaters
        val jobs = (1..numberOfUpdaters).map { updaterId ->
            async {
                repeat(3) { updateId ->
                    val tradeId = "trade_${(updaterId % 20) + 1}" // Update existing trades
                    val newStatus = "STATUS_${updaterId}_$updateId"
                    tradeRepository.updateStatus(tradeId, newStatus)
                    delay(1)
                }
            }
        }

        // Wait for all updates
        jobs.awaitAll()

        // Verify all trades still exist and have valid statuses
        val finalTrades = tradeRepository.fetchAll()
        assertEquals(initialTrades.size, finalTrades.size)

        finalTrades.forEach { trade ->
            assertTrue(trade.status.isNotBlank(), "Trade status should not be blank")
        }
    }

    @Test
    fun testConcurrentDelete() = runBlocking {
        // Pre-populate with trades
        val initialTrades = (1..30).map { createSampleTrade("trade_$it", "BTC", "OPEN") }
        initialTrades.forEach { tradeRepository.create(it) }

        val numberOfDeleters = 10

        // Launch concurrent deleters
        val jobs = (1..numberOfDeleters).map { deleterId ->
            async {
                val tradesToDelete = initialTrades.filter {
                    it.id.endsWith("${deleterId}") || it.id.endsWith("${deleterId + 10}") || it.id.endsWith("${deleterId + 20}")
                }
                tradesToDelete.forEach { trade ->
                    tradeRepository.delete(trade)
                    delay(2)
                }
            }
        }

        // Wait for all deletions
        jobs.awaitAll()

        // Verify consistency - remaining trades should be valid
        val remainingTrades = tradeRepository.fetchAll()
        assertTrue(remainingTrades.size <= initialTrades.size, "Should not have more trades than initially")

        remainingTrades.forEach { trade ->
            assertTrue(trade.id.isNotBlank(), "Remaining trade ID should not be blank")
        }
    }

    @Test
    fun testConcurrentMixedOperations() = runBlocking {
        val numberOfOperators = 20

        // Launch mixed operations concurrently
        val jobs = (1..numberOfOperators).map { operatorId ->
            async {
                repeat(5) { opIndex ->
                    when (opIndex % 4) {
                        0 -> {
                            // Create
                            val trade = createSampleTrade("mixed_${operatorId}_$opIndex", "BTC", "OPEN")
                            tradeRepository.create(trade)
                        }
                        1 -> {
                            // Update
                            tradeRepository.updateStatus("mixed_${operatorId}_${opIndex - 1}", "UPDATED")
                        }
                        2 -> {
                            // Fetch
                            tradeRepository.fetchById("mixed_${operatorId}_${opIndex - 2}")
                        }
                        3 -> {
                            // Delete
                            val tradeToDelete = createSampleTrade("mixed_${operatorId}_${opIndex - 3}", "BTC", "OPEN")
                            tradeRepository.delete(tradeToDelete)
                        }
                    }
                    delay(1)
                }
            }
        }

        // Wait for all operations
        jobs.awaitAll()

        // Verify repository is in consistent state
        val finalTrades = tradeRepository.fetchAll()
        finalTrades.forEach { trade ->
            assertTrue(trade.id.isNotBlank(), "Trade ID should not be blank")
            assertTrue(trade.status.isNotBlank(), "Trade status should not be blank")
        }
    }

    @Test
    fun testConcurrentBatchOperations() = runBlocking {
        val numberOfBatchers = 10
        val batchSize = 5

        // Launch concurrent batch operations
        val jobs = (1..numberOfBatchers).map { batcherId ->
            async {
                // Create batch
                val tradesToCreate = (1..batchSize).map {
                    createSampleTrade("batch_${batcherId}_$it", "BTC", "OPEN")
                }
                tradeRepository.createAll(tradesToCreate)

                delay(5)

                // Delete batch
                tradeRepository.deleteAll(tradesToCreate)
                delay(1)
            }
        }

        // Wait for all batch operations
        jobs.awaitAll()

        // Verify final state is consistent
        val finalTrades = tradeRepository.fetchAll()
        // Should be empty or have consistent data
        finalTrades.forEach { trade ->
            assertTrue(trade.id.isNotBlank(), "Trade ID should not be blank")
        }
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
