import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import network.bisq.mobile.domain.data.model.BaseModel
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.persistance.PersistenceSource
import network.bisq.mobile.domain.di.testModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KeyValueStorageTest : KoinTest {

    private val persistenceSource: PersistenceSource<BaseModel> by inject()

    @BeforeTest
    fun setup() {
        startKoin {
            modules(testModule)
        }
    }

    @AfterTest
    fun teardown() {
        stopKoin()
    }

    @Test
    fun testSaveAndGet() = runBlocking {
        val item = Settings().apply { id = "1" }
        persistenceSource.save(item)

        val testModel = Settings().apply { id = "1" }
        val retrievedItem = persistenceSource.get(testModel)
        assertEquals(item, retrievedItem)
    }

    @Test
    fun testSaveAllAndGetAll() = runBlocking {
        val items = listOf(Settings().apply { id = "1" }, Settings().apply { id = "2" })
        persistenceSource.saveAll(items)

        val testModel = Settings()
        val retrievedItems = persistenceSource.getAll(testModel)
        assertEquals(items, retrievedItems)
    }

    @Test
    fun testDelete() = runBlocking {
        val item = Settings().apply { id = "1" }
        persistenceSource.save(item)

        val testModel = Settings().apply { id = "1" }
        persistenceSource.delete(item)
        val retrievedItem = persistenceSource.get(testModel)
        assertEquals(null, retrievedItem)
    }

    @Test
    fun testClear() = runBlocking {
        val items = listOf(Settings().apply { id = "1" }, Settings().apply { id = "2" })
        persistenceSource.saveAll(items)

        val testModel = Settings()
        persistenceSource.clear()
        val retrievedItems = persistenceSource.getAll(testModel)
        assertEquals(emptyList<BaseModel>(), retrievedItems)
    }

    @Test
    fun testDuplicateIds() = runBlocking {
        val item1 = Settings().apply { id = "1" }
        val item2 = Settings().apply { id = "1" }

        persistenceSource.save(item1)
        persistenceSource.save(item2)

        val testModel = Settings().apply { id = "1" }
        val retrievedItem = persistenceSource.get(testModel)
        assertEquals(item2, retrievedItem)
    }

    @Test
    fun testNonExistentId() = runBlocking {
        val testModel = Settings().apply { id = "999" }
        val retrievedItem = persistenceSource.get(testModel)
        assertEquals(null, retrievedItem)
    }

    @Test
    fun testKeyOverlap() = runBlocking {
        val item1 = Settings().apply { id = "1" }
        val item2 = Settings().apply { id = "10" }

        persistenceSource.save(item1)
        persistenceSource.save(item2)

        val testModel = Settings().apply { id = "1" }
        val retrievedItem1 = persistenceSource.get(testModel)
        val retrievedItem2 = persistenceSource.get(testModel.apply { id = "10" })

        assertEquals(item1, retrievedItem1)
        assertEquals(item2, retrievedItem2)
    }

    @Test
    fun testClearSpecificKey() = runBlocking {
        val item1 = Settings().apply { id = "1" }
        val item2 = Settings().apply { id = "2" }

        persistenceSource.saveAll(listOf(item1, item2))
        persistenceSource.delete(item1)

        val testModel = Settings()
        val allItems = persistenceSource.getAll(testModel)
        assertEquals(listOf(item2), allItems)
    }

    @Test
    fun testConcurrentSave() = runBlocking {
        val numberOfCoroutines = 50
        val itemsPerCoroutine = 10

        // Launch multiple coroutines that save items concurrently
        val jobs = (1..numberOfCoroutines).map { coroutineId ->
            async {
                repeat(itemsPerCoroutine) { itemId ->
                    val uniqueId = "${coroutineId}_$itemId"
                    val item = Settings().apply { id = uniqueId }
                    persistenceSource.save(item)
                }
            }
        }

        // Wait for all coroutines to complete
        jobs.awaitAll()

        // Verify all items were saved correctly
        val testModel = Settings()
        val allItems = persistenceSource.getAll(testModel)
        assertEquals(numberOfCoroutines * itemsPerCoroutine, allItems.size)

        // Verify no data corruption - each item should have correct ID
        allItems.forEach { item ->
            assertTrue(item.id.matches(Regex("\\d+_\\d+")), "Item ID should match pattern: ${item.id}")
        }
    }

    @Test
    fun testConcurrentReadWrite() = runBlocking {
        val numberOfReaders = 20
        val numberOfWriters = 10
        val itemsPerWriter = 5

        // Pre-populate with some data
        val initialItems = (1..10).map { Settings().apply { id = "initial_$it" } }
        persistenceSource.saveAll(initialItems)

        // Launch concurrent readers and writers
        val readerJobs = (1..numberOfReaders).map {
            async {
                repeat(10) {
                    val testModel = Settings()
                    persistenceSource.getAll(testModel)
                    delay(1) // Small delay to increase chance of race conditions
                }
            }
        }

        val writerJobs = (1..numberOfWriters).map { writerId ->
            async {
                repeat(itemsPerWriter) { itemId ->
                    val uniqueId = "writer_${writerId}_$itemId"
                    val item = Settings().apply { id = uniqueId }
                    persistenceSource.save(item)
                    delay(1) // Small delay to increase chance of race conditions
                }
            }
        }

        // Wait for all operations to complete
        (readerJobs + writerJobs).awaitAll()

        // Verify final state
        val testModel = Settings()
        val finalItems = persistenceSource.getAll(testModel)
        val expectedCount = initialItems.size + (numberOfWriters * itemsPerWriter)
        assertEquals(expectedCount, finalItems.size)
    }

    @Test
    fun testConcurrentDeleteAndRead() = runBlocking {
        // Pre-populate with data
        val initialItems = (1..20).map { Settings().apply { id = "item_$it" } }
        persistenceSource.saveAll(initialItems)

        val numberOfReaders = 15
        val numberOfDeleters = 5

        // Launch concurrent readers
        val readerJobs = (1..numberOfReaders).map {
            async {
                repeat(5) {
                    val testModel = Settings()
                    val items = persistenceSource.getAll(testModel)
                    // Verify no null or corrupted items
                    items.forEach { item ->
                        assertTrue(item.id.isNotBlank(), "Item ID should not be blank")
                    }
                    delay(1)
                }
            }
        }

        // Launch concurrent deleters
        val deleterJobs = (1..numberOfDeleters).map { deleterId ->
            async {
                val itemsToDelete = initialItems.filter { it.id.endsWith("${deleterId}") || it.id.endsWith("${deleterId + 10}") }
                itemsToDelete.forEach { item ->
                    persistenceSource.delete(item)
                    delay(2)
                }
            }
        }

        // Wait for all operations
        (readerJobs + deleterJobs).awaitAll()

        // Verify consistency - no exceptions should have been thrown
        val testModel = Settings()
        val remainingItems = persistenceSource.getAll(testModel)
        assertTrue(remainingItems.size <= initialItems.size, "Should not have more items than initially")
    }

    @Test
    fun testReadModifyWriteRaceCondition() = runBlocking {
        // This test simulates the TradeReadState update pattern
        val baseItem = Settings().apply { id = "shared_item" }
        persistenceSource.save(baseItem)

        val numberOfUpdaters = 20
        val updatesPerUpdater = 5

        // Each updater tries to read-modify-write the same item
        val updaterJobs = (1..numberOfUpdaters).map { updaterId ->
            async {
                repeat(updatesPerUpdater) { updateId ->
                    // Simulate the read-modify-write pattern used in TradeReadState
                    val current = persistenceSource.get(Settings().apply { id = "shared_item" })
                    if (current != null) {
                        // Modify the item (in real code this would be updating the map)
                        val updated = Settings().apply {
                            id = "shared_item"
                            // In real TradeReadState, this would be map modifications
                        }
                        persistenceSource.save(updated)
                    }
                    delay(1) // Increase chance of race condition
                }
            }
        }

        // Wait for all updates
        updaterJobs.awaitAll()

        // Verify the item still exists and is not corrupted
        val finalItem = persistenceSource.get(Settings().apply { id = "shared_item" })
        assertTrue(finalItem != null, "Item should still exist after concurrent updates")
        assertEquals("shared_item", finalItem?.id)
    }
}
