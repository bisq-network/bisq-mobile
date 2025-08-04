package network.bisq.mobile.domain.service.market_price

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.persistance.KeyValueStorage
import network.bisq.mobile.domain.data.persistance.PersistenceSource
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.repository.SettingsRepository
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
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GlobalPriceUpdateTest : KoinTest {

    private lateinit var settingsRepository: SettingsRepository
    private val persistenceSource: PersistenceSource<Settings> by inject(qualifier = named("settingsStorage"))

    @BeforeTest
    fun setup() {
        startKoin {
            modules(testModule)
        }
        settingsRepository = SettingsRepository(persistenceSource as KeyValueStorage<Settings>)
        runBlocking { settingsRepository.create(Settings()) }
    }

    @AfterTest
    fun teardown() {
        runBlocking {
            settingsRepository.clear()
        }
        stopKoin()
    }

    // Test implementation of MarketPriceServiceFacade
    private inner class TestMarketPriceServiceFacade : MarketPriceServiceFacade(settingsRepository) {

        override fun findMarketPriceItem(marketVO: MarketVO): MarketPriceItem? = null
        override fun findUSDMarketPriceItem(): MarketPriceItem? = null
        override fun refreshSelectedFormattedMarketPrice() {}
        override fun selectMarket(marketListItem: MarketListItem) {}

        // Expose protected method for testing
        fun testTriggerGlobalPriceUpdate() {
            triggerGlobalPriceUpdate()
        }
    }

    @Test
    fun `globalPriceUpdate should emit initial value of 0`() = runTest {
        // Given
        val service = TestMarketPriceServiceFacade()

        // When
        val initialValue = service.globalPriceUpdate.value

        // Then
        assertEquals(0L, initialValue)
    }

    @Test
    fun `triggerGlobalPriceUpdate should emit new timestamp`() = runTest {
        val service = TestMarketPriceServiceFacade()
        val initialValue = service.globalPriceUpdate.value

        service.testTriggerGlobalPriceUpdate()

        val newValue = service.globalPriceUpdate.value
        assertNotEquals(initialValue, newValue)
        assertTrue(newValue > 0L, "New timestamp should be greater than 0")
    }

    @Test
    fun `triggerGlobalPriceUpdate should emit different timestamps on multiple calls`() = runBlocking {
        val service = TestMarketPriceServiceFacade()

        service.testTriggerGlobalPriceUpdate()
        val firstTimestamp = service.globalPriceUpdate.value

        // Use delay to ensure different timestamps at millisecond level
        delay(2)

        service.testTriggerGlobalPriceUpdate()
        val secondTimestamp = service.globalPriceUpdate.value

        // Then
        assertNotEquals(firstTimestamp, secondTimestamp)
        assertTrue(secondTimestamp > firstTimestamp, "Second timestamp should be later than first")
    }

    @Test
    fun `globalPriceUpdate flow should emit updates`() = runBlocking {
        // Given
        val service = TestMarketPriceServiceFacade()
        val updates = mutableListOf<Long>()
        
        // Collect first few emissions
        val job = launch {
            service.globalPriceUpdate.collect { timestamp ->
                updates.add(timestamp)
                if (updates.size >= 3) {
                    return@collect
                }
            }
        }

        // When
        delay(10) // Let initial value be collected
        service.testTriggerGlobalPriceUpdate()
        delay(10)
        service.testTriggerGlobalPriceUpdate()
        delay(10)
        
        job.cancel()

        // Then
        assertEquals(3, updates.size)
        assertEquals(0L, updates[0]) // Initial value
        assertTrue(updates[1] > 0L, "First update should have timestamp")
        assertTrue(updates[2] > updates[1], "Second update should be later")
    }

    @Test
    fun `multiple services should have independent global update flows`() = runTest {
        val service1 = TestMarketPriceServiceFacade()
        val service2 = TestMarketPriceServiceFacade()

        service1.testTriggerGlobalPriceUpdate()
        val timestamp1 = service1.globalPriceUpdate.value
        val timestamp2Before = service2.globalPriceUpdate.value

        service2.testTriggerGlobalPriceUpdate()
        val timestamp2After = service2.globalPriceUpdate.value

        assertEquals(0L, timestamp2Before) // Service2 should still have initial value
        assertTrue(timestamp1 > 0L, "Service1 should have updated timestamp")
        assertTrue(timestamp2After > 0L, "Service2 should have updated timestamp")
        assertNotEquals(timestamp1, timestamp2After, "Services should have independent timestamps")
    }

    @Test
    fun `globalPriceUpdate should use system time`() = runTest {
        // Given
        val service = TestMarketPriceServiceFacade()
        val beforeTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

        // When
        service.testTriggerGlobalPriceUpdate()
        val timestamp = service.globalPriceUpdate.value
        val afterTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

        // Then
        assertTrue(timestamp >= beforeTime, "Timestamp should be at or after the before time")
        assertTrue(timestamp <= afterTime, "Timestamp should be at or before the after time")
    }

    @Test
    fun `globalPriceUpdate should be observable by multiple collectors`() = runBlocking {
        // Given
        val service = TestMarketPriceServiceFacade()
        val collector1Updates = mutableListOf<Long>()
        val collector2Updates = mutableListOf<Long>()

        // Start collecting from two different collectors
        val job1 = launch {
            service.globalPriceUpdate.collect { timestamp ->
                collector1Updates.add(timestamp)
                if (collector1Updates.size >= 2) return@collect
            }
        }

        val job2 = launch {
            service.globalPriceUpdate.collect { timestamp ->
                collector2Updates.add(timestamp)
                if (collector2Updates.size >= 2) return@collect
            }
        }

        // When
        delay(10) // Let initial values be collected
        service.testTriggerGlobalPriceUpdate()
        delay(10)

        job1.cancel()
        job2.cancel()

        // Then
        assertEquals(2, collector1Updates.size)
        assertEquals(2, collector2Updates.size)
        assertEquals(collector1Updates, collector2Updates, "Both collectors should receive same updates")
    }
}
