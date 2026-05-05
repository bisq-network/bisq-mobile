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
import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.AccountFormUiAction
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

    val sampleZellePaymentMethod: FiatPaymentMethodVO =
        FiatPaymentMethodVO(
            paymentType = PaymentTypeVO.ZELLE,
            name = "Zelle",
            supportedCurrencyCodes = "USD",
            countryNames = "United States",
            chargebackRisk = FiatPaymentMethodChargebackRiskVO.MODERATE,
            tradeLimitInfo = "5000.00",
            tradeDuration = "4 days",
        )

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
                paymentMethod = sampleZellePaymentMethod,
                accountNameEntry = DataEntry(value = "My account"),
                onAction = {},
                isNextEnabled = true,
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
                paymentMethod = sampleZellePaymentMethod,
                accountNameEntry = DataEntry(value = ""),
                onAction = {},
                isNextEnabled = false,
            )
        }

        composeTestRule.onNodeWithText("action.next".i18n()).assertIsNotEnabled()
    }

    @Test
    fun `when next enabled then clicking next invokes action`() {
        var latestAction: AccountFormUiAction? = null
        setTestContent {
            PaymentAccountFormContent(
                paymentMethod = sampleZellePaymentMethod,
                accountNameEntry = DataEntry(value = "Ready"),
                onAction = { action -> latestAction = action },
                isNextEnabled = true,
            )
        }

        composeTestRule.onNodeWithText("action.next".i18n()).assertIsEnabled().performClick()
        assertEquals(AccountFormUiAction.OnNextClick, latestAction)
    }

    @Test
    fun `typing account name emits unique account name change action`() {
        var latestAction: AccountFormUiAction? = null
        setTestContent {
            PaymentAccountFormContent(
                paymentMethod = sampleZellePaymentMethod,
                accountNameEntry = DataEntry(value = ""),
                onAction = { action -> latestAction = action },
                isNextEnabled = true,
            )
        }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("Updated")
        assertEquals(AccountFormUiAction.OnUniqueAccountNameChange("Updated"), latestAction)
    }

    @Test
    fun `when account name has error then shows error instead of helper`() {
        setTestContent {
            PaymentAccountFormContent(
                paymentMethod = sampleZellePaymentMethod,
                accountNameEntry =
                    DataEntry(
                        value = "a",
                        errorMessage = "validation.tooShortOrTooLong".i18n(3, 100),
                    ),
                onAction = {},
                isNextEnabled = true,
            )
        }

        composeTestRule.onNodeWithText("validation.tooShortOrTooLong".i18n(3, 100)).assertIsDisplayed()
    }

    @Test
    fun `renders method specific slot content`() {
        setTestContent {
            PaymentAccountFormContent(
                paymentMethod = sampleZellePaymentMethod,
                accountNameEntry = DataEntry(value = "My account"),
                onAction = {},
                isNextEnabled = true,
                formContent = {
                    Text("Method-specific form preview")
                },
            )
        }

        composeTestRule.onNodeWithText("Method-specific form preview").assertIsDisplayed()
    }
}
