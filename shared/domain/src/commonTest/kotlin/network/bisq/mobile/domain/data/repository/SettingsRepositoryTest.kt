package network.bisq.mobile.domain.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.persistance.KeyValueStorage
import network.bisq.mobile.domain.data.persistance.PersistenceSource
import network.bisq.mobile.domain.di.testModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import org.koin.core.qualifier.named
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail

class SettingsRepositoryTest : KoinTest {

    private lateinit var settingsRepository: SettingsRepository
    private val persistenceSource: PersistenceSource<Settings> by inject(qualifier = named("settingsStorage"))

    @BeforeTest
    fun setup() {
        startKoin {
            modules(testModule)
        }
        settingsRepository = SettingsRepository(persistenceSource as KeyValueStorage<Settings>)
    }

    @AfterTest
    fun teardown() {
        runBlocking {
            settingsRepository.clear()
        }
        stopKoin()
    }

    @Test
    fun testCreateAndFetch() = runBlocking {
        // Create settings
        val settings = createSampleSettings()
        settingsRepository.create(settings)

        // Fetch settings
        val fetchedSettings = settingsRepository.fetch()
        assertNotNull(fetchedSettings)
        assertEquals("https://api.bisq.network", fetchedSettings.bisqApiUrl)
        assertEquals(true, fetchedSettings.firstLaunch)
    }

    @Test
    fun testUpdate() = runBlocking {
        // Create settings
        val settings = createSampleSettings()
        settingsRepository.create(settings)

        // Update settings
        val updatedSettings = settings.apply {
            bisqApiUrl = "https://test-api.bisq.network"
            firstLaunch = false
        }
        settingsRepository.update(updatedSettings)

        // Verify update
        val fetchedSettings = settingsRepository.fetch()
        assertNotNull(fetchedSettings)
        assertEquals("https://test-api.bisq.network", fetchedSettings.bisqApiUrl)
        assertEquals(false, fetchedSettings.firstLaunch)
    }

    @Test
    fun testDelete() = runBlocking {
        // Create settings
        val settings = createSampleSettings()
        settingsRepository.create(settings)

        // Delete settings
        settingsRepository.delete(settings)

        // Verify deletion
        val fetchedSettings = settingsRepository.fetch()
        assertNull(fetchedSettings)
    }

    @Test
    fun testDataFlow() = runBlocking {
        // Create settings
        val settings = createSampleSettings()
        settingsRepository.create(settings)

        // Verify data flow emits the correct value
        val flowValue = settingsRepository.data.first()
        assertNotNull(flowValue)
        assertEquals("https://api.bisq.network", flowValue.bisqApiUrl)
        assertEquals(true, flowValue.firstLaunch)
    }

    @Test
    fun testPersistence() = runBlocking {
        // Create settings with explicit ID
        val settings = createSampleSettings()
        settingsRepository.create(settings)

        // Create a new repository instance with the same prototype
        val newRepository = SettingsRepository(
            persistenceSource as KeyValueStorage<Settings>
        )
        val fetchedSettings = newRepository.fetch()
        
        assertNotNull(fetchedSettings, "Settings should be persisted and accessible by a new repository instance")
        assertEquals("https://api.bisq.network", fetchedSettings.bisqApiUrl)
        assertEquals(true, fetchedSettings.firstLaunch)
    }

    @Test
    fun testClear() = runBlocking {
        // Create settings
        val settings = createSampleSettings()
        settingsRepository.create(settings)

        // Clear repository
        settingsRepository.clear()

        // Verify settings are cleared
        val fetchedSettings = settingsRepository.fetch()
        assertNull(fetchedSettings)
    }

    @Test
    fun testUpdateNonExistentSettings() = runBlocking {
        // Try to update settings that haven't been created yet
        val settings = createSampleSettings()
        settingsRepository.update(settings)

        // Verify the update operation still works
        val fetchedSettings = settingsRepository.fetch()
        assertNotNull(fetchedSettings)
        assertEquals("https://api.bisq.network", fetchedSettings.bisqApiUrl)
    }

    private fun createSampleSettings(): Settings {
        return Settings().apply {
            bisqApiUrl = "https://api.bisq.network"
            firstLaunch = true
            showChatRulesWarnBox = true
        }
    }
}