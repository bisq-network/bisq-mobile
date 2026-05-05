package network.bisq.mobile.presentation.create_payment_account.account_review.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccount
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccountPayload
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserDefinedAccountDetailContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

    private fun setTestContent(account: UserDefinedFiatAccount) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    UserDefinedAccountDetailContent(
                        account = account,
                    )
                }
            }
        }
    }

    @Test
    fun `when user defined detail renders then shows method and account data rows`() {
        // Given / When
        setTestContent(
            account = sampleAccount(),
        )

        // Then
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Bank Transfer").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.userDefined.accountData".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("IBAN: DE89370400440532013000")
            .assertIsDisplayed()
    }

    @Test
    fun `when account has details fields then details section rows are rendered`() {
        // Given / When
        setTestContent(
            account =
                sampleAccount(
                    creationDate = "Apr 3, 2026",
                    tradeLimitInfo = "1000 EUR",
                    tradeDuration = "8 days",
                ),
        )

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.details".i18n().uppercase())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.accountCreationDate".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.tradeLimit".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.tradeDuration".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when account has no details fields then details section is not rendered`() {
        // Given / When
        setTestContent(
            account =
                sampleAccount(
                    creationDate = null,
                    tradeLimitInfo = null,
                    tradeDuration = null,
                ),
        )

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.details".i18n().uppercase())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.accountCreationDate".i18n())
            .assertCountEquals(0)
    }

    @Test
    fun `when chargeback risk present then chargeback badge is shown`() {
        // Given / When
        setTestContent(
            account = sampleAccount(chargebackRisk = FiatPaymentMethodChargebackRisk.LOW),
        )

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText(
                "paymentAccounts.summary.chargebackRisk"
                    .i18n(FiatPaymentMethodChargebackRiskVO.LOW.textKey.i18n()),
            ).assertIsDisplayed()
    }

    @Test
    fun `when chargeback risk absent then chargeback badge is hidden`() {
        // Given / When
        setTestContent(
            account = sampleAccount(chargebackRisk = null),
        )

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText(
                "paymentAccounts.summary.chargebackRisk"
                    .i18n(FiatPaymentMethodChargebackRiskVO.LOW.textKey.i18n()),
                substring = true,
            ).assertCountEquals(0)
    }

    @Test
    fun `when account data is long and valid then account data value is rendered`() {
        // Given
        val longAccountData = "1234567890".repeat(100)

        // When
        setTestContent(
            account = sampleAccount(accountData = longAccountData),
        )

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText(longAccountData, substring = true)
            .assertIsDisplayed()
    }

    private fun sampleAccount(
        accountData: String = "IBAN: DE89370400440532013000",
        creationDate: String? = null,
        tradeLimitInfo: String? = null,
        tradeDuration: String? = null,
        chargebackRisk: FiatPaymentMethodChargebackRisk? = null,
    ): UserDefinedFiatAccount =
        UserDefinedFiatAccount(
            accountName = "Custom Account",
            accountPayload =
                UserDefinedFiatAccountPayload(
                    accountData = accountData,
                    paymentMethodName = "Bank Transfer",
                    currency = "EUR",
                    country = "Germany",
                    chargebackRisk = chargebackRisk,
                ),
            creationDate = creationDate,
            tradeLimitInfo = tradeLimitInfo,
            tradeDuration = tradeDuration,
        )
}
