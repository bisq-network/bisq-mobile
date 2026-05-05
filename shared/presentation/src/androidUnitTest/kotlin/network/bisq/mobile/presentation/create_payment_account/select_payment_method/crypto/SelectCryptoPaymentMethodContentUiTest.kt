package network.bisq.mobile.presentation.create_payment_account.select_payment_method.crypto

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
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.CryptoPaymentMethodVO
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SelectCryptoPaymentMethodContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var onAction: (SelectCryptoPaymentMethodUiAction) -> Unit

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
            SelectCryptoPaymentMethodContent(
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
            SelectCryptoPaymentMethodContent(
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
        verify { onAction(SelectCryptoPaymentMethodUiAction.OnRetryLoadPaymentMethodsClick) }
    }

    @Test
    fun `when crypto loaded then renders crypto methods`() {
        // Given
        val cryptoMethods =
            listOf(
                sampleCryptoMethod(code = "XMR", name = "Monero", paymentType = PaymentTypeVO.XMR),
                sampleCryptoMethod(code = "LTC", name = "Litecoin", paymentType = PaymentTypeVO.LTC),
            )
        val uiState = createUiState(paymentMethods = cryptoMethods)

        // When
        setTestContent {
            SelectCryptoPaymentMethodContent(
                uiState = uiState,
                onAction = onAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.user.paymentAccounts.crypto.select".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("Monero").assertIsDisplayed()
        composeTestRule.onNodeWithText("Litecoin").assertIsDisplayed()
    }

    @Test
    fun `when crypto method clicked then triggers crypto click action`() {
        // Given
        val cryptoMethod = sampleCryptoMethod(code = "XMR", name = "Monero", paymentType = PaymentTypeVO.XMR)
        val uiState = createUiState(paymentMethods = listOf(cryptoMethod))
        setTestContent {
            SelectCryptoPaymentMethodContent(
                uiState = uiState,
                onAction = onAction,
            )
        }
        composeTestRule.waitForIdle()

        // When
        composeTestRule.onNodeWithText("XMR").performClick()

        // Then
        verify { onAction(SelectCryptoPaymentMethodUiAction.OnPaymentMethodClick(cryptoMethod)) }
    }

    @Test
    fun `when no method selected then next button is disabled`() {
        // Given
        val uiState = createUiState()

        // When
        setTestContent {
            SelectCryptoPaymentMethodContent(
                uiState = uiState,
                onAction = onAction,
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("action.next".i18n()).assertIsNotEnabled()
    }

    private fun createUiState(
        paymentMethods: List<CryptoPaymentMethodVO> = emptyList(),
        selectedPaymentMethod: CryptoPaymentMethodVO? = null,
        isLoading: Boolean = false,
        isError: Boolean = false,
        searchQuery: String = "",
    ): SelectCryptoPaymentMethodUiState =
        SelectCryptoPaymentMethodUiState(
            paymentMethods = paymentMethods,
            selectedPaymentMethod = selectedPaymentMethod,
            isLoading = isLoading,
            isError = isError,
            searchQuery = searchQuery,
        )

    private fun sampleCryptoMethod(
        code: String,
        name: String,
        paymentType: PaymentTypeVO,
    ): CryptoPaymentMethodVO =
        CryptoPaymentMethodVO(
            paymentType = paymentType,
            code = code,
            name = name,
            supportAutoConf = false,
            tradeDuration = EMPTY_STRING,
            tradeLimitInfo = EMPTY_STRING,
        )
}
