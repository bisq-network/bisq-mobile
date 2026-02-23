package network.bisq.mobile.presentation.settings.payment_accounts

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.domain.data.replicated.account.fiat.UserDefinedFiatAccountPayloadVO
import network.bisq.mobile.domain.data.replicated.account.fiat.UserDefinedFiatAccountVO
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for PaymentAccountsContent using Robolectric.
 *
 * These tests verify that the PaymentAccountsContent composable renders correctly
 * for different UI states and that user interactions trigger the appropriate actions.
 */
@RunWith(AndroidJUnit4::class)
class PaymentAccountsContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockOnAction: (PaymentAccountsUiAction) -> Unit
    private lateinit var snackbarHostState: SnackbarHostState

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

    @Before
    fun setup() {
        I18nSupport.setLanguage()
        mockOnAction = mockk(relaxed = true)
        snackbarHostState = SnackbarHostState()
    }

    /**
     * Helper function to set up test content with LocalIsTest enabled.
     * Wraps content with CompositionLocalProvider and BisqTheme to avoid repetition.
     */
    private fun setTestContent(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    content()
                }
            }
        }
    }

    // ========== Empty State Tests ==========

    @Test
    fun `when empty state renders then shows no accounts info and create button`() {
        // Given
        val uiState =
            PaymentAccountsUiState(
                accounts = emptyList(),
            )

        // When
        setTestContent {
            PaymentAccountsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.noAccounts.info".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.createAccount".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when empty state and create account clicked then triggers OnAddAccountClick action`() {
        // Given
        val uiState =
            PaymentAccountsUiState(
                accounts = emptyList(),
            )

        setTestContent {
            PaymentAccountsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("paymentAccounts.createAccount".i18n())
            .performClick()

        // Then
        verify { mockOnAction(PaymentAccountsUiAction.OnAddAccountClick) }
    }

    // ========== Loading State Tests ==========

    @Test
    fun `when loading state renders then shows loading indicator`() {
        // Given
        val uiState =
            PaymentAccountsUiState(
                isLoadingAccounts = true,
            )

        // When
        setTestContent {
            PaymentAccountsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        // Then - LoadingState with CircularProgressIndicator should be displayed
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag("loading_indicator")
            .assertIsDisplayed()
    }

    // ========== Error State Tests ==========

    @Test
    fun `when error state renders then shows error message and retry button`() {
        // Given
        val uiState =
            PaymentAccountsUiState(
                isLoadingAccountsError = true,
            )

        // When
        setTestContent {
            PaymentAccountsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        // Then - ErrorState with title and error message should be displayed
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.error.title".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.error.generic".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when error state and retry clicked then triggers OnRetryLoadAccountsClick action`() {
        // Given
        val uiState =
            PaymentAccountsUiState(
                isLoadingAccountsError = true,
            )

        setTestContent {
            PaymentAccountsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When - Click retry button (ErrorState uses "mobile.action.retry" text)
        composeTestRule
            .onNodeWithText("mobile.action.retry".i18n())
            .performClick()

        // Then
        verify { mockOnAction(PaymentAccountsUiAction.OnRetryLoadAccountsClick) }
    }

    // ========== Add Account State Tests ==========

    @Test
    fun `when add account state renders then shows form fields and buttons`() {
        // Given
        val uiState =
            PaymentAccountsUiState(
                showAddAccountState = true,
            )

        // When
        setTestContent {
            PaymentAccountsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.user.paymentAccounts.createAccount.paymentAccount.label".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.legacy.accountData".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.createAccount".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("action.cancel".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when add account state and account name entered then triggers OnAccountNameChange action`() {
        // Given
        val uiState =
            PaymentAccountsUiState(
                showAddAccountState = true,
            )

        setTestContent {
            PaymentAccountsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("paymentAccounts.legacy.createAccount.accountName.prompt".i18n())
            .performTextInput("Test Account")

        // Then
        verify { mockOnAction(PaymentAccountsUiAction.OnAccountNameChange("Test Account")) }
    }

    @Test
    fun `when add account state and account description entered then triggers OnAccountDescriptionChange action`() {
        // Given
        val uiState =
            PaymentAccountsUiState(
                showAddAccountState = true,
            )

        setTestContent {
            PaymentAccountsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("paymentAccounts.legacy.createAccount.accountData.prompt".i18n())
            .performTextInput("test@example.com")

        // Then
        verify { mockOnAction(PaymentAccountsUiAction.OnAccountDescriptionChange("test@example.com")) }
    }

    @Test
    fun `when add account state and confirm clicked then triggers OnConfirmAddAccountClick action`() {
        // Given
        val uiState =
            PaymentAccountsUiState(
                showAddAccountState = true,
            )

        setTestContent {
            PaymentAccountsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("paymentAccounts.createAccount".i18n())
            .performClick()

        // Then
        verify { mockOnAction(PaymentAccountsUiAction.OnConfirmAddAccountClick) }
    }

    @Test
    fun `when add account state and cancel clicked then triggers OnCancelAddEditAccountClick action`() {
        // Given
        val uiState =
            PaymentAccountsUiState(
                showAddAccountState = true,
            )

        setTestContent {
            PaymentAccountsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("action.cancel".i18n())
            .performClick()

        // Then
        verify { mockOnAction(PaymentAccountsUiAction.OnCancelAddEditAccountClick) }
    }

    @Test
    fun `when add account state with validation errors then shows error messages`() {
        // Given
        val uiState =
            PaymentAccountsUiState(
                showAddAccountState = true,
                accountNameEntry = DataEntry(value = "", errorMessage = "Account name required"),
                accountDescriptionEntry = DataEntry(value = "", errorMessage = "Account data required"),
            )

        // When
        setTestContent {
            PaymentAccountsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("Account name required")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Account data required")
            .assertIsDisplayed()
    }

    @Test
    fun `when accounts list in edit mode renders then shows editable form and buttons`() {
        // Given
        val accounts = listOf(sampleAccount1, sampleAccount2)
        val uiState =
            PaymentAccountsUiState(
                accounts = accounts,
                selectedAccountIndex = 0,
                accountNameEntry = DataEntry(value = sampleAccount1.accountName),
                accountDescriptionEntry = DataEntry(value = sampleAccount1.accountPayload.accountData),
                showEditAccountState = true,
            )

        // When
        setTestContent {
            PaymentAccountsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.user.paymentAccounts.createAccount.paymentAccount.label".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.legacy.accountData".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("action.save".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.deleteAccount".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when accounts list in view mode renders then shows dropdown and edit button`() {
        // Given
        val accounts = listOf(sampleAccount1, sampleAccount2)
        val uiState =
            PaymentAccountsUiState(
                accounts = accounts,
                selectedAccountIndex = 0,
                accountNameEntry = DataEntry(value = sampleAccount1.accountName),
                accountDescriptionEntry = DataEntry(value = sampleAccount1.accountPayload.accountData),
                // View mode - will render BisqDropdown with LocalIsTest fallback
            )

        // When
        setTestContent {
            PaymentAccountsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.user.paymentAccounts.createAccount.paymentAccount.label".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.legacy.accountData".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("action.edit".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.legacy.createAccount.headline".i18n())
            .assertIsDisplayed()
    }

    // ========== Edit Account State Tests ==========

    @Test
    fun `when edit account state renders then shows save delete and cancel buttons`() {
        // Given
        val accounts = listOf(sampleAccount1, sampleAccount2)
        val uiState =
            PaymentAccountsUiState(
                accounts = accounts,
                selectedAccountIndex = 0,
                accountNameEntry = DataEntry(value = sampleAccount1.accountName),
                accountDescriptionEntry = DataEntry(value = sampleAccount1.accountPayload.accountData),
                showEditAccountState = true,
            )

        // When
        setTestContent {
            PaymentAccountsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("action.save".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.deleteAccount".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("action.cancel".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when edit account state and save clicked then triggers OnSaveAccountClick action`() {
        // Given
        val accounts = listOf(sampleAccount1, sampleAccount2)
        val uiState =
            PaymentAccountsUiState(
                accounts = accounts,
                selectedAccountIndex = 0,
                accountNameEntry = DataEntry(value = sampleAccount1.accountName),
                accountDescriptionEntry = DataEntry(value = sampleAccount1.accountPayload.accountData),
                showEditAccountState = true,
            )

        setTestContent {
            PaymentAccountsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("action.save".i18n())
            .performClick()

        // Then
        verify { mockOnAction(PaymentAccountsUiAction.OnSaveAccountClick) }
    }

    @Test
    fun `when edit account state and delete clicked then triggers OnDeleteAccountClick action`() {
        // Given
        val accounts = listOf(sampleAccount1, sampleAccount2)
        val uiState =
            PaymentAccountsUiState(
                accounts = accounts,
                selectedAccountIndex = 0,
                accountNameEntry = DataEntry(value = sampleAccount1.accountName),
                accountDescriptionEntry = DataEntry(value = sampleAccount1.accountPayload.accountData),
                showEditAccountState = true,
            )

        setTestContent {
            PaymentAccountsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("paymentAccounts.deleteAccount".i18n())
            .performClick()

        // Then
        verify { mockOnAction(PaymentAccountsUiAction.OnDeleteAccountClick) }
    }

    @Test
    fun `when edit account state and cancel clicked then triggers OnCancelAddEditAccountClick action`() {
        // Given
        val accounts = listOf(sampleAccount1, sampleAccount2)
        val uiState =
            PaymentAccountsUiState(
                accounts = accounts,
                selectedAccountIndex = 0,
                accountNameEntry = DataEntry(value = sampleAccount1.accountName),
                accountDescriptionEntry = DataEntry(value = sampleAccount1.accountPayload.accountData),
                showEditAccountState = true,
            )

        setTestContent {
            PaymentAccountsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("action.cancel".i18n())
            .performClick()

        // Then
        verify { mockOnAction(PaymentAccountsUiAction.OnCancelAddEditAccountClick) }
    }

    @Test
    fun `when edit account state and account name changed then triggers OnAccountNameChange action`() {
        // Given
        val accounts = listOf(sampleAccount1, sampleAccount2)
        val uiState =
            PaymentAccountsUiState(
                accounts = accounts,
                selectedAccountIndex = 0,
                accountNameEntry = DataEntry(value = sampleAccount1.accountName),
                accountDescriptionEntry = DataEntry(value = sampleAccount1.accountPayload.accountData),
                showEditAccountState = true,
            )

        setTestContent {
            PaymentAccountsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When - performTextInput prepends text in tests
        composeTestRule
            .onNodeWithText(sampleAccount1.accountName)
            .performTextInput(" Updated")

        // Then
        verify { mockOnAction(PaymentAccountsUiAction.OnAccountNameChange(" UpdatedPayPal Account")) }
    }

    @Test
    fun `when edit account state and description changed then triggers OnAccountDescriptionChange action`() {
        // Given
        val accounts = listOf(sampleAccount1, sampleAccount2)
        val uiState =
            PaymentAccountsUiState(
                accounts = accounts,
                selectedAccountIndex = 0,
                accountNameEntry = DataEntry(value = sampleAccount1.accountName),
                accountDescriptionEntry = DataEntry(value = sampleAccount1.accountPayload.accountData),
                showEditAccountState = true,
            )

        setTestContent {
            PaymentAccountsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When - performTextInput prepends text in tests
        composeTestRule
            .onNodeWithText(sampleAccount1.accountPayload.accountData)
            .performTextInput(" updated")

        // Then
        verify { mockOnAction(PaymentAccountsUiAction.OnAccountDescriptionChange(" updateduser@example.com")) }
    }

    // ========== Delete Confirmation Dialog Tests ==========

    @Test
    fun `when delete confirmation dialog shown then displays confirmation dialog`() {
        // Given
        val accounts = listOf(sampleAccount1)
        val uiState =
            PaymentAccountsUiState(
                accounts = accounts,
                selectedAccountIndex = 0,
                accountNameEntry = DataEntry(value = sampleAccount1.accountName),
                accountDescriptionEntry = DataEntry(value = sampleAccount1.accountPayload.accountData),
                showDeleteConfirmationDialog = true,
            )

        // When
        setTestContent {
            PaymentAccountsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        // Then - ConfirmationDialog should be displayed
        composeTestRule.waitForIdle()
        // ConfirmationDialog has confirm button ("confirmation.yes")
        composeTestRule
            .onNodeWithText("confirmation.yes".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when delete confirmation dialog and confirm clicked then triggers OnConfirmDeleteAccountClick action`() {
        // Given
        val accounts = listOf(sampleAccount1)
        val uiState =
            PaymentAccountsUiState(
                accounts = accounts,
                selectedAccountIndex = 0,
                accountNameEntry = DataEntry(value = sampleAccount1.accountName),
                accountDescriptionEntry = DataEntry(value = sampleAccount1.accountPayload.accountData),
                showDeleteConfirmationDialog = true,
            )

        setTestContent {
            PaymentAccountsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("confirmation.yes".i18n())
            .performClick()

        // Then
        verify { mockOnAction(PaymentAccountsUiAction.OnConfirmDeleteAccountClick) }
    }

    @Test
    fun `when delete confirmation dialog and cancel clicked then triggers OnCancelDeleteAccountClick action`() {
        // Given
        val accounts = listOf(sampleAccount1)
        val uiState =
            PaymentAccountsUiState(
                accounts = accounts,
                selectedAccountIndex = 0,
                accountNameEntry = DataEntry(value = sampleAccount1.accountName),
                accountDescriptionEntry = DataEntry(value = sampleAccount1.accountPayload.accountData),
                showDeleteConfirmationDialog = true,
            )

        setTestContent {
            PaymentAccountsContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        // When - ConfirmationDialog dismiss button uses "confirmation.no"
        composeTestRule
            .onNodeWithText("confirmation.no".i18n())
            .performClick()

        // Then
        verify { mockOnAction(PaymentAccountsUiAction.OnCancelDeleteAccountClick) }
    }
}
