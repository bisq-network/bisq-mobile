package network.bisq.mobile.presentation.create_payment_account.payment_account_form

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.PaymentAccountFormContent
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.FiatPaymentMethodVO
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class PaymentAccountFormContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
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
    fun `renders form shell and payment method metadata`() {
        setTestContent {
            PaymentAccountFormContent(
                paymentMethod = samplePaymentMethod(),
                accountName = "My account",
                accountNameError = null,
                onAccountNameChange = {},
                isNextEnabled = true,
                onNextClick = {},
            )
        }

        composeTestRule.onNodeWithText("mobile.user.paymentAccounts.details".i18n()).assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.summary.accountNameOverlay.accountName.description".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.user.paymentAccounts.details.accountName.helper".i18n())
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Zelle").assertIsDisplayed()
        composeTestRule.onNodeWithText("action.next".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when next disabled then next button is disabled`() {
        setTestContent {
            PaymentAccountFormContent(
                paymentMethod = samplePaymentMethod(),
                accountName = "",
                accountNameError = null,
                onAccountNameChange = {},
                isNextEnabled = false,
                onNextClick = {},
            )
        }

        composeTestRule.onNodeWithText("action.next".i18n()).assertIsNotEnabled()
    }

    @Test
    fun `when next enabled then clicking next invokes callback`() {
        var nextClicks = 0
        setTestContent {
            PaymentAccountFormContent(
                paymentMethod = samplePaymentMethod(),
                accountName = "Ready",
                accountNameError = null,
                onAccountNameChange = {},
                isNextEnabled = true,
                onNextClick = { nextClicks++ },
            )
        }

        composeTestRule.onNodeWithText("action.next".i18n()).assertIsEnabled().performClick()
        assertEquals(1, nextClicks)
    }

    @Test
    fun `typing account name invokes change callback`() {
        var latestValue = ""
        setTestContent {
            PaymentAccountFormContent(
                paymentMethod = samplePaymentMethod(),
                accountName = "",
                accountNameError = null,
                onAccountNameChange = { latestValue = it },
                isNextEnabled = true,
                onNextClick = {},
            )
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("Updated")
        assertEquals("Updated", latestValue)
    }

    @Test
    fun `when account name has error then shows error instead of helper`() {
        setTestContent {
            PaymentAccountFormContent(
                paymentMethod = samplePaymentMethod(),
                accountName = "a",
                accountNameError = "validation.tooShortOrTooLong".i18n(3, 100),
                onAccountNameChange = {},
                isNextEnabled = true,
                onNextClick = {},
            )
        }

        composeTestRule.onNodeWithText("validation.tooShortOrTooLong".i18n(3, 100)).assertIsDisplayed()
    }

    @Test
    fun `renders method specific slot content`() {
        setTestContent {
            PaymentAccountFormContent(
                paymentMethod = samplePaymentMethod(),
                accountName = "My account",
                accountNameError = null,
                onAccountNameChange = {},
                isNextEnabled = true,
                onNextClick = {},
                formContent = {
                    Text("Method-specific form preview")
                },
            )
        }

        composeTestRule.onNodeWithText("Method-specific form preview").assertIsDisplayed()
    }

    private fun samplePaymentMethod(): FiatPaymentMethodVO =
        FiatPaymentMethodVO(
            paymentType = PaymentTypeVO.ZELLE,
            name = "Zelle",
            supportedCurrencyCodes = "USD",
            countryNames = "United States",
            chargebackRisk = null,
        )
}
