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
import network.bisq.mobile.domain.data.model.TradeReadStateMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TradeReadStateRepositoryImplTest {

    private val mockDataStore = mockk<DataStore<TradeReadStateMap>>()
    private val repository = TradeReadStateRepositoryImpl(mockDataStore)

    @Test
    fun `data flow should return trade read state map from datastore`() = runTest {
        // Given
        val expectedMap = TradeReadStateMap(
            mapOf(
                "trade1" to 5,
                "trade2" to 10,
                "trade3" to 0
            )
        )
        every { mockDataStore.data } returns flowOf(expectedMap)

        // When
        val result = repository.data.first()

        // Then
        assertEquals(expectedMap, result)
    }

    @Test
    fun `data flow should emit empty map on IOException and log error`() = runTest {
        // Given
        val ioException = IOException("Test IO error")
        every { mockDataStore.data } returns kotlinx.coroutines.flow.flow {
            throw ioException
        }

        // When
        val result = repository.data.first()

        // Then
        assertEquals(TradeReadStateMap(emptyMap()), result)
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
    fun `setCount should add new trade read state`() = runTest {
        // Given
        val updateSlot = slot<suspend (TradeReadStateMap) -> TradeReadStateMap>()
        coEvery { mockDataStore.updateData(capture(updateSlot)) } returns TradeReadStateMap()
        
        val originalMap = TradeReadStateMap(
            mapOf("existing_trade" to 3)
        )
        val tradeId = "new_trade"
        val count = 7

        // When
        repository.setCount(tradeId, count)

        // Then
        coVerify { mockDataStore.updateData(any()) }
        
        val updatedMap = updateSlot.captured(originalMap)
        assertEquals(7, updatedMap.map[tradeId])
        assertEquals(3, updatedMap.map["existing_trade"]) // preserved
        assertEquals(2, updatedMap.map.size)
    }

    @Test
    fun `setCount should update existing trade read state`() = runTest {
        // Given
        val updateSlot = slot<suspend (TradeReadStateMap) -> TradeReadStateMap>()
        coEvery { mockDataStore.updateData(capture(updateSlot)) } returns TradeReadStateMap()
        
        val originalMap = TradeReadStateMap(
            mapOf(
                "trade1" to 5,
                "trade2" to 10
            )
        )
        val tradeId = "trade1"
        val newCount = 15

        // When
        repository.setCount(tradeId, newCount)

        // Then
        coVerify { mockDataStore.updateData(any()) }
        
        val updatedMap = updateSlot.captured(originalMap)
        assertEquals(15, updatedMap.map[tradeId])
        assertEquals(10, updatedMap.map["trade2"]) // preserved
        assertEquals(2, updatedMap.map.size)
    }

    @Test
    fun `setCount should reject blank tradeId`() = runTest {
        // When & Then
        assertFailsWith<IllegalArgumentException> {
            repository.setCount("", 5)
        }
        
        assertFailsWith<IllegalArgumentException> {
            repository.setCount("   ", 5)
        }
    }

    @Test
    fun `setCount should reject negative count`() = runTest {
        // When & Then
        assertFailsWith<IllegalArgumentException> {
            repository.setCount("trade1", -1)
        }
        
        assertFailsWith<IllegalArgumentException> {
            repository.setCount("trade1", -10)
        }
    }

    @Test
    fun `setCount should accept zero count`() = runTest {
        // Given
        val updateSlot = slot<suspend (TradeReadStateMap) -> TradeReadStateMap>()
        coEvery { mockDataStore.updateData(capture(updateSlot)) } returns TradeReadStateMap()
        
        val originalMap = TradeReadStateMap(emptyMap())

        // When
        repository.setCount("trade1", 0)

        // Then
        coVerify { mockDataStore.updateData(any()) }
        
        val updatedMap = updateSlot.captured(originalMap)
        assertEquals(0, updatedMap.map["trade1"])
    }

    @Test
    fun `clearId should remove existing trade from map`() = runTest {
        // Given
        val updateSlot = slot<suspend (TradeReadStateMap) -> TradeReadStateMap>()
        coEvery { mockDataStore.updateData(capture(updateSlot)) } returns TradeReadStateMap()
        
        val originalMap = TradeReadStateMap(
            mapOf(
                "trade1" to 5,
                "trade2" to 10,
                "trade3" to 15
            )
        )
        val tradeIdToRemove = "trade2"

        // When
        repository.clearId(tradeIdToRemove)

        // Then
        coVerify { mockDataStore.updateData(any()) }
        
        val updatedMap = updateSlot.captured(originalMap)
        assertEquals(null, updatedMap.map[tradeIdToRemove])
        assertEquals(5, updatedMap.map["trade1"]) // preserved
        assertEquals(15, updatedMap.map["trade3"]) // preserved
        assertEquals(2, updatedMap.map.size)
    }

    @Test
    fun `clearId should handle non-existing trade gracefully`() = runTest {
        // Given
        val updateSlot = slot<suspend (TradeReadStateMap) -> TradeReadStateMap>()
        coEvery { mockDataStore.updateData(capture(updateSlot)) } returns TradeReadStateMap()
        
        val originalMap = TradeReadStateMap(
            mapOf("trade1" to 5)
        )
        val nonExistingTradeId = "non_existing_trade"

        // When
        repository.clearId(nonExistingTradeId)

        // Then
        coVerify { mockDataStore.updateData(any()) }
        
        val updatedMap = updateSlot.captured(originalMap)
        assertEquals(null, updatedMap.map[nonExistingTradeId])
        assertEquals(5, updatedMap.map["trade1"]) // preserved
        assertEquals(1, updatedMap.map.size)
    }

    @Test
    fun `clearId should reject blank tradeId`() = runTest {
        // When & Then
        assertFailsWith<IllegalArgumentException> {
            repository.clearId("")
        }
        
        assertFailsWith<IllegalArgumentException> {
            repository.clearId("   ")
        }
    }

    @Test
    fun `fetch should return first item from data flow`() = runTest {
        // Given
        val expectedMap = TradeReadStateMap(
            mapOf("fetched_trade" to 42)
        )
        every { mockDataStore.data } returns flowOf(expectedMap)

        // When
        val result = repository.fetch()

        // Then
        assertEquals(expectedMap, result)
    }

    @Test
    fun `multiple operations should work correctly together`() = runTest {
        // Given
        val updateSlots = mutableListOf<suspend (TradeReadStateMap) -> TradeReadStateMap>()
        coEvery { mockDataStore.updateData(capture(updateSlots)) } returns TradeReadStateMap()
        
        val originalMap = TradeReadStateMap(
            mapOf("trade1" to 5)
        )

        // When - perform multiple operations
        repository.setCount("trade2", 10)
        repository.setCount("trade1", 15) // update existing
        repository.clearId("trade2") // remove recently added
        repository.setCount("trade3", 20) // add new

        // Then - verify operations
        assertEquals(4, updateSlots.size)
        
        // Apply operations sequentially
        var currentMap = originalMap
        
        // 1. Add trade2 with count 10
        currentMap = updateSlots[0](currentMap)
        assertEquals(10, currentMap.map["trade2"])
        assertEquals(5, currentMap.map["trade1"])
        
        // 2. Update trade1 to count 15
        currentMap = updateSlots[1](currentMap)
        assertEquals(15, currentMap.map["trade1"])
        assertEquals(10, currentMap.map["trade2"])
        
        // 3. Remove trade2
        currentMap = updateSlots[2](currentMap)
        assertEquals(null, currentMap.map["trade2"]) 
        assertEquals(15, currentMap.map["trade1"]) 
        
        // 4. Add trade3 with count 20
        currentMap = updateSlots[3](currentMap)
        assertEquals(20, currentMap.map["trade3"]) 
        assertEquals(15, currentMap.map["trade1"]) 
        assertEquals(null, currentMap.map["trade2"]) 
        assertEquals(2, currentMap.map.size)
    }
}

