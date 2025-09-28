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
import kotlinx.datetime.Clock
import network.bisq.mobile.domain.data.model.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UserRepositoryImplTest {

    private val mockDataStore = mockk<DataStore<User>>()
    private val repository = UserRepositoryImpl(mockDataStore)

    @Test
    fun `data flow should return user data from datastore`() = runTest {
        // Given
        val expectedUser = User(
            tradeTerms = "Test terms",
            statement = "Test statement",
        )
        every { mockDataStore.data } returns flowOf(expectedUser)

        // When
        val result = repository.data.first()

        // Then
        assertEquals(expectedUser, result)
    }

    @Test
    fun `data flow should emit default user on IOException and log error`() = runTest {
        // Given
        val ioException = IOException("Test IO error")
        every { mockDataStore.data } returns kotlinx.coroutines.flow.flow {
            throw ioException
        }

        // When
        val result = repository.data.first()

        // Then
        assertEquals(User(), result)
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
    fun `updateTerms should update user trade terms`() = runTest {
        // Given
        val updateSlot = slot<suspend (User) -> User>()
        coEvery { mockDataStore.updateData(capture(updateSlot)) } returns User()
        
        val originalUser = User(statement = "existing statement")
        val newTerms = "New trade terms"

        // When
        repository.updateTerms(newTerms)

        // Then
        coVerify { mockDataStore.updateData(any()) }

        val updatedUser = updateSlot.captured(originalUser)
        assertEquals(newTerms, updatedUser.tradeTerms)
        // Verify other fields are preserved
        assertEquals("existing statement", updatedUser.statement)
    }

    @Test
    fun `updateStatement should update user statement`() = runTest {
        // Given
        val updateSlot = slot<suspend (User) -> User>()
        coEvery { mockDataStore.updateData(capture(updateSlot)) } returns User()
        
        val originalUser = User(tradeTerms = "existing terms")
        val newStatement = "New statement"

        // When
        repository.updateStatement(newStatement)

        // Then
        coVerify { mockDataStore.updateData(any()) }
        
        val updatedUser = updateSlot.captured(originalUser)
        assertEquals(newStatement, updatedUser.statement)
        // Verify other fields are preserved
        assertEquals("existing terms", updatedUser.tradeTerms)
    }

    @Test
    fun `update should replace entire user`() = runTest {
        // Given
        val updateSlot = slot<suspend (User) -> User>()
        coEvery { mockDataStore.updateData(capture(updateSlot)) } returns User()
        
        val originalUser = User(tradeTerms = "old terms")
        val newUser = User(
            tradeTerms = "new terms",
            statement = "new statement",
        )

        // When
        repository.update(newUser)

        // Then
        coVerify { mockDataStore.updateData(any()) }
        
        val updatedUser = updateSlot.captured(originalUser)
        assertEquals(newUser, updatedUser)
    }

    @Test
    fun `clear should reset user to default`() = runTest {
        // Given
        val updateSlot = slot<suspend (User) -> User>()
        coEvery { mockDataStore.updateData(capture(updateSlot)) } returns User()
        
        val originalUser = User(
            tradeTerms = "some terms",
            statement = "some statement",
        )

        // When
        repository.clear()

        // Then
        coVerify { mockDataStore.updateData(any()) }
        
        val updatedUser = updateSlot.captured(originalUser)
        assertEquals(User(), updatedUser)
    }

    @Test
    fun `fetch should return first item from data flow`() = runTest {
        // Given
        val expectedUser = User(tradeTerms = "fetched terms")
        every { mockDataStore.data } returns flowOf(expectedUser)

        // When
        val result = repository.fetch()

        // Then
        assertEquals(expectedUser, result)
    }
}

