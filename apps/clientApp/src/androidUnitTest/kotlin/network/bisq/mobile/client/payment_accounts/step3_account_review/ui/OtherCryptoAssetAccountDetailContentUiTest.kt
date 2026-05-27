package network.bisq.mobile.client.payment_accounts.step3_account_review.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.OtherCryptoAssetAccountDetailContent
import network.bisq.mobile.domain.model.account.crypto.OtherCryptoAssetAccount
import network.bisq.mobile.domain.model.account.crypto.OtherCryptoAssetAccountPayload
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
class OtherCryptoAssetAccountDetailContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

    private fun setTestContent(account: OtherCryptoAssetAccount) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    OtherCryptoAssetAccountDetailContent(
                        account = account,
                    )
                }
            }
        }
    }

    @Test
    fun `when auto conf supported and enabled then crypto rows and details section are shown`() {
        setTestContent(
            account =
                sampleAccount(
                    supportAutoConf = true,
                    isAutoConf = true,
                    creationDate = "2025-04-01",
                    tradeLimitInfo = "5000.00",
                    tradeDuration = "4 days",
                ),
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("ETH").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ethereum").assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.address".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.isInstant".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.autoConf.use".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.autoConf.numConfirmations".i18n()).assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.tradeLimit".i18n())
            .assertCountEquals(1)
    }

    @Test
    fun `when auto conf not supported then auto conf rows are hidden`() {
        setTestContent(
            account = sampleAccount(supportAutoConf = false, isAutoConf = true),
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.use".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.numConfirmations".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.maxTradeAmount".i18n())
            .assertCountEquals(0)
    }

    @Test
    fun `when details metadata missing then details section fields are hidden`() {
        setTestContent(
            account =
                sampleAccount(
                    supportAutoConf = true,
                    isAutoConf = false,
                    creationDate = null,
                    tradeLimitInfo = null,
                    tradeDuration = null,
                ),
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.accountCreationDate".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.tradeLimit".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.tradeDuration".i18n())
            .assertCountEquals(0)
    }

    private fun sampleAccount(
        supportAutoConf: Boolean,
        isAutoConf: Boolean,
        creationDate: String? = null,
        tradeLimitInfo: String? = null,
        tradeDuration: String? = null,
    ): OtherCryptoAssetAccount =
        OtherCryptoAssetAccount(
            accountName = "My Ethereum Account",
            accountPayload =
                OtherCryptoAssetAccountPayload(
                    address = "0x1234567890abcdef1234567890abcdef12345678",
                    isInstant = true,
                    isAutoConf = isAutoConf,
                    autoConfNumConfirmations = 2,
                    autoConfMaxTradeAmount = 1,
                    autoConfExplorerUrls = "https://explorer.example.com",
                    currencyCode = "ETH",
                    currencyName = "Ethereum",
                    supportAutoConf = supportAutoConf,
                ),
            creationDate = creationDate,
            tradeLimitInfo = tradeLimitInfo,
            tradeDuration = tradeDuration,
        )
}
