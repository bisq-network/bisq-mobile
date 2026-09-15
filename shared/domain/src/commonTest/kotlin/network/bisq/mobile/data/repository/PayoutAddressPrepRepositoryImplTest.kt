package network.bisq.mobile.data.repository

import androidx.datastore.core.DataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.model.PayoutAddressPrep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PayoutAddressPrepRepositoryImplTest {
    private class InMemoryDataStore(
        initialValue: PayoutAddressPrep = PayoutAddressPrep(),
    ) : DataStore<PayoutAddressPrep> {
        private val state = MutableStateFlow(initialValue)
        override val data: Flow<PayoutAddressPrep> = state

        override suspend fun updateData(transform: suspend (t: PayoutAddressPrep) -> PayoutAddressPrep): PayoutAddressPrep {
            state.value = transform(state.value)
            return state.value
        }
    }

    private val store = InMemoryDataStore()
    private val repository = PayoutAddressPrepRepositoryImpl(store)

    @Test
    fun `setPrefill stores the address keyed by trade and clearPrefill removes only that trade`() =
        runTest {
            repository.setPrefill("trade-1", "bc1qaddress1")
            repository.setPrefill("trade-2", "bc1qaddress2")

            assertEquals("bc1qaddress1", repository.fetch().prefillByTradeId["trade-1"])

            repository.clearPrefill("trade-1")

            val remaining = repository.fetch().prefillByTradeId
            assertEquals(mapOf("trade-2" to "bc1qaddress2"), remaining)
        }

    @Test
    fun `markTradeCompleted accumulates profiles and is idempotent`() =
        runTest {
            repository.markTradeCompleted("profile-1")
            repository.markTradeCompleted("profile-1")
            repository.markTradeCompleted("profile-2")

            assertEquals(setOf("profile-1", "profile-2"), repository.fetch().profilesWithCompletedTrade)
        }

    @Test
    fun `blank identifiers are rejected`() =
        runTest {
            assertFailsWith<IllegalArgumentException> { repository.setPrefill(" ", "bc1q") }
            assertFailsWith<IllegalArgumentException> { repository.setPrefill("trade-1", " ") }
            assertFailsWith<IllegalArgumentException> { repository.clearPrefill("") }
            assertFailsWith<IllegalArgumentException> { repository.markTradeCompleted("") }
            assertTrue(repository.fetch().prefillByTradeId.isEmpty())
        }
}
