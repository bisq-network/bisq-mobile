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
import network.bisq.mobile.data.model.TradeCancelReason
import network.bisq.mobile.data.model.TradeCancelReasonMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TradeCancelReasonRepositoryImplTest {
    private val mockDataStore = mockk<DataStore<TradeCancelReasonMap>>()
    private val repository = TradeCancelReasonRepositoryImpl(mockDataStore)

    @Test
    fun `data returns the stored reasons`() =
        runTest {
            val stored = TradeCancelReasonMap(mapOf("trade-1" to TradeCancelReason.BANNED_ACCOUNT_DATA))
            every { mockDataStore.data } returns flowOf(stored)

            assertEquals(stored, repository.data.first())
        }

    @Test
    fun `setReason adds the reason and keeps the others`() =
        runTest {
            val update = slot<suspend (TradeCancelReasonMap) -> TradeCancelReasonMap>()
            coEvery { mockDataStore.updateData(capture(update)) } returns TradeCancelReasonMap()

            repository.setReason("trade-2", TradeCancelReason.BANNED_ACCOUNT_DATA)

            coVerify(exactly = 1) { mockDataStore.updateData(any()) }
            val updated = update.captured(TradeCancelReasonMap(mapOf("trade-1" to TradeCancelReason.BANNED_ACCOUNT_DATA)))
            assertEquals(
                mapOf(
                    "trade-1" to TradeCancelReason.BANNED_ACCOUNT_DATA,
                    "trade-2" to TradeCancelReason.BANNED_ACCOUNT_DATA,
                ),
                updated.map,
            )
        }

    @Test
    fun `clearId removes only that trade`() =
        runTest {
            val update = slot<suspend (TradeCancelReasonMap) -> TradeCancelReasonMap>()
            coEvery { mockDataStore.updateData(capture(update)) } returns TradeCancelReasonMap()

            repository.clearId("trade-1")

            val updated =
                update.captured(
                    TradeCancelReasonMap(
                        mapOf(
                            "trade-1" to TradeCancelReason.BANNED_ACCOUNT_DATA,
                            "trade-2" to TradeCancelReason.BANNED_ACCOUNT_DATA,
                        ),
                    ),
                )
            assertEquals(mapOf("trade-2" to TradeCancelReason.BANNED_ACCOUNT_DATA), updated.map)
        }

    @Test
    fun `blank trade ids are rejected`() =
        runTest {
            assertFailsWith<IllegalArgumentException> { repository.setReason(" ", TradeCancelReason.BANNED_ACCOUNT_DATA) }
            assertFailsWith<IllegalArgumentException> { repository.clearId("") }
        }
}
