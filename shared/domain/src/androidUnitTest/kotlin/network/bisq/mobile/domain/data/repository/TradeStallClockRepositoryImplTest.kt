package network.bisq.mobile.data.repository

import androidx.datastore.core.DataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.model.TradeStallClockEntry
import network.bisq.mobile.data.model.TradeStallClockMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TradeStallClockRepositoryImplTest {
    private val mockDataStore = mockk<DataStore<TradeStallClockMap>>()
    private val repository = TradeStallClockRepositoryImpl(mockDataStore)

    @Test
    fun `data flow should return stall clock map from datastore`() =
        runTest {
            val expectedMap =
                TradeStallClockMap(
                    mapOf(
                        "trade1" to TradeStallClockEntry("INIT", 1_000L),
                        "trade2" to TradeStallClockEntry("BTC_CONFIRMED", null),
                    ),
                )
            every { mockDataStore.data } returns flowOf(expectedMap)

            assertEquals(expectedMap, repository.data.first())
        }

    // Exercises createDefault() via the base class's IOException fallback, like the sibling
    // TradeReadStateRepositoryImplTest does: a corrupt/unreadable store degrades to an empty map,
    // which the tracker reads as honest UNKNOWN rather than failing.
    @Test
    fun `data flow should emit empty map on IOException`() =
        runTest {
            every { mockDataStore.data } returns
                kotlinx.coroutines.flow.flow {
                    throw androidx.datastore.core.IOException("Test IO error")
                }

            assertEquals(TradeStallClockMap(emptyMap()), repository.data.first())
        }

    @Test
    fun `fetch should return first item from data flow`() =
        runTest {
            val expectedMap = TradeStallClockMap(mapOf("trade1" to TradeStallClockEntry("INIT", 42L)))
            every { mockDataStore.data } returns flowOf(expectedMap)

            assertEquals(expectedMap, repository.fetch())
        }

    @Test
    fun `record should add a new entry and preserve existing ones`() =
        runTest {
            val updateSlot = slot<suspend (TradeStallClockMap) -> TradeStallClockMap>()
            coEvery { mockDataStore.updateData(capture(updateSlot)) } returns TradeStallClockMap()

            val originalMap = TradeStallClockMap(mapOf("existing" to TradeStallClockEntry("INIT", 5L)))

            repository.record("new_trade", "TAKER_SENT_TAKE_OFFER_REQUEST", 99L)

            coVerify { mockDataStore.updateData(any()) }
            val updatedMap = updateSlot.captured(originalMap)
            assertEquals(TradeStallClockEntry("TAKER_SENT_TAKE_OFFER_REQUEST", 99L), updatedMap.map["new_trade"])
            assertEquals(TradeStallClockEntry("INIT", 5L), updatedMap.map["existing"]) // preserved
            assertEquals(2, updatedMap.map.size)
        }

    @Test
    fun `record should overwrite an existing entry`() =
        runTest {
            val updateSlot = slot<suspend (TradeStallClockMap) -> TradeStallClockMap>()
            coEvery { mockDataStore.updateData(capture(updateSlot)) } returns TradeStallClockMap()

            val originalMap = TradeStallClockMap(mapOf("trade1" to TradeStallClockEntry("INIT", 5L)))

            repository.record("trade1", "BTC_CONFIRMED", 100L)

            val updatedMap = updateSlot.captured(originalMap)
            assertEquals(TradeStallClockEntry("BTC_CONFIRMED", 100L), updatedMap.map["trade1"])
            assertEquals(1, updatedMap.map.size)
        }

    // A first sighting has no witnessed transition, so its time is null by contract.
    @Test
    fun `record should accept a null transition time`() =
        runTest {
            val updateSlot = slot<suspend (TradeStallClockMap) -> TradeStallClockMap>()
            coEvery { mockDataStore.updateData(capture(updateSlot)) } returns TradeStallClockMap()

            repository.record("trade1", "INIT", null)

            val updatedMap = updateSlot.captured(TradeStallClockMap())
            assertEquals(TradeStallClockEntry("INIT", null), updatedMap.map["trade1"])
        }

    @Test
    fun `record should reject blank tradeId`() =
        runTest {
            assertFailsWith<IllegalArgumentException> { repository.record("", "INIT", 1L) }
            assertFailsWith<IllegalArgumentException> { repository.record("   ", "INIT", 1L) }
        }

    @Test
    fun `retainAll should keep only the given trade ids`() =
        runTest {
            val updateSlot = slot<suspend (TradeStallClockMap) -> TradeStallClockMap>()
            coEvery { mockDataStore.updateData(capture(updateSlot)) } returns TradeStallClockMap()

            val originalMap =
                TradeStallClockMap(
                    mapOf(
                        "open1" to TradeStallClockEntry("INIT", 1L),
                        "closed" to TradeStallClockEntry("BTC_CONFIRMED", 2L),
                        "open2" to TradeStallClockEntry("INIT", null),
                    ),
                )

            repository.retainAll(setOf("open1", "open2"))

            val updatedMap = updateSlot.captured(originalMap)
            assertEquals(setOf("open1", "open2"), updatedMap.map.keys)
            assertEquals(TradeStallClockEntry("INIT", 1L), updatedMap.map["open1"])
            assertEquals(TradeStallClockEntry("INIT", null), updatedMap.map["open2"])
        }

    @Test
    fun `retainAll with an empty set should clear the map`() =
        runTest {
            val updateSlot = slot<suspend (TradeStallClockMap) -> TradeStallClockMap>()
            coEvery { mockDataStore.updateData(capture(updateSlot)) } returns TradeStallClockMap()

            val originalMap = TradeStallClockMap(mapOf("trade1" to TradeStallClockEntry("INIT", 1L)))

            repository.retainAll(emptySet())

            val updatedMap = updateSlot.captured(originalMap)
            assertEquals(emptyMap(), updatedMap.map)
        }
}
