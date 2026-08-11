package network.bisq.mobile.client.payment_accounts.common.ui.account_detail

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut.RevolutAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut.RevolutAccountPayload
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.revolut.RevolutAccountDetailContent
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import org.robolectric.annotation.Config

@Config(application = TestApplication::class)
class RevolutAccountDetailContentUiTest : BisqComposeUiTestBase() {
    private fun setAccountContent(account: RevolutAccount) {
        setTestContent {
            RevolutAccountDetailContent(account = account)
        }
    }

    @Test
    fun `when revolut review renders then shows base account details`() {
        setAccountContent(sampleAccount())

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
        setAccountContent(sampleAccount(chargebackRisk = FiatPaymentMethodChargebackRisk.LOW))

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText(
                "paymentAccounts.createAccount.paymentMethod.risk.low".i18n(),
                substring = true,
            ).assertCountEquals(1)
    }

    @Test
    fun `when chargeback risk is absent then badge is hidden`() {
        setAccountContent(sampleAccount(chargebackRisk = null))

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
