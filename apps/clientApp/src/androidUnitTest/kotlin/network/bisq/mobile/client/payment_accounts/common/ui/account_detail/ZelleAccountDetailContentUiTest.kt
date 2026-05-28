package network.bisq.mobile.client.payment_accounts.common.ui.account_detail

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.client.common.presentation.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle.ZelleAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle.ZelleAccountPayload
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.ZelleAccountDetailContent
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
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
class ZelleAccountDetailContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

    private fun setTestContent(account: ZelleAccount) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    ZelleAccountDetailContent(
                        account = account,
                    )
                }
            }
        }
    }

    @Test
    fun `when zelle review renders then shows header and base rows`() {
        setTestContent(
            account = sampleAccount(),
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Zelle").assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.country".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.currency".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("USD (US Dollar)").assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.holderName".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.emailOrMobileNr".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when chargeback risk is present then badge is displayed`() {
        setTestContent(
            account = sampleAccount(chargebackRisk = FiatPaymentMethodChargebackRisk.LOW),
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText(
                "paymentAccounts.summary.chargebackRisk"
                    .i18n(FiatPaymentMethodChargebackRiskVO.LOW.textKey.i18n()),
            ).assertIsDisplayed()
    }

    @Test
    fun `when chargeback risk is absent then badge is hidden`() {
        setTestContent(
            account = sampleAccount(chargebackRisk = null),
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText(
                "paymentAccounts.summary.chargebackRisk"
                    .i18n(FiatPaymentMethodChargebackRiskVO.LOW.textKey.i18n()),
                substring = true,
            ).assertCountEquals(0)
    }

    private fun sampleAccount(
        chargebackRisk: FiatPaymentMethodChargebackRisk? = null,
    ): ZelleAccount =
        ZelleAccount(
            accountName = "Alice Doe",
            accountPayload =
                ZelleAccountPayload(
                    holderName = "Alice Doe",
                    emailOrMobileNr = "alice@example.com",
                    country = Country(code = "US", name = "United States"),
                    currency = FiatCurrency(code = "USD", name = "US Dollar"),
                    paymentMethodName = "Zelle",
                    chargebackRisk = chargebackRisk,
                ),
        )
}
