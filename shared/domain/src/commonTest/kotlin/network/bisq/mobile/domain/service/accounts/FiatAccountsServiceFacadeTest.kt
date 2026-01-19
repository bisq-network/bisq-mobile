package network.bisq.mobile.domain.service.accounts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.data.replicated.account.fiat.FiatAccountVO
import network.bisq.mobile.domain.data.replicated.account.fiat.UserDefinedFiatAccountPayloadVO
import network.bisq.mobile.domain.data.replicated.account.fiat.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.data.replicated.account.payment_method.FiatPaymentRailEnum
import network.bisq.mobile.domain.di.testModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for FiatAccountsServiceFacade.
 *
 * These tests verify the business logic of the abstract FiatAccountsServiceFacade class,
 * including account management, state updates, sorting, and error handling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FiatAccountsServiceFacadeTest : KoinTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testFacade: TestFiatAccountsServiceFacade

    // Test data
    private val accountA =
        UserDefinedFiatAccountVO(
            accountName = "Account A",
            accountPayload =
                UserDefinedFiatAccountPayloadVO(
                    accountData = "accountA@example.com",
                ),
        )

    private val accountB =
        UserDefinedFiatAccountVO(
            accountName = "Account B",
            accountPayload =
                UserDefinedFiatAccountPayloadVO(
                    accountData = "accountB@example.com",
                ),
        )

    private val accountC =
        UserDefinedFiatAccountVO(
            accountName = "Account C",
            accountPayload =
                UserDefinedFiatAccountPayloadVO(
                    accountData = "accountC@example.com",
                ),
        )

    private val accountZ =
        UserDefinedFiatAccountVO(
            accountName = "Zebra Account",
            accountPayload =
                UserDefinedFiatAccountPayloadVO(
                    accountData = "zebra@example.com",
                ),
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin {
            modules(testModule)
        }
        testFacade = TestFiatAccountsServiceFacade()
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    // ========== Get Accounts Tests ==========

    @Test
    fun `when getAccounts succeeds then updates state with sorted accounts`() =
        runTest(testDispatcher) {
            // Given
            val unsortedAccounts = listOf(accountZ, accountB, accountA)
            testFacade.mockExecuteGetAccounts = { Result.success(unsortedAccounts) }

            // When
            val result = testFacade.getAccounts()
            advanceUntilIdle()

            // Then
            assertTrue(result.isSuccess)
            val accounts = result.getOrNull()
            assertNotNull(accounts)
            assertEquals(3, accounts.size)
            // Verify sorting by account name
            assertEquals("Account A", accounts[0].accountName)
            assertEquals("Account B", accounts[1].accountName)
            assertEquals("Zebra Account", accounts[2].accountName)

            // Verify state was updated
            val state = testFacade.accountState.value
            assertEquals(3, state.accounts.size)
            assertEquals("Account A", state.accounts[0].accountName)
        }

    @Test
    fun `when getAccounts with empty list then updates state correctly`() =
        runTest(testDispatcher) {
            // Given
            testFacade.mockExecuteGetAccounts = { Result.success(emptyList()) }

            // When
            val result = testFacade.getAccounts()
            advanceUntilIdle()

            // Then
            assertTrue(result.isSuccess)
            val accounts = result.getOrNull()
            assertNotNull(accounts)
            assertTrue(accounts.isEmpty())

            val state = testFacade.accountState.value
            assertTrue(state.accounts.isEmpty())
        }

    @Test
    fun `when getAccounts with payment rails filter then passes filter to backend`() =
        runTest(testDispatcher) {
            // Given
            val paymentRails = setOf(FiatPaymentRailEnum.ACH_TRANSFER, FiatPaymentRailEnum.SEPA)
            var capturedFilter: Set<FiatPaymentRailEnum>? = null
            testFacade.mockExecuteGetAccounts = { filter ->
                capturedFilter = filter
                Result.success(listOf(accountA))
            }

            // When
            val result = testFacade.getAccounts(paymentRails)
            advanceUntilIdle()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(paymentRails, capturedFilter)
        }

    @Test
    fun `when getAccounts fails then returns failure`() =
        runTest(testDispatcher) {
            // Given
            val exception = Exception("Network error")
            testFacade.mockExecuteGetAccounts = { Result.failure(exception) }

            // When
            val result = testFacade.getAccounts()
            advanceUntilIdle()

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    // ========== Get Selected Account Tests ==========

    @Test
    fun `when getSelectedAccount with existing account then updates index correctly`() =
        runTest(testDispatcher) {
            // Given - setup state with accounts
            testFacade.mockExecuteGetAccounts = { Result.success(listOf(accountA, accountB)) }
            testFacade.getAccounts()
            advanceUntilIdle()

            // Setup selected account
            testFacade.mockExecuteGetSelectedAccount = { Result.success(accountB) }

            // When
            val result = testFacade.getSelectedAccount()
            advanceUntilIdle()

            // Then
            assertTrue(result.isSuccess)
            val state = testFacade.accountState.value
            assertEquals(1, state.selectedAccountIndex) // accountB is at index 1
        }

    @Test
    fun `when getSelectedAccount with null account then sets index to -1`() =
        runTest(testDispatcher) {
            // Given - setup state with accounts
            testFacade.mockExecuteGetAccounts = { Result.success(listOf(accountA, accountB)) }
            testFacade.getAccounts()
            advanceUntilIdle()

            testFacade.mockExecuteGetSelectedAccount = { Result.success(null) }

            // When
            val result = testFacade.getSelectedAccount()
            advanceUntilIdle()

            // Then
            assertTrue(result.isSuccess)
            val state = testFacade.accountState.value
            assertEquals(-1, state.selectedAccountIndex)
        }

    @Test
    fun `when getSelectedAccount with account not in list then sets index to -1`() =
        runTest(testDispatcher) {
            // Given - setup state with accounts
            testFacade.mockExecuteGetAccounts = { Result.success(listOf(accountA, accountB)) }
            testFacade.getAccounts()
            advanceUntilIdle()

            // Return an account that's not in the list
            testFacade.mockExecuteGetSelectedAccount = { Result.success(accountZ) }

            // When
            val result = testFacade.getSelectedAccount()
            advanceUntilIdle()

            // Then
            assertTrue(result.isSuccess)
            val state = testFacade.accountState.value
            assertEquals(-1, state.selectedAccountIndex)
        }

    @Test
    fun `when getSelectedAccount fails then returns failure`() =
        runTest(testDispatcher) {
            // Given
            val exception = Exception("Backend error")
            testFacade.mockExecuteGetSelectedAccount = { Result.failure(exception) }

            // When
            val result = testFacade.getSelectedAccount()
            advanceUntilIdle()

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    // ========== Add Account Tests ==========

    @Test
    fun `when addAccount succeeds then adds account in sorted order and selects it`() =
        runTest(testDispatcher) {
            // Given - start with Account A and C
            testFacade.mockExecuteGetAccounts = { Result.success(listOf(accountA, accountC)) }
            testFacade.getAccounts()
            advanceUntilIdle()

            testFacade.mockExecuteAddAccount = { Result.success(Unit) }
            testFacade.mockExecuteSetSelectedAccount = { Result.success(Unit) }

            // When - add Account B (should be inserted in middle)
            val result = testFacade.addAccount(accountB)
            advanceUntilIdle()

            // Then
            assertTrue(result.isSuccess)
            val state = testFacade.accountState.value
            assertEquals(3, state.accounts.size)
            assertEquals("Account A", state.accounts[0].accountName)
            assertEquals("Account B", state.accounts[1].accountName)
            assertEquals("Account C", state.accounts[2].accountName)
            assertEquals(1, state.selectedAccountIndex) // accountB should be selected
        }

    @Test
    fun `when addAccount to empty list then adds and selects it`() =
        runTest(testDispatcher) {
            // Given - empty state
            testFacade.mockExecuteAddAccount = { Result.success(Unit) }
            testFacade.mockExecuteSetSelectedAccount = { Result.success(Unit) }

            // When
            val result = testFacade.addAccount(accountA)
            advanceUntilIdle()

            // Then
            assertTrue(result.isSuccess)
            val state = testFacade.accountState.value
            assertEquals(1, state.accounts.size)
            assertEquals("Account A", state.accounts[0].accountName)
            assertEquals(0, state.selectedAccountIndex)
        }

    @Test
    fun `when addAccount fails then returns failure and state unchanged`() =
        runTest(testDispatcher) {
            // Given - setup initial state
            testFacade.mockExecuteGetAccounts = { Result.success(listOf(accountA)) }
            testFacade.getAccounts()
            advanceUntilIdle()

            val initialState = testFacade.accountState.value
            val exception = Exception("Add failed")
            testFacade.mockExecuteAddAccount = { Result.failure(exception) }

            // When
            val result = testFacade.addAccount(accountB)
            advanceUntilIdle()

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
            // State should be unchanged
            assertEquals(initialState.accounts.size, testFacade.accountState.value.accounts.size)
        }

    @Test
    fun `when addAccount succeeds then calls setSelectedAccountIndex`() =
        runTest(testDispatcher) {
            // Given
            testFacade.mockExecuteAddAccount = { Result.success(Unit) }
            var setSelectedAccountIndexCalled = false
            var capturedIndex = -1
            testFacade.mockExecuteSetSelectedAccount = { account ->
                setSelectedAccountIndexCalled = true
                capturedIndex = testFacade.accountState.value.selectedAccountIndex
                Result.success(Unit)
            }

            // When
            testFacade.addAccount(accountA)
            advanceUntilIdle()

            // Then
            assertTrue(setSelectedAccountIndexCalled)
            assertEquals(0, capturedIndex)
        }

    // ========== Save Account Tests ==========

    @Test
    fun `when saveAccount succeeds then updates account and maintains sort order`() =
        runTest(testDispatcher) {
            // Given - setup with accounts
            testFacade.mockExecuteGetAccounts = { Result.success(listOf(accountA, accountB, accountC)) }
            testFacade.getAccounts()
            advanceUntilIdle()

            // Select accountB
            testFacade.mockExecuteSetSelectedAccount = { Result.success(Unit) }
            testFacade.setSelectedAccountIndex(1) // Select accountB
            advanceUntilIdle()

            // Create updated account with new name
            val updatedAccount =
                UserDefinedFiatAccountVO(
                    accountName = "Account Z Updated",
                    accountPayload =
                        UserDefinedFiatAccountPayloadVO(
                            accountData = "updated@example.com",
                        ),
                )

            testFacade.mockExecuteSaveAccount = { _, _ -> Result.success(Unit) }

            // When
            val result = testFacade.saveAccount(updatedAccount)
            advanceUntilIdle()

            // Then
            assertTrue(result.isSuccess)
            val state = testFacade.accountState.value
            assertEquals(3, state.accounts.size)
            // Should be sorted: A, C, Z Updated
            assertEquals("Account A", state.accounts[0].accountName)
            assertEquals("Account C", state.accounts[1].accountName)
            assertEquals("Account Z Updated", state.accounts[2].accountName)
            assertEquals(2, state.selectedAccountIndex) // Updated account moved to end
        }

    @Test
    fun `when saveAccount with no selected account then throws IllegalStateException`() =
        runTest(testDispatcher) {
            // Given - no selected account (index -1)
            val updatedAccount =
                UserDefinedFiatAccountVO(
                    accountName = "Updated",
                    accountPayload =
                        UserDefinedFiatAccountPayloadVO(
                            accountData = "test@example.com",
                        ),
                )

            // When/Then
            val result = testFacade.saveAccount(updatedAccount)
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertNotNull(exception)
            assertTrue(exception is IllegalStateException)
            assertEquals("No account selected", exception.message)
        }

    @Test
    fun `when saveAccount fails then returns failure`() =
        runTest(testDispatcher) {
            // Given - setup with selected account
            testFacade.mockExecuteGetAccounts = { Result.success(listOf(accountA)) }
            testFacade.getAccounts()
            advanceUntilIdle()

            testFacade.mockExecuteSetSelectedAccount = { Result.success(Unit) }
            testFacade.setSelectedAccountIndex(0)
            advanceUntilIdle()

            val exception = Exception("Save failed")
            testFacade.mockExecuteSaveAccount = { _, _ -> Result.failure(exception) }

            // When
            val result = testFacade.saveAccount(accountA)
            advanceUntilIdle()

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    // ========== Delete Account Tests ==========

    @Test
    fun `when deleteAccount of non-selected account then removes it and preserves selection`() =
        runTest(testDispatcher) {
            // Given - setup with accounts
            testFacade.mockExecuteGetAccounts = { Result.success(listOf(accountA, accountB, accountC)) }
            testFacade.getAccounts()
            advanceUntilIdle()

            // Select accountA (index 0)
            testFacade.mockExecuteSetSelectedAccount = { Result.success(Unit) }
            testFacade.setSelectedAccountIndex(0)
            advanceUntilIdle()

            testFacade.mockExecuteDeleteAccount = { Result.success(Unit) }

            // When - delete accountC (not selected)
            val result = testFacade.deleteAccount(accountC)
            advanceUntilIdle()

            // Then
            assertTrue(result.isSuccess)
            val state = testFacade.accountState.value
            assertEquals(2, state.accounts.size)
            assertEquals("Account A", state.accounts[0].accountName)
            assertEquals("Account B", state.accounts[1].accountName)
            assertEquals(0, state.selectedAccountIndex) // Still accountA
        }

    @Test
    fun `when deleteAccount of selected account with others remaining then selects index 0`() =
        runTest(testDispatcher) {
            // Given - setup with accounts
            testFacade.mockExecuteGetAccounts = { Result.success(listOf(accountA, accountB, accountC)) }
            testFacade.getAccounts()
            advanceUntilIdle()

            // Select accountB (index 1)
            testFacade.mockExecuteSetSelectedAccount = { Result.success(Unit) }
            testFacade.setSelectedAccountIndex(1)
            advanceUntilIdle()

            testFacade.mockExecuteDeleteAccount = { Result.success(Unit) }

            // When - delete selected accountB
            val result = testFacade.deleteAccount(accountB)
            advanceUntilIdle()

            // Then
            assertTrue(result.isSuccess)
            val state = testFacade.accountState.value
            assertEquals(2, state.accounts.size)
            assertEquals("Account A", state.accounts[0].accountName)
            assertEquals("Account C", state.accounts[1].accountName)
            assertEquals(0, state.selectedAccountIndex) // Should reset to 0
        }

    @Test
    fun `when deleteAccount of last account then sets index to -1`() =
        runTest(testDispatcher) {
            // Given - setup with one account
            testFacade.mockExecuteGetAccounts = { Result.success(listOf(accountA)) }
            testFacade.getAccounts()
            advanceUntilIdle()

            testFacade.mockExecuteSetSelectedAccount = { Result.success(Unit) }
            testFacade.setSelectedAccountIndex(0)
            advanceUntilIdle()

            testFacade.mockExecuteDeleteAccount = { Result.success(Unit) }

            // When - delete the only account
            val result = testFacade.deleteAccount(accountA)
            advanceUntilIdle()

            // Then
            assertTrue(result.isSuccess)
            val state = testFacade.accountState.value
            assertTrue(state.accounts.isEmpty())
            assertEquals(-1, state.selectedAccountIndex)
        }

    @Test
    fun `when deleteAccount fails then returns failure and state unchanged`() =
        runTest(testDispatcher) {
            // Given - setup with accounts
            testFacade.mockExecuteGetAccounts = { Result.success(listOf(accountA, accountB)) }
            testFacade.getAccounts()
            advanceUntilIdle()

            val initialState = testFacade.accountState.value
            val exception = Exception("Delete failed")
            testFacade.mockExecuteDeleteAccount = { Result.failure(exception) }

            // When
            val result = testFacade.deleteAccount(accountA)
            advanceUntilIdle()

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
            // State should be unchanged
            assertEquals(initialState.accounts.size, testFacade.accountState.value.accounts.size)
        }

    @Test
    fun `when deleteAccount succeeds then calls setSelectedAccountIndex with new index`() =
        runTest(testDispatcher) {
            // Given - setup with accounts
            testFacade.mockExecuteGetAccounts = { Result.success(listOf(accountA, accountB)) }
            testFacade.getAccounts()
            advanceUntilIdle()

            var setSelectedAccountCalled = false
            testFacade.mockExecuteSetSelectedAccount = {
                setSelectedAccountCalled = true
                Result.success(Unit)
            }
            testFacade.setSelectedAccountIndex(0)
            advanceUntilIdle()

            setSelectedAccountCalled = false // Reset flag
            testFacade.mockExecuteDeleteAccount = { Result.success(Unit) }

            // When - delete selected account
            testFacade.deleteAccount(accountA)
            advanceUntilIdle()

            // Then - should call setSelectedAccountIndex
            assertTrue(setSelectedAccountCalled)
        }

    // ========== Set Selected Account Index Tests ==========

    @Test
    fun `when setSelectedAccountIndex with different index then updates state and calls backend`() =
        runTest(testDispatcher) {
            // Given - setup with accounts
            testFacade.mockExecuteGetAccounts = { Result.success(listOf(accountA, accountB, accountC)) }
            testFacade.getAccounts()
            advanceUntilIdle()

            testFacade.mockExecuteSetSelectedAccount = { Result.success(Unit) }
            testFacade.setSelectedAccountIndex(0) // Select first
            advanceUntilIdle()

            var capturedAccount: FiatAccountVO? = null
            testFacade.mockExecuteSetSelectedAccount = { account ->
                capturedAccount = account
                Result.success(Unit)
            }

            // When - change to index 1
            val result = testFacade.setSelectedAccountIndex(1)
            advanceUntilIdle()

            // Then
            assertTrue(result.isSuccess)
            val state = testFacade.accountState.value
            assertEquals(1, state.selectedAccountIndex)
            assertNotNull(capturedAccount)
            assertEquals("Account B", capturedAccount!!.accountName)
        }

    @Test
    fun `when setSelectedAccountIndex with same index then still calls backend`() =
        runTest(testDispatcher) {
            // Given - setup with accounts
            testFacade.mockExecuteGetAccounts = { Result.success(listOf(accountA, accountB)) }
            testFacade.getAccounts()
            advanceUntilIdle()

            var callCount = 0
            testFacade.mockExecuteSetSelectedAccount = {
                callCount++
                Result.success(Unit)
            }

            // Set to index 0
            testFacade.setSelectedAccountIndex(0)
            advanceUntilIdle()
            val firstCallCount = callCount

            // When - set to same index 0 again
            val result = testFacade.setSelectedAccountIndex(0)
            advanceUntilIdle()

            // Then - state update skipped but backend still called
            assertTrue(result.isSuccess)
            assertEquals(firstCallCount + 1, callCount) // Backend should be called again
        }

    @Test
    fun `when setSelectedAccountIndex with invalid index then does not call backend`() =
        runTest(testDispatcher) {
            // Given - setup with accounts
            testFacade.mockExecuteGetAccounts = { Result.success(listOf(accountA)) }
            testFacade.getAccounts()
            advanceUntilIdle()

            var backendCalled = false
            testFacade.mockExecuteSetSelectedAccount = {
                backendCalled = true
                Result.success(Unit)
            }

            // When - set to invalid index
            val result = testFacade.setSelectedAccountIndex(99)
            advanceUntilIdle()

            // Then - backend should not be called (no account at that index)
            assertTrue(result.isSuccess)
            assertFalse(backendCalled)
            assertEquals(99, testFacade.accountState.value.selectedAccountIndex)
        }

    @Test
    fun `when setSelectedAccountIndex fails then returns failure`() =
        runTest(testDispatcher) {
            // Given - setup with accounts
            testFacade.mockExecuteGetAccounts = { Result.success(listOf(accountA, accountB)) }
            testFacade.getAccounts()
            advanceUntilIdle()

            val exception = Exception("Backend error")
            testFacade.mockExecuteSetSelectedAccount = { Result.failure(exception) }

            // When
            val result = testFacade.setSelectedAccountIndex(1)
            advanceUntilIdle()

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    // ========== Helper Method Tests ==========

    @Test
    fun `when getSortedAccounts then sorts by account name`() {
        // Given
        val unsortedAccounts = listOf(accountZ, accountB, accountA, accountC)

        // When
        val sortedAccounts = testFacade.testGetSortedAccounts(unsortedAccounts)

        // Then
        assertEquals(4, sortedAccounts.size)
        assertEquals("Account A", sortedAccounts[0].accountName)
        assertEquals("Account B", sortedAccounts[1].accountName)
        assertEquals("Account C", sortedAccounts[2].accountName)
        assertEquals("Zebra Account", sortedAccounts[3].accountName)
    }

    @Test
    fun `when getCurrentSelectedAccount with valid index then returns account`() =
        runTest(testDispatcher) {
            // Given - setup with accounts
            testFacade.mockExecuteGetAccounts = { Result.success(listOf(accountA, accountB)) }
            testFacade.getAccounts()
            advanceUntilIdle()

            testFacade.mockExecuteSetSelectedAccount = { Result.success(Unit) }
            testFacade.setSelectedAccountIndex(1)
            advanceUntilIdle()

            // When
            val currentAccount = testFacade.testGetCurrentSelectedAccount()

            // Then
            assertNotNull(currentAccount)
            assertEquals("Account B", currentAccount.accountName)
        }

    @Test
    fun `when getCurrentSelectedAccount with invalid index then returns null`() =
        runTest(testDispatcher) {
            // Given - setup with accounts but invalid index
            testFacade.mockExecuteGetAccounts = { Result.success(listOf(accountA)) }
            testFacade.getAccounts()
            advanceUntilIdle()

            // When - state has index -1 by default
            val currentAccount = testFacade.testGetCurrentSelectedAccount()

            // Then
            assertNull(currentAccount)
        }

    @Test
    fun `when getAccountsExcluding then filters out specified account`() =
        runTest(testDispatcher) {
            // Given - setup with accounts
            testFacade.mockExecuteGetAccounts = { Result.success(listOf(accountA, accountB, accountC)) }
            testFacade.getAccounts()
            advanceUntilIdle()

            // When
            val filteredAccounts = testFacade.testGetAccountsExcluding("Account B")

            // Then
            assertEquals(2, filteredAccounts.size)
            assertEquals("Account A", filteredAccounts[0].accountName)
            assertEquals("Account C", filteredAccounts[1].accountName)
        }

    @Test
    fun `when getAccountsExcluding with non-existent account then returns all accounts`() =
        runTest(testDispatcher) {
            // Given - setup with accounts
            testFacade.mockExecuteGetAccounts = { Result.success(listOf(accountA, accountB)) }
            testFacade.getAccounts()
            advanceUntilIdle()

            // When
            val filteredAccounts = testFacade.testGetAccountsExcluding("Non Existent")

            // Then
            assertEquals(2, filteredAccounts.size)
        }

    // ========== State Flow Tests ==========

    @Test
    fun `when multiple operations then accountState emits correct values`() =
        runTest(testDispatcher) {
            // Given
            testFacade.mockExecuteGetAccounts = { Result.success(listOf(accountA)) }
            testFacade.mockExecuteAddAccount = { Result.success(Unit) }
            testFacade.mockExecuteSetSelectedAccount = { Result.success(Unit) }

            // When - perform operations and verify state after each
            testFacade.getAccounts()
            advanceUntilIdle()

            val stateAfterGet = testFacade.accountState.value
            assertEquals(1, stateAfterGet.accounts.size)

            testFacade.addAccount(accountB)
            advanceUntilIdle()

            // Then - verify final state has both accounts
            val finalState = testFacade.accountState.value
            assertEquals(2, finalState.accounts.size)
            assertEquals("Account A", finalState.accounts[0].accountName)
            assertEquals("Account B", finalState.accounts[1].accountName)
        }

    // ========== Test Implementation ==========

    /**
     * Concrete test implementation of FiatAccountsServiceFacade that allows mocking
     * of abstract methods for testing purposes.
     */
    private class TestFiatAccountsServiceFacade : FiatAccountsServiceFacade() {
        var mockExecuteGetAccounts: (Set<FiatPaymentRailEnum>?) -> Result<List<FiatAccountVO>> =
            { Result.success(emptyList()) }

        var mockExecuteGetSelectedAccount: () -> Result<FiatAccountVO?> =
            { Result.success(null) }

        var mockExecuteAddAccount: (FiatAccountVO) -> Result<Unit> =
            { Result.success(Unit) }

        var mockExecuteSaveAccount: (String, FiatAccountVO) -> Result<Unit> =
            { _, _ -> Result.success(Unit) }

        var mockExecuteDeleteAccount: (String) -> Result<Unit> =
            { Result.success(Unit) }

        var mockExecuteSetSelectedAccount: (FiatAccountVO) -> Result<Unit> =
            { Result.success(Unit) }

        override suspend fun executeGetAccounts(paymentRails: Set<FiatPaymentRailEnum>?): Result<List<FiatAccountVO>> = mockExecuteGetAccounts(paymentRails)

        override suspend fun executeGetSelectedAccount(): Result<FiatAccountVO?> = mockExecuteGetSelectedAccount()

        override suspend fun executeAddAccount(account: FiatAccountVO): Result<Unit> = mockExecuteAddAccount(account)

        override suspend fun executeSaveAccount(
            accountName: String,
            account: FiatAccountVO,
        ): Result<Unit> = mockExecuteSaveAccount(accountName, account)

        override suspend fun executeDeleteAccount(accountName: String): Result<Unit> = mockExecuteDeleteAccount(accountName)

        override suspend fun executeSetSelectedAccount(account: FiatAccountVO): Result<Unit> = mockExecuteSetSelectedAccount(account)

        // Expose protected methods for testing
        fun testGetSortedAccounts(accounts: List<FiatAccountVO>) = getSortedAccounts(accounts)

        fun testGetCurrentSelectedAccount() = getCurrentSelectedAccount()

        fun testGetAccountsExcluding(accountName: String) = getAccountsExcluding(accountName)
    }
}
