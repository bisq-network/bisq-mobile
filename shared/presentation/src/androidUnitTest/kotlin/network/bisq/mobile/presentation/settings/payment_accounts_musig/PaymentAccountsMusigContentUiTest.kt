package network.bisq.mobile.presentation.settings.payment_accounts_musig

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.common.model.account.PaymentMethodVO
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.settings.payment_accounts_musig.model.CryptoAccountVO
import network.bisq.mobile.presentation.settings.payment_accounts_musig.model.FiatAccountVO
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaymentAccountsMusigContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var onAction: (PaymentAccountsMusigUiAction) -> Unit

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
    fun `when loading accounts then shows loading indicator`() {
        // Given
        val uiState = createUiState(isLoadingAccounts = true)

        // When
        setTestContent {
            PaymentAccountsMusigContent(
                uiState = uiState,
                onAction = onAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag("loading_indicator")
            .assertIsDisplayed()
    }

    @Test
    fun `when loading accounts error then shows error state content`() {
        // Given
        val uiState = createUiState(isLoadingAccountsError = true)

        // When
        setTestContent {
            PaymentAccountsMusigContent(
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
    }

    @Test
    fun `when loading accounts error and retry clicked then triggers retry action`() {
        // Given
        val uiState = createUiState(isLoadingAccountsError = true)
        setTestContent {
            PaymentAccountsMusigContent(
                uiState = uiState,
                onAction = onAction,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("mobile.action.retry".i18n())
            .performClick()

        // Then
        verify { onAction(PaymentAccountsMusigUiAction.OnRetryLoadAccountsClick) }
    }

    @Test
    fun `when fiat tab has no accounts then shows empty info and fiat create button text`() {
        // Given
        val uiState = createUiState(selectedTab = PaymentAccountTab.FIAT)

        // When
        setTestContent {
            PaymentAccountsMusigContent(
                uiState = uiState,
                onAction = onAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.noAccounts.info".i18n(), substring = true)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.createAccount".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when crypto tab has no accounts then shows empty info and crypto create button text`() {
        // Given
        val uiState = createUiState(selectedTab = PaymentAccountTab.CRYPTO)

        // When
        setTestContent {
            PaymentAccountsMusigContent(
                uiState = uiState,
                onAction = onAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.noAccounts.info".i18n(), substring = true)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.createAccount".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when fiat tab has accounts then renders fiat account names`() {
        // Given
        val fiatAccounts =
            listOf(
                sampleFiatAccount(accountName = "SEPA Personal"),
                sampleFiatAccount(accountName = "Zelle Family", paymentMethod = PaymentMethodVO.ZELLE),
            )
        val uiState = createUiState(selectedTab = PaymentAccountTab.FIAT, fiatAccounts = fiatAccounts)

        // When
        setTestContent {
            PaymentAccountsMusigContent(
                uiState = uiState,
                onAction = onAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("SEPA Personal")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Zelle Family")
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Monero", substring = true)
            .assertCountEquals(0)
    }

    @Test
    fun `when crypto tab has accounts then renders crypto account names and address substring`() {
        // Given
        val cryptoAccounts =
            listOf(
                sampleCryptoAccount(
                    accountName = "Monero Main",
                    currencyName = "Monero",
                    address = "84ABcdXy12pqRstUvw3456EfGh7890JKLMnOPQ",
                ),
                sampleCryptoAccount(
                    accountName = "Ethereum Wallet",
                    currencyName = "Ethereum",
                    paymentMethod = PaymentMethodVO.ETH,
                ),
            )
        val uiState = createUiState(selectedTab = PaymentAccountTab.CRYPTO, cryptoAccounts = cryptoAccounts)

        // When
        setTestContent {
            PaymentAccountsMusigContent(
                uiState = uiState,
                onAction = onAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("Monero Main")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Ethereum Wallet")
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("84ABcdXy12pq", substring = true)
            .assertCountEquals(2)
    }

    @Test
    fun `when fiat tab and create clicked then triggers add fiat action`() {
        // Given
        val uiState = createUiState(selectedTab = PaymentAccountTab.FIAT)
        setTestContent {
            PaymentAccountsMusigContent(
                uiState = uiState,
                onAction = onAction,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("paymentAccounts.createAccount".i18n())
            .performClick()

        // Then
        verify { onAction(PaymentAccountsMusigUiAction.OnAddFiatAccountClick) }
    }

    @Test
    fun `when crypto tab and create clicked then triggers add crypto action`() {
        // Given
        val uiState = createUiState(selectedTab = PaymentAccountTab.CRYPTO)
        setTestContent {
            PaymentAccountsMusigContent(
                uiState = uiState,
                onAction = onAction,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.createAccount".i18n())
            .performClick()

        // Then
        verify { onAction(PaymentAccountsMusigUiAction.OnAddCryptoAccountClick) }
    }

    @Test
    fun `when crypto tab selected then triggers tab select action`() {
        // Given
        val uiState = createUiState(selectedTab = PaymentAccountTab.FIAT)
        setTestContent {
            PaymentAccountsMusigContent(
                uiState = uiState,
                onAction = onAction,
            )
        }

        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText(PaymentAccountTab.CRYPTO.titleKey.i18n())
            .performClick()

        // Then
        verify { onAction(PaymentAccountsMusigUiAction.OnTabSelect(PaymentAccountTab.CRYPTO)) }
    }

    @Test
    fun `when delete confirmation dialog shown then displays confirm and cancel buttons`() {
        // Given
        val uiState = createUiState(showDeleteConfirmationDialog = true)

        // When
        setTestContent {
            PaymentAccountsMusigContent(
                uiState = uiState,
                onAction = onAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("confirmation.yes".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("confirmation.no".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when delete confirmation dialog and confirm clicked then triggers confirm delete action`() {
        // Given
        val uiState = createUiState(showDeleteConfirmationDialog = true)
        setTestContent {
            PaymentAccountsMusigContent(
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
        verify { onAction(PaymentAccountsMusigUiAction.OnConfirmDeleteAccountClick) }
    }

    @Test
    fun `when delete confirmation dialog and cancel clicked then triggers cancel delete action`() {
        // Given
        val uiState = createUiState(showDeleteConfirmationDialog = true)
        setTestContent {
            PaymentAccountsMusigContent(
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
        verify { onAction(PaymentAccountsMusigUiAction.OnCancelDeleteAccountClick) }
    }

    private fun createUiState(
        fiatAccounts: List<FiatAccountVO> = emptyList(),
        cryptoAccounts: List<CryptoAccountVO> = emptyList(),
        isLoadingAccounts: Boolean = false,
        isLoadingAccountsError: Boolean = false,
        selectedTab: PaymentAccountTab = PaymentAccountTab.FIAT,
        showDeleteConfirmationDialog: Boolean = false,
    ): PaymentAccountsMusigUiState =
        PaymentAccountsMusigUiState(
            fiatAccounts = fiatAccounts,
            cryptoAccounts = cryptoAccounts,
            isLoadingAccounts = isLoadingAccounts,
            isLoadingAccountsError = isLoadingAccountsError,
            selectedTab = selectedTab,
            showDeleteConfirmationDialog = showDeleteConfirmationDialog,
        )

    private fun sampleFiatAccount(
        accountName: String = "SEPA Personal",
        chargebackRisk: FiatPaymentMethodChargebackRiskVO = FiatPaymentMethodChargebackRiskVO.LOW,
        paymentMethod: PaymentMethodVO = PaymentMethodVO.SEPA,
        paymentMethodName: String = "Sepa",
        country: String = "Germany",
        currency: String = "EUR (Euro)",
    ): FiatAccountVO =
        FiatAccountVO(
            accountName = accountName,
            chargebackRisk = chargebackRisk,
            paymentMethod = paymentMethod,
            paymentMethodName = paymentMethodName,
            country = country,
            currency = currency,
        )

    private fun sampleCryptoAccount(
        accountName: String = "Monero Main",
        currencyName: String = "Monero",
        address: String = "84ABcdXy12pqRstUvw3456EfGh7890JKLMnOPQ",
        paymentMethod: PaymentMethodVO = PaymentMethodVO.XMR,
    ): CryptoAccountVO =
        CryptoAccountVO(
            accountName = accountName,
            currencyName = currencyName,
            address = address,
            paymentMethod = paymentMethod,
        )
}
