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
}
