package network.bisq.mobile.presentation.create_payment_account.select_payment_method

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
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
import network.bisq.mobile.presentation.common.ui.navigation.types.PaymentAccountType
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.CryptoPaymentMethodVO
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.FiatPaymentMethodVO
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SelectPaymentMethodContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var onAction: (SelectPaymentMethodUiAction) -> Unit
    private lateinit var onContinue: (PaymentMethodVO) -> Unit

    @Before
    fun setup() {
        I18nSupport.setLanguage()
        onAction = mockk(relaxed = true)
        onContinue = mockk(relaxed = true)
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
    fun `when loading then shows loading indicator`() {
        // Given
        val uiState = createUiState(isLoading = true)

        // When
        setTestContent {
            SelectPaymentMethodContent(
                uiState = uiState,
                accountType = PaymentAccountType.FIAT,
                onAction = onAction,
                onContinue = onContinue,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
    }

    @Test
    fun `when error then shows error state and retry click triggers retry action`() {
        // Given
        val uiState = createUiState(isError = true)
        setTestContent {
            SelectPaymentMethodContent(
                uiState = uiState,
                accountType = PaymentAccountType.FIAT,
                onAction = onAction,
                onContinue = onContinue,
            )
        }
        composeTestRule.waitForIdle()

        // When
        composeTestRule.onNodeWithText("mobile.action.retry".i18n()).performClick()

        // Then
        composeTestRule.onNodeWithText("mobile.error.title".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.error.generic".i18n()).assertIsDisplayed()
        verify { onAction(SelectPaymentMethodUiAction.OnRetryLoadPaymentMethodsClick) }
    }

    @Test
    fun `when fiat loaded then renders fiat methods and hides crypto rows`() {
        // Given
        val fiatMethods =
            listOf(
                sampleFiatMethod(name = "SEPA", paymentMethod = PaymentMethodVO.SEPA),
                sampleFiatMethod(name = "Zelle", paymentMethod = PaymentMethodVO.ZELLE),
            )
        val uiState = createUiState(fiatPaymentMethods = fiatMethods)

        // When
        setTestContent {
            SelectPaymentMethodContent(
                uiState = uiState,
                accountType = PaymentAccountType.FIAT,
                onAction = onAction,
                onContinue = onContinue,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.user.paymentAccounts.fiat.select".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("SEPA").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zelle").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("XMR", substring = true).assertCountEquals(0)
    }

    @Test
    fun `when crypto loaded then renders crypto methods`() {
        // Given
        val cryptoMethods =
            listOf(
                sampleCryptoMethod(code = "XMR", name = "Monero", paymentMethod = PaymentMethodVO.XMR),
                sampleCryptoMethod(code = "LTC", name = "Litecoin", paymentMethod = PaymentMethodVO.LTC),
            )
        val uiState = createUiState(cryptoPaymentMethods = cryptoMethods)

        // When
        setTestContent {
            SelectPaymentMethodContent(
                uiState = uiState,
                accountType = PaymentAccountType.CRYPTO,
                onAction = onAction,
                onContinue = onContinue,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.user.paymentAccounts.crypto.select".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("Monero").assertIsDisplayed()
        composeTestRule.onNodeWithText("Litecoin").assertIsDisplayed()
    }

    @Test
    fun `when fiat method clicked then triggers fiat click action`() {
        // Given
        val fiatMethod = sampleFiatMethod(name = "SEPA")
        val uiState = createUiState(fiatPaymentMethods = listOf(fiatMethod))
        setTestContent {
            SelectPaymentMethodContent(
                uiState = uiState,
                accountType = PaymentAccountType.FIAT,
                onAction = onAction,
                onContinue = onContinue,
            )
        }
        composeTestRule.waitForIdle()

        // When
        composeTestRule.onNodeWithText("SEPA").performClick()

        // Then
        verify { onAction(SelectPaymentMethodUiAction.OnFiatPaymentMethodClick(fiatMethod)) }
    }

    @Test
    fun `when crypto method clicked then triggers crypto click action`() {
        // Given
        val cryptoMethod = sampleCryptoMethod(code = "XMR", name = "Monero")
        val uiState = createUiState(cryptoPaymentMethods = listOf(cryptoMethod))
        setTestContent {
            SelectPaymentMethodContent(
                uiState = uiState,
                accountType = PaymentAccountType.CRYPTO,
                onAction = onAction,
                onContinue = onContinue,
            )
        }
        composeTestRule.waitForIdle()

        // When
        composeTestRule.onNodeWithText("XMR").performClick()

        // Then
        verify { onAction(SelectPaymentMethodUiAction.OnCryptoPaymentMethodClick(cryptoMethod)) }
    }

    @Test
    fun `when no method selected then next button is disabled`() {
        // Given
        val uiState = createUiState()

        // When
        setTestContent {
            SelectPaymentMethodContent(
                uiState = uiState,
                accountType = PaymentAccountType.FIAT,
                onAction = onAction,
                onContinue = onContinue,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("action.next".i18n()).assertIsNotEnabled()
    }

    @Test
    fun `when fiat method selected and next clicked then triggers continue with selected payment method`() {
        // Given
        val selectedFiatMethod = sampleFiatMethod(name = "SEPA", paymentMethod = PaymentMethodVO.SEPA)
        val uiState = createUiState(selectedFiatPaymentMethod = selectedFiatMethod)
        setTestContent {
            SelectPaymentMethodContent(
                uiState = uiState,
                accountType = PaymentAccountType.FIAT,
                onAction = onAction,
                onContinue = onContinue,
            )
        }
        composeTestRule.waitForIdle()

        // When
        composeTestRule.onNodeWithText("action.next".i18n()).performClick()

        // Then
        verify { onContinue(PaymentMethodVO.SEPA) }
    }

    @Test
    fun `when crypto method selected and next clicked then triggers continue with selected payment method`() {
        // Given
        val selectedCryptoMethod = sampleCryptoMethod(code = "XMR", name = "Monero", paymentMethod = PaymentMethodVO.XMR)
        val uiState = createUiState(selectedCryptoPaymentMethod = selectedCryptoMethod)
        setTestContent {
            SelectPaymentMethodContent(
                uiState = uiState,
                accountType = PaymentAccountType.CRYPTO,
                onAction = onAction,
                onContinue = onContinue,
            )
        }
        composeTestRule.waitForIdle()

        // When
        composeTestRule.onNodeWithText("action.next".i18n()).performClick()

        // Then
        verify { onContinue(PaymentMethodVO.XMR) }
    }

    private fun createUiState(
        fiatPaymentMethods: List<FiatPaymentMethodVO> = emptyList(),
        cryptoPaymentMethods: List<CryptoPaymentMethodVO> = emptyList(),
        selectedFiatPaymentMethod: FiatPaymentMethodVO? = null,
        selectedCryptoPaymentMethod: CryptoPaymentMethodVO? = null,
        isLoading: Boolean = false,
        isError: Boolean = false,
        searchQuery: String = "",
        activeRiskFilter: FiatPaymentMethodChargebackRiskVO? = null,
    ): SelectPaymentMethodUiState =
        SelectPaymentMethodUiState(
            fiatPaymentMethods = fiatPaymentMethods,
            cryptoPaymentMethods = cryptoPaymentMethods,
            selectedFiatPaymentMethod = selectedFiatPaymentMethod,
            selectedCryptoPaymentMethod = selectedCryptoPaymentMethod,
            isLoading = isLoading,
            isError = isError,
            searchQuery = searchQuery,
            activeRiskFilter = activeRiskFilter,
        )

    private fun sampleFiatMethod(
        name: String = "SEPA",
        paymentMethod: PaymentMethodVO = PaymentMethodVO.SEPA,
        supportedCurrencyCodes: String = "EUR",
        countryNames: String = "Germany",
        chargebackRisk: FiatPaymentMethodChargebackRiskVO? = FiatPaymentMethodChargebackRiskVO.VERY_LOW,
    ): FiatPaymentMethodVO =
        FiatPaymentMethodVO(
            paymentMethod = paymentMethod,
            name = name,
            supportedCurrencyCodes = supportedCurrencyCodes,
            countryNames = countryNames,
            chargebackRisk = chargebackRisk,
        )

    private fun sampleCryptoMethod(
        code: String = "XMR",
        name: String = "Monero",
        paymentMethod: PaymentMethodVO = PaymentMethodVO.XMR,
    ): CryptoPaymentMethodVO =
        CryptoPaymentMethodVO(
            paymentMethod = paymentMethod,
            code = code,
            name = name,
        )
}
