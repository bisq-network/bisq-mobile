package network.bisq.mobile.presentation.create_payment_account.select_payment_method.fiat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.FiatPaymentMethodVO
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SelectFiatPaymentMethodContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var onAction: (SelectFiatPaymentMethodUiAction) -> Unit

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
    fun `when loading then shows loading indicator`() {
        // Given
        val uiState = createUiState(isLoading = true)

        // When
        setTestContent {
            SelectFiatPaymentMethodContent(
                uiState = uiState,
                onAction = onAction,
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
            SelectFiatPaymentMethodContent(
                uiState = uiState,
                onAction = onAction,
            )
        }
        composeTestRule.waitForIdle()

        // When
        composeTestRule.onNodeWithText("mobile.action.retry".i18n()).performClick()

        // Then
        composeTestRule.onNodeWithText("mobile.error.title".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.error.generic".i18n()).assertIsDisplayed()
        verify { onAction(SelectFiatPaymentMethodUiAction.OnRetryLoadPaymentMethodsClick) }
    }

    @Test
    fun `when fiat loaded then renders fiat methods`() {
        // Given
        val fiatMethods =
            listOf(
                sampleFiatMethod(name = "SEPA", paymentType = PaymentTypeVO.SEPA),
                sampleFiatMethod(name = "Zelle", paymentType = PaymentTypeVO.ZELLE),
            )
        val uiState = createUiState(paymentMethods = fiatMethods)

        // When
        setTestContent {
            SelectFiatPaymentMethodContent(
                uiState = uiState,
                onAction = onAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.user.paymentAccounts.fiat.select".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("SEPA").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zelle").assertIsDisplayed()
    }

    @Test
    fun `when fiat method clicked then triggers fiat click action`() {
        // Given
        val fiatMethod = sampleFiatMethod(name = "SEPA")
        val uiState = createUiState(paymentMethods = listOf(fiatMethod))
        setTestContent {
            SelectFiatPaymentMethodContent(
                uiState = uiState,
                onAction = onAction,
            )
        }
        composeTestRule.waitForIdle()

        // When
        composeTestRule.onNodeWithText("SEPA").performClick()

        // Then
        verify { onAction(SelectFiatPaymentMethodUiAction.OnPaymentMethodClick(fiatMethod)) }
    }

    @Test
    fun `when no method selected then next button is disabled`() {
        // Given
        val uiState = createUiState()

        // When
        setTestContent {
            SelectFiatPaymentMethodContent(
                uiState = uiState,
                onAction = onAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("action.next".i18n()).assertIsNotEnabled()
    }

    private fun createUiState(
        paymentMethods: List<FiatPaymentMethodVO> = emptyList(),
        selectedPaymentMethod: FiatPaymentMethodVO? = null,
        isLoading: Boolean = false,
        isError: Boolean = false,
        searchQuery: String = "",
        activeRiskFilter: FiatPaymentMethodChargebackRiskVO? = null,
    ): SelectFiatPaymentMethodUiState =
        SelectFiatPaymentMethodUiState(
            paymentMethods = paymentMethods,
            selectedPaymentMethod = selectedPaymentMethod,
            isLoading = isLoading,
            isError = isError,
            searchQuery = searchQuery,
            activeRiskFilter = activeRiskFilter,
        )

    private fun sampleFiatMethod(
        name: String = "SEPA",
        paymentType: PaymentTypeVO = PaymentTypeVO.SEPA,
        supportedCurrencyCodes: String = "EUR",
        countryNames: String = "Germany",
        chargebackRisk: FiatPaymentMethodChargebackRiskVO? = FiatPaymentMethodChargebackRiskVO.VERY_LOW,
    ): FiatPaymentMethodVO =
        FiatPaymentMethodVO(
            paymentType = paymentType,
            name = name,
            supportedCurrencyCodes = supportedCurrencyCodes,
            countryNames = countryNames,
            chargebackRisk = chargebackRisk,
        )
}
