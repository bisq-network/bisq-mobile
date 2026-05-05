package network.bisq.mobile.presentation.settings.payment_accounts_musig.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.domain.model.account.PaymentAccountPayload
import network.bisq.mobile.domain.model.account.crypto.MoneroAccount
import network.bisq.mobile.domain.model.account.crypto.MoneroAccountPayload
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccount
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccountPayload
import network.bisq.mobile.domain.model.account.fiat.ZelleAccount
import network.bisq.mobile.domain.model.account.fiat.ZelleAccountPayload
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaymentAccountMusigDetailContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var onAction: (PaymentAccountMusigDetailUiAction) -> Unit

    @Before
    fun setup() {
        I18nSupport.setLanguage()
        onAction = mockk(relaxed = true)
    }

    private fun setTestContent(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    content()
                }
            }
        }
    }

    @Test
    fun `when account missing then shows generic error and no delete button`() {
        // Given
        val uiState = createUiState(isAccountMissing = true)

        // When
        setTestContent {
            PaymentAccountMusigDetailContent(
                uiState = uiState,
                onAction = onAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.error.title".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.error.generic".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("mobile.action.delete".i18n())
            .assertCountEquals(0)
    }

    @Test
    fun `when zelle account present then shows shared summary fields and delete button`() {
        // Given
        val uiState = createUiState(paymentAccount = sampleZelleAccount())

        // When
        setTestContent {
            PaymentAccountMusigDetailContent(
                uiState = uiState,
                onAction = onAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.summary.accountNameOverlay.accountName.description".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("alice@example.com")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.holderName".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.action.delete".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when monero account present then shows monero detail labels`() {
        // Given
        val uiState = createUiState(paymentAccount = sampleMoneroAccount())

        // When
        setTestContent {
            PaymentAccountMusigDetailContent(
                uiState = uiState,
                onAction = onAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.address".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.xmr.useSubAddresses.switch".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when user defined fiat account present then shows custom account data label`() {
        // Given
        val uiState = createUiState(paymentAccount = sampleUserDefinedAccount())

        // When
        setTestContent {
            PaymentAccountMusigDetailContent(
                uiState = uiState,
                onAction = onAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.userDefined.accountData".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when delete button clicked then triggers delete account action`() {
        // Given
        val uiState = createUiState(paymentAccount = sampleZelleAccount())
        setTestContent {
            PaymentAccountMusigDetailContent(
                uiState = uiState,
                onAction = onAction,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("mobile.action.delete".i18n())
            .performClick()

        // Then
        verify { onAction(PaymentAccountMusigDetailUiAction.OnDeleteAccountClick) }
    }

    @Test
    fun `when delete confirmation dialog shown then displays confirm and cancel actions`() {
        // Given
        val uiState =
            createUiState(
                paymentAccount = sampleZelleAccount(),
                showDeleteConfirmationDialog = true,
            )

        // When
        setTestContent {
            PaymentAccountMusigDetailContent(
                uiState = uiState,
                onAction = onAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.user.paymentAccounts.delete.confirmation".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("confirmation.yes".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("confirmation.no".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when delete confirmation and confirm clicked then triggers confirm action`() {
        // Given
        val uiState =
            createUiState(
                paymentAccount = sampleZelleAccount(),
                showDeleteConfirmationDialog = true,
            )
        setTestContent {
            PaymentAccountMusigDetailContent(
                uiState = uiState,
                onAction = onAction,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("confirmation.yes".i18n())
            .performClick()

        // Then
        verify { onAction(PaymentAccountMusigDetailUiAction.OnConfirmDeleteAccountClick) }
    }

    @Test
    fun `when delete confirmation and cancel clicked then triggers cancel action`() {
        // Given
        val uiState =
            createUiState(
                paymentAccount = sampleZelleAccount(),
                showDeleteConfirmationDialog = true,
            )
        setTestContent {
            PaymentAccountMusigDetailContent(
                uiState = uiState,
                onAction = onAction,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("confirmation.no".i18n())
            .performClick()

        // Then
        verify { onAction(PaymentAccountMusigDetailUiAction.OnCancelDeleteAccountClick) }
    }

    @Test
    fun `when unsupported account provided then no branch specific labels are rendered`() {
        // Given
        val uiState = createUiState(paymentAccount = sampleUnsupportedAccount())

        // When
        setTestContent {
            PaymentAccountMusigDetailContent(
                uiState = uiState,
                onAction = onAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.holderName".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.address".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.userDefined.accountData".i18n())
            .assertCountEquals(0)
    }

    private fun createUiState(
        paymentAccount: PaymentAccount? = null,
        isAccountMissing: Boolean = false,
        showDeleteConfirmationDialog: Boolean = false,
    ): PaymentAccountMusigDetailUiState =
        PaymentAccountMusigDetailUiState(
            paymentAccount = paymentAccount,
            isAccountMissing = isAccountMissing,
            showDeleteConfirmationDialog = showDeleteConfirmationDialog,
        )

    private fun sampleZelleAccount(): ZelleAccount =
        ZelleAccount(
            accountName = "Alice Doe",
            accountPayload =
                ZelleAccountPayload(
                    holderName = "Alice Doe",
                    emailOrMobileNr = "alice@example.com",
                    paymentMethodName = "Zelle",
                    currency = "USD",
                    country = "United States",
                ),
            creationDate = null,
            tradeLimitInfo = null,
            tradeDuration = null,
        )

    private fun sampleMoneroAccount(): MoneroAccount =
        MoneroAccount(
            accountName = "Monero Main",
            accountPayload =
                MoneroAccountPayload(
                    address = "48A_MAIN_ADDRESS",
                    isInstant = false,
                    useSubAddresses = false,
                    currencyName = "Monero",
                    currencyCode = "XMR",
                    supportAutoConf = true,
                ),
            creationDate = null,
            tradeLimitInfo = null,
            tradeDuration = null,
        )

    private fun sampleUserDefinedAccount(): UserDefinedFiatAccount =
        UserDefinedFiatAccount(
            accountName = "Custom Account",
            accountPayload =
                UserDefinedFiatAccountPayload(
                    paymentMethodName = "Bank Transfer",
                    accountData = "IBAN: DE89370400440532013000",
                    currency = "EUR",
                    country = "Germany",
                ),
            creationDate = null,
            tradeLimitInfo = null,
            tradeDuration = null,
        )

    private fun sampleUnsupportedAccount(): PaymentAccount =
        object : PaymentAccount {
            override val accountName: String = "Unsupported"
            override val accountPayload: PaymentAccountPayload = object : PaymentAccountPayload {}
            override val creationDate: String? = null
            override val tradeLimitInfo: String? = null
            override val tradeDuration: String? = null
        }
}
