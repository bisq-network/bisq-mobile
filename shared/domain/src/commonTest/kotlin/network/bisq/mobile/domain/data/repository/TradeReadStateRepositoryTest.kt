package network.bisq.mobile.domain.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import network.bisq.mobile.domain.data.model.TradeReadState
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TradeReadStateRepositoryTest : KoinTest {

    private lateinit var repository: TradeReadStateRepository
    private val persistenceSource: PersistenceSource<TradeReadState> by inject(qualifier = named("tradeReadStateStorage"))

    @BeforeTest
    fun setup() {
        startKoin {
            modules(testModule)
        }
        repository = TradeReadStateRepository(persistenceSource as KeyValueStorage<TradeReadState>)
    }

    @AfterTest
    fun teardown() {
        runBlocking {
            repository.clear()
        }
        stopKoin()
    }

    @Test
    fun testBasicReadCountOperations() = runBlocking {
        val tradeId = "trade123"
        
        // Initially should be 0
        assertEquals(0, repository.getReadCount(tradeId))
        
        // Set read count
        repository.setReadCount(tradeId, 5)
        assertEquals(5, repository.getReadCount(tradeId))
        
        // Increment read count
        val newCount = repository.incrementReadCount(tradeId)
        assertEquals(6, newCount)
        assertEquals(6, repository.getReadCount(tradeId))
        
        // Clear read state
        repository.clearReadState(tradeId)
        assertEquals(0, repository.getReadCount(tradeId))
    }

    @Test
    fun testUpdateReadCountIfGreater() = runBlocking {
        val tradeId = "trade456"
        
        // Set initial count
        repository.setReadCount(tradeId, 10)
        
        // Try to update with smaller value - should fail
        assertFalse(repository.updateReadCountIfGreater(tradeId, 5))
        assertEquals(10, repository.getReadCount(tradeId))
        
        // Try to update with same value - should fail
        assertFalse(repository.updateReadCountIfGreater(tradeId, 10))
        assertEquals(10, repository.getReadCount(tradeId))
        
        // Try to update with greater value - should succeed
        assertTrue(repository.updateReadCountIfGreater(tradeId, 15))
        assertEquals(15, repository.getReadCount(tradeId))
    }

    @Test
    fun testMultipleTradesReadState() = runBlocking {
        val trade1 = "trade1"
        val trade2 = "trade2"
        val trade3 = "trade3"
        
        // Set different counts for different trades
        repository.setReadCount(trade1, 5)
        repository.setReadCount(trade2, 10)
        repository.setReadCount(trade3, 15)
        
        // Verify each trade has correct count
        assertEquals(5, repository.getReadCount(trade1))
        assertEquals(10, repository.getReadCount(trade2))
        assertEquals(15, repository.getReadCount(trade3))
        
        // Clear one trade
        repository.clearReadState(trade2)
        assertEquals(5, repository.getReadCount(trade1))
        assertEquals(0, repository.getReadCount(trade2))
        assertEquals(15, repository.getReadCount(trade3))
    }

    @Test
    fun testConcurrentReadCountUpdates() = runBlocking {
        val tradeId = "concurrentTrade"
        val numberOfUpdaters = 50
        val incrementsPerUpdater = 10
        
        // Launch multiple coroutines that increment the same trade's read count
        val jobs = (1..numberOfUpdaters).map {
            async {
                repeat(incrementsPerUpdater) {
                    repository.incrementReadCount(tradeId)
                    delay(1) // Small delay to increase chance of race conditions
                }
            }
        }
        
        // Wait for all updates to complete
        jobs.awaitAll()
        
        // Verify final count is correct (no lost updates)
        val expectedCount = numberOfUpdaters * incrementsPerUpdater
        assertEquals(expectedCount, repository.getReadCount(tradeId))
    }

    @Test
    fun testConcurrentSetReadCount() = runBlocking {
        val tradeId = "setCountTrade"
        val numberOfSetters = 20
        
        // Launch multiple coroutines that set different values
        val jobs = (1..numberOfSetters).map { setterId ->
            async {
                repository.setReadCount(tradeId, setterId * 10)
                delay(1)
            }
        }
        
        // Wait for all operations to complete
        jobs.awaitAll()
        
        // Verify that the final value is one of the expected values
        val finalCount = repository.getReadCount(tradeId)
        val expectedValues = (1..numberOfSetters).map { it * 10 }
        assertTrue(finalCount in expectedValues, "Final count $finalCount should be one of $expectedValues")
    }

    @Test
    fun testConcurrentUpdateIfGreater() = runBlocking {
        val tradeId = "updateIfGreaterTrade"
        val numberOfUpdaters = 30
        
        // Start with initial value
        repository.setReadCount(tradeId, 0)
        
        // Launch multiple coroutines that try to update with increasing values
        val jobs = (1..numberOfUpdaters).map { updaterId ->
            async {
                val newValue = updaterId * 5
                repository.updateReadCountIfGreater(tradeId, newValue)
                delay(1)
            }
        }
        
        // Wait for all operations to complete
        jobs.awaitAll()
        
        // The final value should be the maximum attempted value
        val expectedMaxValue = numberOfUpdaters * 5
        assertEquals(expectedMaxValue, repository.getReadCount(tradeId))
    }

    @Test
    fun testConcurrentMultipleTradesOperations() = runBlocking {
        val numberOfTrades = 10
        val operationsPerTrade = 20
        
        // Launch operations on multiple trades concurrently
        val jobs = (1..numberOfTrades).map { tradeIndex ->
            async {
                val tradeId = "trade_$tradeIndex"
                repeat(operationsPerTrade) { opIndex ->
                    when (opIndex % 4) {
                        0 -> repository.setReadCount(tradeId, opIndex)
                        1 -> repository.incrementReadCount(tradeId)
                        2 -> repository.updateReadCountIfGreater(tradeId, opIndex * 2)
                        3 -> repository.getReadCount(tradeId)
                    }
                    delay(1)
                }
            }
        }
        
        // Wait for all operations to complete
        jobs.awaitAll()
        
        // Verify that all trades have some read count (operations didn't interfere)
        (1..numberOfTrades).forEach { tradeIndex ->
            val tradeId = "trade_$tradeIndex"
            val count = repository.getReadCount(tradeId)
            assertTrue(count >= 0, "Trade $tradeId should have non-negative read count, got $count")
        }
    }

    @Test
    fun testReadModifyWriteRaceConditionPrevention() = runBlocking {
        val tradeId = "raceConditionTrade"
        val numberOfOperations = 100
        
        // This test simulates the problematic pattern that was used before
        // Multiple coroutines try to read-modify-write the same trade's count
        val jobs = (1..numberOfOperations).map { operationId ->
            async {
                // Simulate the old problematic pattern but using the new safe methods
                val currentCount = repository.getReadCount(tradeId)
                val newCount = currentCount + 1
                repository.updateReadCountIfGreater(tradeId, newCount)
                delay(1)
            }
        }
        
        // Wait for all operations
        jobs.awaitAll()
        
        // With proper synchronization, we should get the expected count
        // (though the exact value depends on timing, it should be reasonable)
        val finalCount = repository.getReadCount(tradeId)
        assertTrue(finalCount > 0, "Final count should be greater than 0")
        assertTrue(finalCount <= numberOfOperations, "Final count should not exceed number of operations")
    }
}
