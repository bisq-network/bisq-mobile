package network.bisq.mobile.data.repository

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
import network.bisq.mobile.data.model.BatteryOptimizationState
import network.bisq.mobile.data.model.PermissionState
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.data.model.market.MarketFilter
import network.bisq.mobile.data.model.market.MarketSortBy
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRepositoryImplTest {
    private val mockDataStore = mockk<DataStore<Settings>>()
    private val repository = SettingsRepositoryImpl(mockDataStore)

    @Test
    fun `data flow should return settings data from datastore`() =
        runTest {
            // Given
            val expectedSettings =
                Settings(
                    firstLaunch = false,
                    showChatRulesWarnBox = false,
                    selectedMarketCode = "BTC/EUR",
                )
            every { mockDataStore.data } returns flowOf(expectedSettings)

            // When
            val result = repository.data.first()

            // Then
            assertEquals(expectedSettings, result)
        }

    @Test
    fun `data flow should emit default settings on IOException and log error`() =
        runTest {
            // Given
            val ioException = IOException("Test IO error")
            every { mockDataStore.data } returns
                kotlinx.coroutines.flow.flow {
                    throw ioException
                }

            // When
            val result = repository.data.first()

            // Then
            assertEquals(Settings(), result)
        }

    @Test
    fun `data flow should rethrow non-IOException`() =
        runTest {
            // Given
            val runtimeException = RuntimeException("Test runtime error")
            every { mockDataStore.data } returns
                kotlinx.coroutines.flow.flow {
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
    fun `setFirstLaunch should update first launch flag`() =
        runTest {
            // Given
            val updateSlot = slot<suspend (Settings) -> Settings>()
            coEvery { mockDataStore.updateData(capture(updateSlot)) } returns Settings()

            val originalSettings =
                Settings(
                    firstLaunch = true,
                    selectedMarketCode = "BTC/USD",
                )

            // When
            repository.setFirstLaunch(false)

            // Then
            coVerify { mockDataStore.updateData(any()) }

            val updatedSettings = updateSlot.captured(originalSettings)
            assertEquals(false, updatedSettings.firstLaunch)
            // Verify other fields are preserved
            assertEquals("BTC/USD", updatedSettings.selectedMarketCode)
        }

    @Test
    fun `setShowChatRulesWarnBox should update chat rules warning flag`() =
        runTest {
            // Given
            val updateSlot = slot<suspend (Settings) -> Settings>()
            coEvery { mockDataStore.updateData(capture(updateSlot)) } returns Settings()

            val originalSettings =
                Settings(
                    showChatRulesWarnBox = true,
                    selectedMarketCode = "BTC/GBP",
                )

            // When
            repository.setShowChatRulesWarnBox(false)

            // Then
            coVerify { mockDataStore.updateData(any()) }

            val updatedSettings = updateSlot.captured(originalSettings)
            assertEquals(false, updatedSettings.showChatRulesWarnBox)
            // Verify other fields are preserved
            assertEquals("BTC/GBP", updatedSettings.selectedMarketCode)
        }

    @Test
    fun `setSelectedMarketCode should update selected market code`() =
        runTest {
            // Given
            val updateSlot = slot<suspend (Settings) -> Settings>()
            coEvery { mockDataStore.updateData(capture(updateSlot)) } returns Settings()

            val originalSettings =
                Settings(
                    firstLaunch = false,
                    selectedMarketCode = "BTC/USD",
                )
            val newMarketCode = "BTC/JPY"

            // When
            repository.setSelectedMarketCode(newMarketCode)

            // Then
            coVerify { mockDataStore.updateData(any()) }

            val updatedSettings = updateSlot.captured(originalSettings)
            assertEquals(newMarketCode, updatedSettings.selectedMarketCode)
            // Verify other fields are preserved
            assertEquals(false, updatedSettings.firstLaunch)
        }

    @Test
    fun `clear should reset settings to default`() =
        runTest {
            // Given
            val updateSlot = slot<suspend (Settings) -> Settings>()
            coEvery { mockDataStore.updateData(capture(updateSlot)) } returns Settings()

            val originalSettings =
                Settings(
                    firstLaunch = false,
                    showChatRulesWarnBox = false,
                    selectedMarketCode = "BTC/EUR",
                )

            // When
            repository.clear()

            // Then
            coVerify { mockDataStore.updateData(any()) }

            val updatedSettings = updateSlot.captured(originalSettings)
            assertEquals(Settings(), updatedSettings)
        }

    @Test
    fun `fetch should return first item from data flow`() =
        runTest {
            // Given
            val expectedSettings =
                Settings(
                    firstLaunch = false,
                )
            every { mockDataStore.data } returns flowOf(expectedSettings)

            // When
            val result = repository.fetch()

            // Then
            assertEquals(expectedSettings, result)
        }

    @Test
    fun `multiple updates should preserve unmodified fields`() =
        runTest {
            // Given
            val updateSlots = mutableListOf<suspend (Settings) -> Settings>()
            coEvery { mockDataStore.updateData(capture(updateSlots)) } returns Settings()

            val originalSettings =
                Settings(
                    firstLaunch = true,
                    showChatRulesWarnBox = true,
                    selectedMarketCode = "BTC/USD",
                )

            // When - perform multiple updates
            repository.setFirstLaunch(false)
            repository.setSelectedMarketCode("BTC/EUR")

            // Then - verify each update preserves other fields
            assertEquals(2, updateSlots.size)

            // first update: setFirstLaunch
            val afterFirstLaunchUpdate = updateSlots[0](originalSettings)
            assertEquals(false, afterFirstLaunchUpdate.firstLaunch)
            assertEquals(true, afterFirstLaunchUpdate.showChatRulesWarnBox) // preserved
            assertEquals("BTC/USD", afterFirstLaunchUpdate.selectedMarketCode) // preserved

            // second update: setSelectedMarketCode
            val afterMarketUpdate = updateSlots[1](afterFirstLaunchUpdate)
            assertEquals(false, afterMarketUpdate.firstLaunch) // preserved
            assertEquals(true, afterMarketUpdate.showChatRulesWarnBox) // preserved
            assertEquals("BTC/EUR", afterMarketUpdate.selectedMarketCode)
        }

    // ============ Opt-in analytics (issue #525) ============
    //
    // These pins matter because the DI module's runtimeOptInProvider reads
    // these settings on every analytics emit. If the persistence path
    // regresses, the SDK silently keeps using the stale value — which would
    // either suppress events the user enabled (annoying) or worse, emit when
    // they didn't (privacy contract violation).

    @Test
    fun `setAnalyticsEnabled should update analyticsEnabled flag without touching other fields`() =
        runTest {
            val updateSlot = slot<suspend (Settings) -> Settings>()
            coEvery { mockDataStore.updateData(capture(updateSlot)) } returns Settings()

            val originalSettings =
                Settings(
                    firstLaunch = false,
                    analyticsEnabled = false,
                    analyticsPromptSeen = true,
                    selectedMarketCode = "BTC/EUR",
                )

            repository.setAnalyticsEnabled(true)

            coVerify { mockDataStore.updateData(any()) }
            val updated = updateSlot.captured(originalSettings)
            assertEquals(true, updated.analyticsEnabled)
            // Sibling fields preserved
            assertEquals(true, updated.analyticsPromptSeen)
            assertEquals(false, updated.firstLaunch)
            assertEquals("BTC/EUR", updated.selectedMarketCode)
        }

    @Test
    fun `setAnalyticsPromptSeen should update analyticsPromptSeen flag without touching analyticsEnabled`() =
        runTest {
            // Critical: the welcome carousel marks the prompt as seen via
            // "Don't ask again" WITHOUT enabling analytics. If this setter
            // ever flipped analyticsEnabled as a side effect, a user who
            // declined the prompt would start emitting events anyway.
            val updateSlot = slot<suspend (Settings) -> Settings>()
            coEvery { mockDataStore.updateData(capture(updateSlot)) } returns Settings()

            val originalSettings =
                Settings(
                    analyticsEnabled = false,
                    analyticsPromptSeen = false,
                )

            repository.setAnalyticsPromptSeen(true)

            val updated = updateSlot.captured(originalSettings)
            assertEquals(true, updated.analyticsPromptSeen)
            assertEquals(false, updated.analyticsEnabled, "promptSeen flip must NOT enable analytics — privacy contract")
        }

    @Test
    fun `remaining setters each delegate to datastore updateData`() =
        runTest {
            coEvery { mockDataStore.updateData(any()) } returns Settings()

            repository.setSelectedMarketCode("BTC/USD")
            repository.setNotificationPermissionState(PermissionState.entries.first())
            repository.setBatteryOptimizationPermissionState(BatteryOptimizationState.entries.first())
            repository.setMarketSortBy(MarketSortBy.entries.first())
            repository.setMarketFilter(MarketFilter.entries.first())
            repository.setDontShowAgainHyperlinksOpenInBrowser(true)
            repository.setPermitOpeningBrowser(true)
            repository.setAnalyticsBaselineSent(true)
            repository.setRememberOfferbookFilterPreferences(true)
            repository.update { it }

            coVerify(exactly = 10) { mockDataStore.updateData(any()) }
        }
}
