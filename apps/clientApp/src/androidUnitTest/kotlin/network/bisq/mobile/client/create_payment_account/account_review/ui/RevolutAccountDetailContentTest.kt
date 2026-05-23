package network.bisq.mobile.client.create_payment_account.account_review.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.client.create_payment_account.account_review.ui.revolut.RevolutAccountDetailContent
import network.bisq.mobile.domain.model.account.fiat.FiatCurrency
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.domain.model.account.fiat.RevolutAccount
import network.bisq.mobile.domain.model.account.fiat.RevolutAccountPayload
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(application = TestApplication::class)
@RunWith(AndroidJUnit4::class)
class RevolutAccountDetailContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

    private fun setTestContent(account: RevolutAccount) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    RevolutAccountDetailContent(account = account)
                }
            }
        }
    }

    @Test
    fun `when revolut review renders then shows base account details`() {
        setTestContent(sampleAccount())

        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Revolut").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("paymentAccounts.userName".i18n()).assertCountEquals(1)
        composeTestRule.onAllNodesWithText("satoshi").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("mobile.paymentAccounts.currencyPicker.title".i18n()).assertCountEquals(1)
        composeTestRule.onAllNodesWithText("EUR (Euro), USD (US Dollar)").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("5000.00").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("4 days").assertCountEquals(1)
    }

    @Test
    fun `when chargeback risk is present then badge is displayed`() {
        setTestContent(sampleAccount(chargebackRisk = FiatPaymentMethodChargebackRisk.LOW))

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText(
                "paymentAccounts.createAccount.paymentMethod.risk.low".i18n(),
                substring = true,
            ).assertCountEquals(1)
    }

    @Test
    fun `when chargeback risk is absent then badge is hidden`() {
        setTestContent(sampleAccount(chargebackRisk = null))

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText(
                "paymentAccounts.createAccount.paymentMethod.risk.low".i18n(),
                substring = true,
            ).assertCountEquals(0)
    }

    private fun sampleAccount(
        chargebackRisk: FiatPaymentMethodChargebackRisk? = null,
    ): RevolutAccount =
        RevolutAccount(
            accountName = "Revolut Main",
            accountPayload =
                RevolutAccountPayload(
                    selectedCurrencies = listOf(FiatCurrency(code = "USD", name = "US Dollar"), FiatCurrency(code = "EUR", name = "Euro")),
                    userName = "satoshi",
                    paymentMethodName = "Revolut",
                    chargebackRisk = chargebackRisk,
                ),
            tradeLimitInfo = "5000.00",
            tradeDuration = "4 days",
        )
}
