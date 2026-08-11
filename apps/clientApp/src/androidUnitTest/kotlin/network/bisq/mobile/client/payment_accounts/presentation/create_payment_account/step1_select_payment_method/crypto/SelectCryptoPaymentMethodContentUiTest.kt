package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step1_select_payment_method.crypto

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.client.common.presentation.model.account.CryptoPaymentMethodVO
import network.bisq.mobile.client.common.presentation.model.account.PaymentTypeVO
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import org.robolectric.annotation.Config

@Config(application = TestApplication::class)
class SelectCryptoPaymentMethodContentUiTest : BisqComposeUiTestBase() {
    private lateinit var onAction: (SelectCryptoPaymentMethodUiAction) -> Unit

    override fun setUpUiTest() {
        super.setUpUiTest()
        onAction = mockk(relaxed = true)
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
