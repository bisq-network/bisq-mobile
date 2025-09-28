package network.bisq.mobile.domain.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.domain.data.model.Settings
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRepositoryImplTest {

    private val mockDataStore = mockk<DataStore<Settings>>()
    private val repository = SettingsRepositoryImpl(mockDataStore)

    @Test
    fun `data flow should return settings data from datastore`() = runTest {
        // Given
        val expectedSettings = Settings(
            bisqApiUrl = "https://api.bisq.network",
            firstLaunch = false,
            showChatRulesWarnBox = false,
            selectedMarketCode = "BTC/EUR"
        )
        every { mockDataStore.data } returns flowOf(expectedSettings)

        // When
        val result = repository.data.first()

        // Then
        assertEquals(expectedSettings, result)
    }

    @Test
    fun `data flow should emit default settings on IOException and log error`() = runTest {
        // Given
        val ioException = IOException("Test IO error")
        every { mockDataStore.data } returns kotlinx.coroutines.flow.flow {
            throw ioException
        }

        // When
        val result = repository.data.first()

        // Then
        assertEquals(Settings(), result)
    }

    @Test
    fun `data flow should rethrow non-IOException`() = runTest {
        // Given
        val runtimeException = RuntimeException("Test runtime error")
        every { mockDataStore.data } returns kotlinx.coroutines.flow.flow {
            throw runtimeException
        }

        // When & Then
        try {
            repository.data.first()
            kotlin.test.fail("Expected exception to be thrown")
        } catch (e: RuntimeException) {
            assertEquals("Test runtime error", e.message)
        }
    }

    @Test
    fun `setBisqApiUrl should update bisq API URL`() = runTest {
        // Given
        val updateSlot = slot<suspend (Settings) -> Settings>()
        coEvery { mockDataStore.updateData(capture(updateSlot)) } returns Settings()
        
        val originalSettings = Settings(
            bisqApiUrl = "old-url",
            firstLaunch = false,
            selectedMarketCode = "BTC/EUR"
        )
        val newUrl = "https://new-api.bisq.network"

        // When
        repository.setBisqApiUrl(newUrl)

        // Then
        coVerify { mockDataStore.updateData(any()) }
        
        val updatedSettings = updateSlot.captured(originalSettings)
        assertEquals(newUrl, updatedSettings.bisqApiUrl)
        // Verify other fields are preserved
        assertEquals(false, updatedSettings.firstLaunch)
        assertEquals("BTC/EUR", updatedSettings.selectedMarketCode)
    }

    @Test
    fun `setFirstLaunch should update first launch flag`() = runTest {
        // Given
        val updateSlot = slot<suspend (Settings) -> Settings>()
        coEvery { mockDataStore.updateData(capture(updateSlot)) } returns Settings()
        
        val originalSettings = Settings(
            bisqApiUrl = "https://api.bisq.network",
            firstLaunch = true,
            selectedMarketCode = "BTC/USD"
        )

        // When
        repository.setFirstLaunch(false)

        // Then
        coVerify { mockDataStore.updateData(any()) }
        
        val updatedSettings = updateSlot.captured(originalSettings)
        assertEquals(false, updatedSettings.firstLaunch)
        // Verify other fields are preserved
        assertEquals("https://api.bisq.network", updatedSettings.bisqApiUrl)
        assertEquals("BTC/USD", updatedSettings.selectedMarketCode)
    }

    @Test
    fun `setShowChatRulesWarnBox should update chat rules warning flag`() = runTest {
        // Given
        val updateSlot = slot<suspend (Settings) -> Settings>()
        coEvery { mockDataStore.updateData(capture(updateSlot)) } returns Settings()
        
        val originalSettings = Settings(
            bisqApiUrl = "https://api.bisq.network",
            showChatRulesWarnBox = true,
            selectedMarketCode = "BTC/GBP"
        )

        // When
        repository.setShowChatRulesWarnBox(false)

        // Then
        coVerify { mockDataStore.updateData(any()) }
        
        val updatedSettings = updateSlot.captured(originalSettings)
        assertEquals(false, updatedSettings.showChatRulesWarnBox)
        // Verify other fields are preserved
        assertEquals("https://api.bisq.network", updatedSettings.bisqApiUrl)
        assertEquals("BTC/GBP", updatedSettings.selectedMarketCode)
    }

    @Test
    fun `setSelectedMarketCode should update selected market code`() = runTest {
        // Given
        val updateSlot = slot<suspend (Settings) -> Settings>()
        coEvery { mockDataStore.updateData(capture(updateSlot)) } returns Settings()
        
        val originalSettings = Settings(
            bisqApiUrl = "https://api.bisq.network",
            firstLaunch = false,
            selectedMarketCode = "BTC/USD"
        )
        val newMarketCode = "BTC/JPY"

        // When
        repository.setSelectedMarketCode(newMarketCode)

        // Then
        coVerify { mockDataStore.updateData(any()) }
        
        val updatedSettings = updateSlot.captured(originalSettings)
        assertEquals(newMarketCode, updatedSettings.selectedMarketCode)
        // Verify other fields are preserved
        assertEquals("https://api.bisq.network", updatedSettings.bisqApiUrl)
        assertEquals(false, updatedSettings.firstLaunch)
    }

    @Test
    fun `clear should reset settings to default`() = runTest {
        // Given
        val updateSlot = slot<suspend (Settings) -> Settings>()
        coEvery { mockDataStore.updateData(capture(updateSlot)) } returns Settings()
        
        val originalSettings = Settings(
            bisqApiUrl = "https://custom-api.bisq.network",
            firstLaunch = false,
            showChatRulesWarnBox = false,
            selectedMarketCode = "BTC/EUR"
        )

        // When
        repository.clear()

        // Then
        coVerify { mockDataStore.updateData(any()) }
        
        val updatedSettings = updateSlot.captured(originalSettings)
        assertEquals(Settings(), updatedSettings)
    }

    @Test
    fun `fetch should return first item from data flow`() = runTest {
        // Given
        val expectedSettings = Settings(
            bisqApiUrl = "https://fetched-api.bisq.network",
            firstLaunch = false
        )
        every { mockDataStore.data } returns flowOf(expectedSettings)

        // When
        val result = repository.fetch()

        // Then
        assertEquals(expectedSettings, result)
    }

    @Test
    fun `multiple updates should preserve unmodified fields`() = runTest {
        // Given
        val updateSlots = mutableListOf<suspend (Settings) -> Settings>()
        coEvery { mockDataStore.updateData(capture(updateSlots)) } returns Settings()
        
        val originalSettings = Settings(
            bisqApiUrl = "https://original.bisq.network",
            firstLaunch = true,
            showChatRulesWarnBox = true,
            selectedMarketCode = "BTC/USD"
        )

        // When - perform multiple updates
        repository.setBisqApiUrl("https://new.bisq.network")
        repository.setFirstLaunch(false)
        repository.setSelectedMarketCode("BTC/EUR")

        // Then - verify each update preserves other fields
        assertEquals(3, updateSlots.size)
        
        // First update: setBisqApiUrl
        val afterUrlUpdate = updateSlots[0](originalSettings)
        assertEquals("https://new.bisq.network", afterUrlUpdate.bisqApiUrl)
        assertEquals(true, afterUrlUpdate.firstLaunch) // preserved
        assertEquals(true, afterUrlUpdate.showChatRulesWarnBox) // preserved
        assertEquals("BTC/USD", afterUrlUpdate.selectedMarketCode) // preserved
        
        // Second update: setFirstLaunch
        val afterFirstLaunchUpdate = updateSlots[1](afterUrlUpdate)
        assertEquals("https://new.bisq.network", afterFirstLaunchUpdate.bisqApiUrl) // preserved
        assertEquals(false, afterFirstLaunchUpdate.firstLaunch)
        assertEquals(true, afterFirstLaunchUpdate.showChatRulesWarnBox) // preserved
        assertEquals("BTC/USD", afterFirstLaunchUpdate.selectedMarketCode) // preserved
        
        // Third update: setSelectedMarketCode
        val afterMarketUpdate = updateSlots[2](afterFirstLaunchUpdate)
        assertEquals("https://new.bisq.network", afterMarketUpdate.bisqApiUrl) // preserved
        assertEquals(false, afterMarketUpdate.firstLaunch) // preserved
        assertEquals(true, afterMarketUpdate.showChatRulesWarnBox) // preserved
        assertEquals("BTC/EUR", afterMarketUpdate.selectedMarketCode)
    }
}

