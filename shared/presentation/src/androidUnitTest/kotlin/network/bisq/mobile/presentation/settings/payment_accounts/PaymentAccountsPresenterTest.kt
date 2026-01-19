package network.bisq.mobile.presentation.settings.payment_accounts

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.data.replicated.account.fiat.UserDefinedFiatAccountPayloadVO
import network.bisq.mobile.domain.data.replicated.account.fiat.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.service.accounts.AccountsState
import network.bisq.mobile.domain.service.accounts.FiatAccountsServiceFacade
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.main.MainPresenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for PaymentAccountsPresenter.
 *
 * These tests verify the business logic of the PaymentAccountsPresenter,
 * including account loading, state management, and user actions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PaymentAccountsPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fiatAccountsServiceFacade: FiatAccountsServiceFacade
    private lateinit var mainPresenter: MainPresenter
    private lateinit var presenter: PaymentAccountsPresenter

    // Test data
    private val sampleAccount1 =
        UserDefinedFiatAccountVO(
            accountName = "PayPal Account",
            accountPayload =
                UserDefinedFiatAccountPayloadVO(
                    accountData = "user@example.com",
                ),
        )

    private val sampleAccount2 =
        UserDefinedFiatAccountVO(
            accountName = "Bank Transfer",
            accountPayload =
                UserDefinedFiatAccountPayloadVO(
                    accountData = "IBAN: DE89370400440532013000",
                ),
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Setup mocks
        fiatAccountsServiceFacade = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)

        startKoin {
            modules(
                module {
                    single<NavigationManager> { mockk(relaxed = true) }
                    single<CoroutineJobsManager> { DefaultCoroutineJobsManager() }
                    single<GlobalUiManager> { mockk(relaxed = true) }
                },
            )
        }

        // Default mock behaviors
        every { fiatAccountsServiceFacade.accountState } returns MutableStateFlow(AccountsState())
    }

    @AfterTest
    fun tearDown() {
        try {
            stopKoin()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createPresenter(): PaymentAccountsPresenter =
        PaymentAccountsPresenter(
            fiatAccountsServiceFacade,
            mainPresenter,
        )

    @Test
    fun `when initial state then has correct default values`() =
        runTest(testDispatcher) {
            // Given
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(emptyList())

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertTrue(state.accounts.isEmpty())
            assertEquals(-1, state.selectedAccountIndex)
            assertFalse(state.isLoadingAccounts)
            assertFalse(state.isLoadingAccountsError)
            assertFalse(state.showAddAccountState)
            assertFalse(state.showEditAccountState)
        }

    @Test
    fun `when add account clicked then shows add account state`() =
        runTest(testDispatcher) {
            // Given
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(emptyList())
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountsUiAction.OnAddAccountClick)
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertTrue(state.showAddAccountState)
            assertFalse(state.showEditAccountState)
        }

    // ========== Account Loading Tests ==========

    @Test
    fun `when loading accounts succeeds then updates state correctly`() =
        runTest(testDispatcher) {
            // Given
            val accounts = listOf(sampleAccount1, sampleAccount2)
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(accounts)
            coEvery { fiatAccountsServiceFacade.getSelectedAccount() } returns Result.success(Unit)

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            coVerify { fiatAccountsServiceFacade.getAccounts() }
            coVerify { fiatAccountsServiceFacade.getSelectedAccount() }
            val state = presenter.uiState.value
            assertFalse(state.isLoadingAccounts)
            assertFalse(state.isLoadingAccountsError)
        }

    @Test
    fun `when loading accounts with empty list then does not fetch selected account`() =
        runTest(testDispatcher) {
            // Given
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(emptyList())

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            coVerify { fiatAccountsServiceFacade.getAccounts() }
            coVerify(exactly = 0) { fiatAccountsServiceFacade.getSelectedAccount() }
            val state = presenter.uiState.value
            assertFalse(state.isLoadingAccounts)
            assertFalse(state.isLoadingAccountsError)
        }

    @Test
    fun `when loading accounts fails then sets error state`() =
        runTest(testDispatcher) {
            // Given
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.failure(Exception("Network error"))

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertFalse(state.isLoadingAccounts)
            assertTrue(state.isLoadingAccountsError)
        }

    @Test
    fun `when loading selected account fails then sets error state`() =
        runTest(testDispatcher) {
            // Given
            val accounts = listOf(sampleAccount1)
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(accounts)
            coEvery { fiatAccountsServiceFacade.getSelectedAccount() } returns Result.failure(Exception("Error"))

            // When
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertFalse(state.isLoadingAccounts)
            assertTrue(state.isLoadingAccountsError)
        }

    @Test
    fun `when retry load accounts clicked then reloads accounts`() =
        runTest(testDispatcher) {
            // Given
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.failure(Exception("Error"))
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // Setup successful response for retry
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(listOf(sampleAccount1))

            // When
            presenter.onAction(PaymentAccountsUiAction.OnRetryLoadAccountsClick)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 2) { fiatAccountsServiceFacade.getAccounts() }
            val state = presenter.uiState.value
            assertFalse(state.isLoadingAccounts)
        }

    // ========== Account Observation Tests ==========

    @Test
    fun `when account state changes then updates ui state`() =
        runTest(testDispatcher) {
            // Given
            val accountStateFlow = MutableStateFlow(AccountsState())
            every { fiatAccountsServiceFacade.accountState } returns accountStateFlow
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(emptyList())

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            accountStateFlow.value =
                AccountsState(
                    accounts = listOf(sampleAccount1, sampleAccount2),
                    selectedAccountIndex = 0,
                )
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(2, state.accounts.size)
            assertEquals(0, state.selectedAccountIndex)
            assertEquals("PayPal Account", state.accountNameEntry.value)
            assertEquals("user@example.com", state.accountDescriptionEntry.value)
            assertFalse(state.showAddAccountState)
            assertFalse(state.showEditAccountState)
        }

    @Test
    fun `when selected account changes then updates fields`() =
        runTest(testDispatcher) {
            // Given
            val accountStateFlow =
                MutableStateFlow(
                    AccountsState(
                        accounts = listOf(sampleAccount1, sampleAccount2),
                        selectedAccountIndex = 0,
                    ),
                )
            every { fiatAccountsServiceFacade.accountState } returns accountStateFlow
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns
                Result.success(
                    listOf(
                        sampleAccount1,
                        sampleAccount2,
                    ),
                )
            coEvery { fiatAccountsServiceFacade.getSelectedAccount() } returns Result.success(Unit)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            accountStateFlow.value =
                AccountsState(
                    accounts = listOf(sampleAccount1, sampleAccount2),
                    selectedAccountIndex = 1,
                )
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals("Bank Transfer", state.accountNameEntry.value)
            assertEquals("IBAN: DE89370400440532013000", state.accountDescriptionEntry.value)
        }

    // ========== Validation Tests ==========

    @Test
    fun `when account name is too short then validation fails`() =
        runTest(testDispatcher) {
            // Given
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(emptyList())
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PaymentAccountsUiAction.OnAddAccountClick)
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountsUiAction.OnAccountNameChange("ab"))
            presenter.onAction(PaymentAccountsUiAction.OnAccountDescriptionChange("Valid description text"))
            presenter.onAction(PaymentAccountsUiAction.OnConfirmAddAccountClick)
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertNotNull(state.accountNameEntry.errorMessage)
            assertTrue(state.showAddAccountState) // Still in add mode
        }

    @Test
    fun `when account name is too long then validation fails`() =
        runTest(testDispatcher) {
            // Given
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(emptyList())
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PaymentAccountsUiAction.OnAddAccountClick)
            advanceUntilIdle()

            // When
            val longName = "a".repeat(257)
            presenter.onAction(PaymentAccountsUiAction.OnAccountNameChange(longName))
            presenter.onAction(PaymentAccountsUiAction.OnAccountDescriptionChange("Valid description text"))
            presenter.onAction(PaymentAccountsUiAction.OnConfirmAddAccountClick)
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertNotNull(state.accountNameEntry.errorMessage)
        }

    @Test
    fun `when account description is too short then validation fails`() =
        runTest(testDispatcher) {
            // Given
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(emptyList())
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PaymentAccountsUiAction.OnAddAccountClick)
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountsUiAction.OnAccountNameChange("Valid Name"))
            presenter.onAction(PaymentAccountsUiAction.OnAccountDescriptionChange("ab"))
            presenter.onAction(PaymentAccountsUiAction.OnConfirmAddAccountClick)
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertNotNull(state.accountDescriptionEntry.errorMessage)
        }

    @Test
    fun `when account description is too long then validation fails`() =
        runTest(testDispatcher) {
            // Given
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(emptyList())
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PaymentAccountsUiAction.OnAddAccountClick)
            advanceUntilIdle()

            // When
            val longDescription = "a".repeat(1001)
            presenter.onAction(PaymentAccountsUiAction.OnAccountNameChange("Valid Name"))
            presenter.onAction(PaymentAccountsUiAction.OnAccountDescriptionChange(longDescription))
            presenter.onAction(PaymentAccountsUiAction.OnConfirmAddAccountClick)
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertNotNull(state.accountDescriptionEntry.errorMessage)
        }

    @Test
    fun `when adding duplicate account name then validation fails`() =
        runTest(testDispatcher) {
            // Given
            val accountStateFlow =
                MutableStateFlow(
                    AccountsState(accounts = listOf(sampleAccount1)),
                )
            every { fiatAccountsServiceFacade.accountState } returns accountStateFlow
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(listOf(sampleAccount1))

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PaymentAccountsUiAction.OnAddAccountClick)
            advanceUntilIdle()

            // When - try to add account with existing name
            presenter.onAction(PaymentAccountsUiAction.OnAccountNameChange("PayPal Account"))
            presenter.onAction(PaymentAccountsUiAction.OnAccountDescriptionChange("different@email.com"))
            presenter.onAction(PaymentAccountsUiAction.OnConfirmAddAccountClick)
            advanceUntilIdle()

            // Then
            // Validation failed - service should not be called
            val state = presenter.uiState.value
            assertTrue(state.showAddAccountState) // Still in add mode
        }

    // ========== Add Account Tests ==========

    @Test
    fun `when adding account with valid data then succeeds`() =
        runTest(testDispatcher) {
            // Given
            val accountStateFlow = MutableStateFlow(AccountsState())
            every { fiatAccountsServiceFacade.accountState } returns accountStateFlow
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(emptyList())
            coEvery { fiatAccountsServiceFacade.addAccount(any()) } coAnswers {
                accountStateFlow.value =
                    AccountsState(
                        accounts = listOf(firstArg()),
                        selectedAccountIndex = 0,
                    )
                Result.success(Unit)
            }

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PaymentAccountsUiAction.OnAddAccountClick)
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountsUiAction.OnAccountNameChange("New Account"))
            presenter.onAction(PaymentAccountsUiAction.OnAccountDescriptionChange("account@example.com"))
            presenter.onAction(PaymentAccountsUiAction.OnConfirmAddAccountClick)
            advanceUntilIdle()

            // Then
            // Verify the account was added through state observation
            val accountState = accountStateFlow.value
            assertEquals(1, accountState.accounts.size)
            assertEquals("New Account", accountState.accounts[0].accountName)
            val payload = accountState.accounts[0].accountPayload as UserDefinedFiatAccountPayloadVO
            assertEquals("account@example.com", payload.accountData)
            assertFalse(presenter.uiState.value.showAddAccountState) // Dialog should be closed
        }

    @Test
    fun `when adding account fails then shows error`() =
        runTest(testDispatcher) {
            // Given
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(emptyList())
            coEvery { fiatAccountsServiceFacade.addAccount(any()) } returns Result.failure(Exception("Error"))

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PaymentAccountsUiAction.OnAddAccountClick)
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountsUiAction.OnAccountNameChange("New Account"))
            presenter.onAction(PaymentAccountsUiAction.OnAccountDescriptionChange("account@example.com"))
            presenter.onAction(PaymentAccountsUiAction.OnConfirmAddAccountClick)
            advanceUntilIdle()

            // Then
            // Verify add account was called but failed
            coVerify(atLeast = 1) { fiatAccountsServiceFacade.addAccount(any()) }
            // Should still be in add mode since it failed
            assertTrue(presenter.uiState.value.showAddAccountState)
        }

    // ========== Save Account Tests ==========

    @Test
    fun `when saving account with valid data then succeeds`() =
        runTest(testDispatcher) {
            // Given
            val accountStateFlow =
                MutableStateFlow(
                    AccountsState(
                        accounts = listOf(sampleAccount1),
                        selectedAccountIndex = 0,
                    ),
                )
            every { fiatAccountsServiceFacade.accountState } returns accountStateFlow
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(listOf(sampleAccount1))
            coEvery { fiatAccountsServiceFacade.getSelectedAccount() } returns Result.success(Unit)
            coEvery { fiatAccountsServiceFacade.saveAccount(any()) } returns Result.success(Unit)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PaymentAccountsUiAction.OnEditAccountClick)
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountsUiAction.OnAccountNameChange("Updated Account"))
            presenter.onAction(PaymentAccountsUiAction.OnAccountDescriptionChange("updated@example.com"))
            presenter.onAction(PaymentAccountsUiAction.OnSaveAccountClick)
            advanceUntilIdle()

            // Then
            // Verify the mock was called and state can be checked
            coVerify(atLeast = 1) { fiatAccountsServiceFacade.saveAccount(any()) }
        }

    @Test
    fun `when saving account with same name then succeeds`() =
        runTest(testDispatcher) {
            // Given
            val accountStateFlow =
                MutableStateFlow(
                    AccountsState(
                        accounts = listOf(sampleAccount1),
                        selectedAccountIndex = 0,
                    ),
                )
            every { fiatAccountsServiceFacade.accountState } returns accountStateFlow
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(listOf(sampleAccount1))
            coEvery { fiatAccountsServiceFacade.getSelectedAccount() } returns Result.success(Unit)
            coEvery { fiatAccountsServiceFacade.saveAccount(any()) } returns Result.success(Unit)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PaymentAccountsUiAction.OnEditAccountClick)
            advanceUntilIdle()

            // When - keep the same name but change description
            presenter.onAction(PaymentAccountsUiAction.OnAccountDescriptionChange("updated@example.com"))
            presenter.onAction(PaymentAccountsUiAction.OnSaveAccountClick)
            advanceUntilIdle()

            // Then - should succeed (same name is allowed when editing the current account)
            coVerify(atLeast = 1) { fiatAccountsServiceFacade.saveAccount(any()) }
        }

    @Test
    fun `when saving account with duplicate name then fails`() =
        runTest(testDispatcher) {
            // Given
            val accountStateFlow =
                MutableStateFlow(
                    AccountsState(
                        accounts = listOf(sampleAccount1, sampleAccount2),
                        selectedAccountIndex = 0,
                    ),
                )
            every { fiatAccountsServiceFacade.accountState } returns accountStateFlow
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns
                Result.success(
                    listOf(
                        sampleAccount1,
                        sampleAccount2,
                    ),
                )
            coEvery { fiatAccountsServiceFacade.getSelectedAccount() } returns Result.success(Unit)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PaymentAccountsUiAction.OnEditAccountClick)
            advanceUntilIdle()

            // When - try to rename to existing account name
            presenter.onAction(PaymentAccountsUiAction.OnAccountNameChange("Bank Transfer"))
            presenter.onAction(PaymentAccountsUiAction.OnSaveAccountClick)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 0) { fiatAccountsServiceFacade.saveAccount(any()) }
        }

    @Test
    fun `when saving account with invalid fields then fails`() =
        runTest(testDispatcher) {
            // Given
            val accountStateFlow =
                MutableStateFlow(
                    AccountsState(
                        accounts = listOf(sampleAccount1),
                        selectedAccountIndex = 0,
                    ),
                )
            every { fiatAccountsServiceFacade.accountState } returns accountStateFlow
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(listOf(sampleAccount1))
            coEvery { fiatAccountsServiceFacade.getSelectedAccount() } returns Result.success(Unit)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PaymentAccountsUiAction.OnEditAccountClick)
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountsUiAction.OnAccountNameChange("ab")) // Too short
            presenter.onAction(PaymentAccountsUiAction.OnSaveAccountClick)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 0) { fiatAccountsServiceFacade.saveAccount(any()) }
            val state = presenter.uiState.value
            assertNotNull(state.accountNameEntry.errorMessage)
        }

    @Test
    fun `when saving account fails then shows error`() =
        runTest(testDispatcher) {
            // Given
            val accountStateFlow =
                MutableStateFlow(
                    AccountsState(
                        accounts = listOf(sampleAccount1),
                        selectedAccountIndex = 0,
                    ),
                )
            every { fiatAccountsServiceFacade.accountState } returns accountStateFlow
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(listOf(sampleAccount1))
            coEvery { fiatAccountsServiceFacade.getSelectedAccount() } returns Result.success(Unit)
            coEvery { fiatAccountsServiceFacade.saveAccount(any()) } returns Result.failure(Exception("Error"))

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PaymentAccountsUiAction.OnEditAccountClick)
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountsUiAction.OnAccountNameChange("Updated Name"))
            presenter.onAction(PaymentAccountsUiAction.OnSaveAccountClick)
            advanceUntilIdle()

            // Then
            // Verify save was called but failed
            coVerify(atLeast = 1) { fiatAccountsServiceFacade.saveAccount(any()) }
        }

    // ========== Delete Account Tests ==========

    @Test
    fun `when deleting account succeeds then closes dialog`() =
        runTest(testDispatcher) {
            // Given
            val accountStateFlow =
                MutableStateFlow(
                    AccountsState(
                        accounts = listOf(sampleAccount1),
                        selectedAccountIndex = 0,
                    ),
                )
            every { fiatAccountsServiceFacade.accountState } returns accountStateFlow
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(listOf(sampleAccount1))
            coEvery { fiatAccountsServiceFacade.getSelectedAccount() } returns Result.success(Unit)
            coEvery { fiatAccountsServiceFacade.deleteAccount(any()) } returns Result.success(Unit)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountsUiAction.OnDeleteAccountClick)
            advanceUntilIdle()
            presenter.onAction(PaymentAccountsUiAction.OnConfirmDeleteAccountClick)
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertFalse(state.showDeleteConfirmationDialog)
        }

    @Test
    fun `when deleting account fails then dialog remains open`() =
        runTest(testDispatcher) {
            // Given
            val accountStateFlow =
                MutableStateFlow(
                    AccountsState(
                        accounts = listOf(sampleAccount1),
                        selectedAccountIndex = 0,
                    ),
                )
            every { fiatAccountsServiceFacade.accountState } returns accountStateFlow
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(listOf(sampleAccount1))
            coEvery { fiatAccountsServiceFacade.getSelectedAccount() } returns Result.success(Unit)
            coEvery { fiatAccountsServiceFacade.deleteAccount(any()) } returns Result.failure(Exception("Delete failed"))

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountsUiAction.OnDeleteAccountClick)
            advanceUntilIdle()
            presenter.onAction(PaymentAccountsUiAction.OnConfirmDeleteAccountClick)
            advanceUntilIdle()

            // Then
            // Verify delete was called but failed
            coVerify(atLeast = 1) { fiatAccountsServiceFacade.deleteAccount(any()) }
            // Verify the dialog remains open (presenter doesn't close it on failure)
            val state = presenter.uiState.value
            assertTrue(state.showDeleteConfirmationDialog)
        }

    // ========== UI Action Tests ==========

    @Test
    fun `when edit account clicked then shows edit state`() =
        runTest(testDispatcher) {
            // Given
            val accountStateFlow =
                MutableStateFlow(
                    AccountsState(
                        accounts = listOf(sampleAccount1),
                        selectedAccountIndex = 0,
                    ),
                )
            every { fiatAccountsServiceFacade.accountState } returns accountStateFlow
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(listOf(sampleAccount1))
            coEvery { fiatAccountsServiceFacade.getSelectedAccount() } returns Result.success(Unit)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountsUiAction.OnEditAccountClick)
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertTrue(state.showEditAccountState)
            assertFalse(state.showAddAccountState)
        }

    @Test
    fun `when account name changed then updates state`() =
        runTest(testDispatcher) {
            // Given
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(emptyList())
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountsUiAction.OnAccountNameChange("New Name"))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals("New Name", state.accountNameEntry.value)
        }

    @Test
    fun `when account description changed then updates state`() =
        runTest(testDispatcher) {
            // Given
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(emptyList())
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountsUiAction.OnAccountDescriptionChange("New Description"))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals("New Description", state.accountDescriptionEntry.value)
        }

    @Test
    fun `when account selected with different index then calls service`() =
        runTest(testDispatcher) {
            // Given
            val accountStateFlow =
                MutableStateFlow(
                    AccountsState(
                        accounts = listOf(sampleAccount1, sampleAccount2),
                        selectedAccountIndex = 0,
                    ),
                )
            every { fiatAccountsServiceFacade.accountState } returns accountStateFlow
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns
                Result.success(
                    listOf(
                        sampleAccount1,
                        sampleAccount2,
                    ),
                )
            coEvery { fiatAccountsServiceFacade.getSelectedAccount() } returns Result.success(Unit)
            coEvery { fiatAccountsServiceFacade.setSelectedAccountIndex(any()) } returns Result.success(Unit)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountsUiAction.OnAccountSelect(1))
            advanceUntilIdle()

            // Then
            coVerify { fiatAccountsServiceFacade.setSelectedAccountIndex(1) }
        }

    @Test
    fun `when account selected with same index then does nothing`() =
        runTest(testDispatcher) {
            // Given
            val accountStateFlow =
                MutableStateFlow(
                    AccountsState(
                        accounts = listOf(sampleAccount1),
                        selectedAccountIndex = 0,
                    ),
                )
            every { fiatAccountsServiceFacade.accountState } returns accountStateFlow
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(listOf(sampleAccount1))
            coEvery { fiatAccountsServiceFacade.getSelectedAccount() } returns Result.success(Unit)
            coEvery { fiatAccountsServiceFacade.setSelectedAccountIndex(any()) } returns Result.success(Unit)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountsUiAction.OnAccountSelect(0))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 0) { fiatAccountsServiceFacade.setSelectedAccountIndex(any()) }
        }

    @Test
    fun `when cancel add edit clicked then restores previous values`() =
        runTest(testDispatcher) {
            // Given
            val accountStateFlow =
                MutableStateFlow(
                    AccountsState(
                        accounts = listOf(sampleAccount1),
                        selectedAccountIndex = 0,
                    ),
                )
            every { fiatAccountsServiceFacade.accountState } returns accountStateFlow
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(listOf(sampleAccount1))
            coEvery { fiatAccountsServiceFacade.getSelectedAccount() } returns Result.success(Unit)

            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PaymentAccountsUiAction.OnEditAccountClick)
            advanceUntilIdle()

            // Change values
            presenter.onAction(PaymentAccountsUiAction.OnAccountNameChange("Modified Name"))
            presenter.onAction(PaymentAccountsUiAction.OnAccountDescriptionChange("Modified Description"))
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountsUiAction.OnCancelAddEditAccountClick)
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertFalse(state.showEditAccountState)
            assertFalse(state.showAddAccountState)
            assertEquals("PayPal Account", state.accountNameEntry.value)
            assertEquals("user@example.com", state.accountDescriptionEntry.value)
            assertNull(state.accountNameEntry.errorMessage)
            assertNull(state.accountDescriptionEntry.errorMessage)
        }

    @Test
    fun `when delete account clicked then shows confirmation dialog`() =
        runTest(testDispatcher) {
            // Given
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(emptyList())
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountsUiAction.OnDeleteAccountClick)
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertTrue(state.showDeleteConfirmationDialog)
        }

    @Test
    fun `when cancel delete clicked then hides confirmation dialog`() =
        runTest(testDispatcher) {
            // Given
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(emptyList())
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PaymentAccountsUiAction.OnDeleteAccountClick)
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountsUiAction.OnCancelDeleteAccountClick)
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertFalse(state.showDeleteConfirmationDialog)
        }

    @Test
    fun `when cancel add account clicked with no selected account then clears fields`() =
        runTest(testDispatcher) {
            // Given
            coEvery { fiatAccountsServiceFacade.getAccounts() } returns Result.success(emptyList())
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(PaymentAccountsUiAction.OnAddAccountClick)
            advanceUntilIdle()

            presenter.onAction(PaymentAccountsUiAction.OnAccountNameChange("Test Name"))
            presenter.onAction(PaymentAccountsUiAction.OnAccountDescriptionChange("Test Description"))
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountsUiAction.OnCancelAddEditAccountClick)
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertFalse(state.showAddAccountState)
            assertEquals("", state.accountNameEntry.value)
            assertEquals("", state.accountDescriptionEntry.value)
        }
}
